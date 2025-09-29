package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi.getBridgeApi
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertV2GroupedLights
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertV2Light
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ADD
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_DELETE
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ERROR
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_UPDATE
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceServerSentEvent
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_BRIDGE_HOME
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_PRIVATE_GROUP
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ZONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents a single Philips Hue bridge.  Communication to and from the bridge
 * goes here.
 *
 * Usage
 *  - Instantiate with info that this can use to find the bridge (just an ID,
 *  an ip, and token are needed).  This will try to contact the bridge and get
 *  all relevant data (rooms, scenes, zones).
 *
 *  - Receive flow.  Once the bridge is setup, it will start a stateflow to [bridge],
 *  which will notify all listeners about the state of the bridge.  Any changes will
 *  be sent through the flow.  Null means that the bridge is still gathering data.
 *
 *  - Connect SSE.  This tells the bridge to start receiving server-sent events
 *  to this class.  It's done automatically the first time if initialization was
 *  successful.  Disconnect with [disconnectSSE] and reconnect with [connectSSE].
 *  Use the [bridge] flow variable to see if the bridge is currently connected.
 *
 * @param   bridgeId            The id used to differentiate this bridge.
 *                              Note that this is the v2 id (long) and NOT
 *                              the v1 (short) bridge id.
 *
 * @param   bridgeIpAddress     Ip of this bridge on the local wifi network
 *
 * @param   bridgeToken         The token necessary to communicate with this bridge.
 *                              Also called username in the PH docs.
 *
 * @param   coroutineScope      Used for all the suspend functions within this class
 */
class PhilipsHueBridgeModel(
    val bridgeId: String,
    val bridgeIpAddress: String,
    val bridgeToken: String,
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------------
    //  flow data
    //-------------------------------------

    private val _bridge = MutableStateFlow(
        PhilipsHueBridgeInfo(
            v2Id = bridgeId,
            bridgeId = "",     // todo make sure this is retrieved by bridge itself later
            ipAddress = bridgeIpAddress,
            token = bridgeToken,
            active = false,
            connected = false,
            rooms = emptyList(),
            scenes = emptyList(),
            zones = emptyList(),
            humanName = "default"
        ))
    /** Flow for this bridge. Collectors will be notified of changes to this bridge. */
    val bridge = _bridge.asStateFlow()

    private val _devices = MutableStateFlow<List<PHv2Device>>(emptyList())
    /** master list of all devices on this bridge */
    val devices = _devices.asStateFlow()

    private val _lights = MutableStateFlow<List<PhilipsHueLightInfo>>(emptyList())
    /** master list of all lights for this bridge */
    val lights = _lights.asStateFlow()

    private val _groupedLights = MutableStateFlow<List<PHv2GroupedLight>>(emptyList())
    /** master list of all the light groups for this bridge */
    val groupedLights = _groupedLights.asStateFlow()

    // fixme: redundant with PhilipsHueBridgeInfo.rooms
    private val _rooms = MutableStateFlow<List<PhilipsHueRoomInfo>>(listOf())
    /** list of rooms for this bridge */
    val rooms = _rooms.asStateFlow()

    // fixme: redundant with PhilipsHueBridgeInfo.scenes
    private val _scenes = MutableStateFlow<List<PHv2Scene>>(listOf())
    /** list of all scenes for this bridge */
    val scenes = _scenes.asStateFlow()

    // fixme: redundant with PhilipsHueBridgeInfo.zones
    private val _zones = MutableStateFlow<List< PHv2Zone>>(listOf())
    /** list of all zones for this bridge */
    val zones = _zones.asStateFlow()

    //-------------------------------------
    //  class data
    //-------------------------------------

    /** Access point to server-sent events from this bridge */
    private lateinit var phSse : PhilipsHueSSE

    //-------------------------------------
    //  constructors & initializers
    //-------------------------------------

    /**
     * Alternate constructor:
     *
     * @param   bridgeInfo          Takes a [PhilipsHueBridgeInfo] and turns it
     *                              into this Model.
     *
     * @param   coroutineScope      Used for all the suspend functions within this class
     */
    operator fun invoke(
        bridgeInfo: PhilipsHueBridgeInfo,
        coroutineScope: CoroutineScope
    ) : PhilipsHueBridgeModel {
        return PhilipsHueBridgeModel(
            bridgeId = bridgeInfo.v2Id,
            bridgeIpAddress = bridgeInfo.v2Id,
            bridgeToken = bridgeInfo.token,
            coroutineScope = coroutineScope
        )
    }


    /**
     * Called every time this class is instantiated.  Does NOT do the
     * work of retrieving info from the long-term storage (that stuff
     * is done in [PhilipsHueBridgeStorage]).
     */
    init {
        Log.d(TAG, "init() begin")
        coroutineScope.launch(Dispatchers.IO) {

            refresh()

            // exit if the bridge is not active as there's nothing more to do
            if (bridge.value.active == false) {
                return@launch
            }

            // initialize server-sent events.
            Log.d(TAG, "init(): initializing sse")
            phSse = PhilipsHueSSE(
                bridgeId = bridgeId,
                bridgeIpAddress = bridgeIpAddress,
                bridgeToken = bridgeToken,
                coroutineScope = coroutineScope
            )
            connectSSE()

            // take 3
            // start collecting open events
            coroutineScope.launch {
                while (true) {
                    phSse.openEvent.collectLatest { openEvent ->
                        Log.d(TAG, "received openEvent: $openEvent for bridge ${bridge.value.humanName}")
                        interpretOpenEvent(openEvent)
                    }
                }
            }

            coroutineScope.launch {
                while (true) {
                    phSse.serverSentEvent.collectLatest { sseEvent ->
                        Log.d(TAG, "received sseEvent for bridge ${bridge.value.humanName}:")
                        Log.d(TAG, "      type: ${sseEvent.type}")
                        Log.d(TAG, "      id:   ${sseEvent.eventId}")
                        Log.d(TAG, "      data: ${sseEvent.data}")
                        interpretEvent(sseEvent)
                    }
                }
            }
        }
    }

    //-------------------------------------
    //  overloaded functions
    //-------------------------------------

    // equals is overloaded so that flow will correctly reflect changes
    override fun equals(other: Any?): Boolean {
        if (super.equals(other) == false) { return false }

        val otherBridgeModel = other as PhilipsHueBridgeModel

        if (otherBridgeModel.bridgeId  != bridgeId) { return false }
        if (otherBridgeModel.bridgeIpAddress  != bridgeIpAddress) { return false }
        if (otherBridgeModel.bridgeToken  != bridgeToken) { return false }
        if (otherBridgeModel.bridge != bridge) { return false }
        if (otherBridgeModel.phSse != phSse) { return false }

        return true
    }

    override fun hashCode(): Int {
        var result = bridgeId.hashCode()
        result = 31 * result + bridgeIpAddress.hashCode()
        result = 31 * result + bridgeToken.hashCode()
        result = 31 * result + bridge.hashCode()
//        result = 31 * result + phSse.hashCode()
        return result
    }

    //-------------------------------------
    //  public functions
    //-------------------------------------

    /**
     * Checks the bridge status and refreshes all the bridge data using the
     * api.
     *
     * NOTE:
     *  Does NOT do anything with the sse (server-sent events)!
     *
     * side effects
     *  - numerous!!!
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {

        Log.d(TAG, "refresh(): gathering data from bridge")

        // check to see if the bridge is active
        if (isBridgeActive(
                bridgeIpStr = bridgeIpAddress,
                bridgeToken = bridgeToken
            )
        ) {
            //
            // yep, active. continue
            //

            val v2bridgeResource = getBridgeApi(
                bridgeIpStr = bridgeIpAddress,
                token = bridgeToken
            )
            val bridgeId = v2bridgeResource.getDeviceName()

            _devices.update {
                PhilipsHueBridgeApi.getAllDevicesFromApi(
                    bridgeIp = bridgeIpAddress,
                    bridgeToken = bridgeToken
                )
            }

            _lights.update {
                val v2LightsList = PhilipsHueBridgeApi.getAllLightsFromApi(
                    bridgeIp = bridgeIpAddress,
                    bridgeToken = bridgeToken
                )

                val newLightList = mutableListOf<PhilipsHueLightInfo>()
                v2LightsList.data.forEach { v2Light ->
                    newLightList.add(convertV2Light(v2Light))
                }
                newLightList
            }

            _groupedLights.update {
                val apiGroup = PhilipsHueBridgeApi.getAllGroupedLightsFromApi(
                    bridgeIp = bridgeIpAddress,
                    bridgeToken = bridgeToken
                )

                if (apiGroup == null) {
                    Log.d(TAG, "refresh(): network error trying to find grouped lights!")
                    emptyList()
                }

                else if (apiGroup.errors.isNotEmpty()) {
                    Log.w(TAG, "refresh(): api error trying to find grouped lights!")
                    emptyList()
                }
                else {
                    convertV2GroupedLights(apiGroup)
                }
            }

            // load up the various groupings
            val rooms = loadRoomsFromApi() ?: emptyList()
            val scenes = loadScenesFromApi()
            val zones = loadZonesFromApi()

            // use the devices to find the bridge human name
            val humanName = PhilipsHueDataConverter.getBridgeUsername(
                bridgeIp = bridgeIpAddress,
                bridgeToken = bridgeToken
            )

            // finally update the flow
            _bridge.update {
                Log.d(TAG, "refresh() - updating _bridge bridgeId = $bridgeId")
                it.copy(
                    active = true,
                    rooms = rooms,
                    scenes = scenes,
                    zones = zones,
                    bridgeId = bridgeId,
                    humanName = humanName
                )
            }

        } else {
            // no longer connected.  Remember to turn off sse (just in case)!
            phSse.stopSSE()
            _bridge.update {
                Log.d(TAG, "no longer connected")
                it.copy(
                    active = false,
                    connected = false
                )
            }
        }
    }

    /**
     * Checks to see if this bridge is responding to its ip.  This tests
     * actual connection (not internal data), so it needs to be done in
     * a separate coroutine.
     */
    suspend fun isConnectedSlow() : Boolean {
        return doesBridgeRespondToIp(bridgeIpAddress)
    }

    /**
     * Fast version to check connection.  Just looks at the current connected
     * value.  Suitable for a quick check, but not to see if the brige has actually
     * changed.
     */
    fun isConnectedFast() : Boolean {
        return bridge.value.connected
    }


    /**
     * Connects this app to the bridge so that it can receive updates from
     * the bridge itself.
     *
     * side effects
     *  - The bridge will start receiving server-sent events from the bridge
     *  - No need to change the connection status as the flow from [PhilipsHueSSE]
     *    will take care of that
     */
    fun connectSSE() {
        phSse.startSse()
    }

    /**
     * Stop the bridge from receiving server-sent events from the bridge.
     *
     * side effects
     *  - The bridge will no longer report server-sent events
     *  - No need to change the connection status as the flow from [PhilipsHueSSE]
     *    will take care of that
     */
    fun disconnectSSE() {
        phSse.stopSSE()
    }

    //-------------------------------------
    //  private functions
    //-------------------------------------

    /**
     * Asks the bridge to get all the rooms.  Caller should know what to do with
     * this data.
     *
     * On error, returns null (probably the bridge is not connected).
     * Empty list means that there simply aren't any rooms.
     */
    private suspend fun loadRoomsFromApi() : List<PhilipsHueRoomInfo>? {

        val bridgeRooms = PhilipsHueBridgeApi.getAllRoomsFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        if (bridgeRooms.errors.isNotEmpty()) {
            Log.e(TAG, "loadRoomsFromApi() error getting rooms:")
            Log.e(TAG, "   err msg = ${bridgeRooms.errors[0].description}")
            return null
        }

        return PhilipsHueDataConverter.convertV2RoomAll(
            phV2Rooms = bridgeRooms,
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        ).toList()
    }

    /**
     * Finds all the scenes from the bridge.  Returns null if bridge is not
     * responding.  Could be an empty list.
     */
    private suspend fun loadScenesFromApi() : List<PHv2Scene> {
        val v2AllScenes = PhilipsHueBridgeApi.getAllScenesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        return PhilipsHueDataConverter.convertV2ScenesAll(v2AllScenes)
    }

    /**
     * Asks the bridge to get all its zones.  Will return null if the bridge
     * isn't working.  Note that this could be an empty list.
     */
    private suspend fun loadZonesFromApi() : List<PHv2Zone> {
        val v2AllZones = PhilipsHueBridgeApi.getAllZonesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        return PhilipsHueDataConverter.convertV2ZonesAll(v2AllZones)
    }


    /**
     * Working completely by side effect, this analyses the result of a server-
     * sent event and changes the contents of the bridge data appropriately.
     *
     * @param   event           Data structure describing the event.
     *
     * side effects
     *  [bridge] - modified to reflect whatever changed according the the bridge's sse.
     */
    private fun interpretEvent(event: PHv2ResourceServerSentEvent) {

        Log.d(TAG, "interpretEvent()  event.type = ${event.type}")
        Log.d(TAG, "interpretEvent()  event.eventId = ${event.eventId}")
        Log.d(TAG, "interpretEvent()  event.data = ${event.data}")

        // what kind of event?
        when (event.type) {
            EVENT_UPDATE -> {
                Log.v(TAG, "interpreting UPDATE event")
                interpretUpdateEvent(event)
            }

            EVENT_ADD -> {
                interpretAddEvent(event)
            }

            EVENT_DELETE -> {
                interpretDeleteEvent(event)
            }

            EVENT_ERROR -> {
                Log.e(TAG, "interpretEvent() - can't do anything with an error, skipping!")
                return
            }

            else -> {
                Log.e(TAG, "Unknown event type!!! Aborting!")
                return
            }
        }
    }

    /**
     * Interprets an UPDATE sse that was sent from a bridge.
     *
     * @param   event       The event it sent (should be an UPDATE)
     *
     * side effects
     *  - aspects of this bridge will change based on the event.  For
     *  example: a room's brightness and the status of its light can change.
     */
    private fun interpretUpdateEvent(event: PHv2ResourceServerSentEvent) {

        event.data.forEach { eventDatum ->
            // What kind of device changed?
            when (eventDatum.type) {
                RTYPE_LIGHT -> {
                    val light = findLightFromId(eventDatum.owner!!.rid, bridge.value)
                    if (light == null) {
                        Log.e(TAG, "unable to find changed light in interpretEvent()!")
                    } else {
                        // yep, there is indeed a light. What changed?
                        Log.d(
                            TAG,
                            "updating light event (id = ${light.lightId}, deviceId = ${light.deviceId})"
                        )
                        if (eventDatum.on != null) {
                            // On/Off changed.  Set the light approprately.
                            light.state.on = eventDatum.on.on
                        }

                        if (eventDatum.dimming != null) {
                            light.state.bri = eventDatum.dimming.brightness
                        }

                        // todo: save that light change!
                        Log.e(TAG, "light change not fully implemented!")
                    }
                }

                RTYPE_GROUP_LIGHT -> {
                    Log.d(TAG, "updating grouped_light event. owner = ${eventDatum.owner}")

                    // figure out what rtype the owner of this grouped_light event is
                    when (eventDatum.owner?.rtype) {
                        RTYPE_PRIVATE_GROUP -> {
                            // todo!!!
                            Log.e(TAG, "implement me!!!")
                        }

                        RTYPE_BRIDGE_HOME -> {
                            // todo
                            Log.e(TAG, "implement me!!!")
                        }

                        RTYPE_ROOM -> {
                            Log.d(TAG, "interpretUpdateEvent() RTYPE_GROUP_LIGHT / RTYPE_ROOM")
                            // Ah, the owner is a room.  Signal that room to change according to the event.
                            val roomId = eventDatum.owner.rid
                            var room = findRoomFromId(roomId, bridge.value)
                            if (room != null) {
                                // what changed?  Did the room dim or was it turned on/off?
                                if (eventDatum.dimming != null) {
                                    room = room.copy(brightness = eventDatum.dimming.brightness)
                                }
                                if (eventDatum.on != null) {
                                    room = room.copy(on = eventDatum.on.on)
                                }

                                updateBridgeRooms(room)

                            } else {
                                Log.e(
                                    TAG,
                                    "Error in interpretEvent()--can't find room that owns grouped_light event. Aborting!"
                                )
                            }
                        }
                    }
                }

                RTYPE_ROOM -> {
                    // todo: implement rooms
                    Log.e(TAG, "sse updating room not implemented")
                }

                RTYPE_ZONE -> {
                    // todo: implement zones
                    Log.e(TAG, "sse updating zone not implemented")
                }
            } // when (eventDatum.type)
        }
    }

    /**
     * Interprets an ADD sse that was sent from a bridge.
     *
     * @param   event       The event it sent (should be an ADD)
     *
     * side effects
     *  - aspects of this bridge will change based on the event.  For
     *  example: a room's brightness and the status of its light can change.
     */
    private fun interpretAddEvent(
        event: PHv2ResourceServerSentEvent
    ) {
        // double check to make sure this is an Add event
        if (event.type != EVENT_ADD) {
            Log.e(TAG, "interpretAddEvent() trying to interpret something else: ${event.type}! Aborting.")
            return
        }

        // go through the data and work on each
        event.data.forEach { eventDatum ->
            when (eventDatum.type) {
                RTYPE_LIGHT -> {
                    // todo: add light
                    Log.e(TAG, "sse adding light not implemented")
                }

                RTYPE_GROUP_LIGHT -> {
                    // todo: add grouped light
                    Log.e(TAG, "sse adding grouped light not implemented")
                }

                RTYPE_ROOM -> {
                    // todo: add room
                    Log.e(TAG, "sse adding room not implemented")
                }

                RTYPE_ZONE -> {
                    // todo add zoone
                    Log.e(TAG, "sse adding zone not implemented")
                }
            }
        }

        // fixme
        Log.e(TAG, "todo: implement interpreting ADD event")
    }

    /**
     * Similar to [interpretUpdateEvent] and [interpretAddEvent], but for
     * delete events.
     */
    private fun interpretDeleteEvent(
        event: PHv2ResourceServerSentEvent
    ) {
        // fixme
        Log.e(TAG, "todo: implement interpreting DELETE event")

    }

    /**
     * A bridge connection for sse has either begun or ended.  Make changes
     * accordingly.
     *
     * @param   isOpen          true -> bridge connection started
     *                          false -> bridge connection close
     */
    private fun interpretOpenEvent(isOpen: Boolean) {
        Log.d(TAG, "interpretOpenEvent() isOpen = $isOpen")

        _bridge.update {
            Log.d(TAG, "interpretOpenEvent() update")
            it.copy(connected = isOpen)
        }
    }

/*
    /** decompiled to java version--very useful to see what kotlin is doing under the hood! */
    private fun updateBridgeRooms(modifiedRoom: PhilipsHueRoomInfo) {
        Log.d(
            "PhilipsHueBridgeModel",
            "updateBridgeRooms() begin - bridge " + this.bridge.value.humanName
        )
        val newRoomList = (ArrayList<Any?>()) as MutableList<*>
        var counter = 0
//        val `$this$update$iv`: MutableStateFlow<*> = this._bridge
        val `$i$f$update` = 0

//        var `prevValue$iv`: Any?
        var prevValue: Any?
//        var `nextValue$iv`: Any?
        var nextValue: Any?

        do {
//            `prevValue$iv` = `$this$update$iv`.value
//            `prevValue$iv` = this._bridge.value
            prevValue = _bridge.value
//            val it = `prevValue$iv` as PhilipsHueBridgeInfo?
            val it = prevValue
            val var8 = 0
            Log.d("PhilipsHueBridgeModel", "   done creating new room list. " + counter)
            ++counter
//            `nextValue$iv` = dummyPhInfo
            nextValue = dummyPhInfo
//        } while (!`$this$update$iv`.compareAndSet(`prevValue$iv`, `nextValue$iv`))
//        } while (!this._bridge.compareAndSet(`prevValue$iv` as PhilipsHueBridgeInfo, `nextValue$iv` as PhilipsHueBridgeInfo))

        // Make sure _bridge.value hasn't changed while we were doing this computation.  That's the
        // whole point of this being thread safe.  Although it's really weird that it accomplishes
        // the thread safety by trying again and again.  It should work, but in my case it doesn't--why?
        // I've narrowed it down to my override of equals(), but I need to dive even deeper.
        } while (_bridge.compareAndSet(prevValue as PhilipsHueBridgeInfo, nextValue as PhilipsHueBridgeInfo) == false)

        Log.d(
            "PhilipsHueBridgeModel",
            "updateBridgeRooms() end.  bridge " + this.bridge.value.humanName
        )
    }
*/


    /**
     * This works by pure side effect.  The [bridge] flow variable is updated
     * with new data to a room.
     */
    private fun updateBridgeRooms(modifiedRoom: PhilipsHueRoomInfo) {
        Log.d(TAG, "updateBridgeRooms() BEGIN  bridge ${bridge.value.humanName}")
        bridge.value.rooms.forEach { room ->
            Log.d(TAG, "    - $room")
        }
        val newRoomList = mutableListOf<PhilipsHueRoomInfo>()

        _bridge.update {
            // replace the room matching the id with the modified room
            bridge.value.rooms.forEach { room ->
                if (room.id == modifiedRoom.id) {
                    newRoomList.add(modifiedRoom)
                }
                else {
                    newRoomList.add(room)
                }
            }

            // finally make a new version of the bridge using the new rooms
            it.copy(rooms = newRoomList)
        }

        Log.d(TAG, "updateBridgeRooms() end.  bridge ${bridge.value.humanName}")
        bridge.value.rooms.forEach { room ->
            Log.d(TAG, "    - $room")
        }
    }



    private fun updateBridgeScenes(modifiedScene: PHv2Scene) {
        TODO()
    }

    private fun updateBridgeZones(modifiedZone: PHv2Zone) {
        TODO()
    }

}

//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueBridgeModel"