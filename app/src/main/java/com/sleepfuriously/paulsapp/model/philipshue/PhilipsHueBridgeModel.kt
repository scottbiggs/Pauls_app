package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertPHv2Room
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertV2GroupedLights
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertV2Light
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ADD
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_DELETE
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ERROR
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_UPDATE
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_BRIDGE_HOME
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_PRIVATE_GROUP
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ZONE
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

    private val _bridge = MutableStateFlow<PhilipsHueBridgeInfo>(
        PhilipsHueBridgeInfo(
            v2Id = bridgeId,
            labelName = "",     // todo make sure this is retrieved bridge itself later
            ipAddress = bridgeIpAddress,
            token = bridgeToken,
            active = false,
            connected = false,
            rooms = emptySet(),
            scenes = emptyList(),
            zones = emptyList()
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

    private val _rooms = MutableStateFlow<List<PhilipsHueRoomInfo>>(listOf())
    /** list of rooms for this bridge */
    val rooms = _rooms.asStateFlow()

    private val _scenes = MutableStateFlow<List<PHv2Scene>>(listOf())
    /** list of all scenes for this bridge */
    val scenes = _scenes.asStateFlow()

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

            // start sse, but only if the bridge is active
            if (bridge.value.active) {

                // initialize server-sent events.
                Log.d(TAG, "init(): initializing sse")
                phSse = PhilipsHueSSE(
                    bridgeId = bridgeId,
                    bridgeIpAddress = bridgeIpAddress,
                    bridgeToken = bridgeToken,
                    coroutineScope = coroutineScope
                )
                connectSSE()
                Log.d(TAG, "init(): collecting sse")
                while (true) {
                    phSse.serverSentEvent.collect { sseEvent ->
                        interpretEvent(sseEvent)
                    }
                    // todo: test to see if this actually works!!!
                    phSse.openEvent.collect { openEvent ->
                        interpretOpenEvent(openEvent)
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
            // yep, active. so let's update our flows
            //

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

                if (apiGroup != null) {
                    convertV2GroupedLights(
                        v2GroupedLists = apiGroup,
                        bridgeIp = bridgeIpAddress,
                        bridgeToken = bridgeToken
                    )
                }
                else {
                    emptyList()
                }
            }

            _rooms.update {
                loadRoomsFromApi(
                    bridgeIp = bridgeIpAddress,
                    bridgeToken = bridgeToken
                ) ?: emptyList()
            }

            // finally update the flow
            _bridge.update {
                it.copy(
                    active = true,
                    rooms = rooms.value.toSet(),
                    scenes = scenes.value,
                    zones = zones.value
                )
            }

        } else {
            // no longer connected.  Remember to turn off sse (just in case)!
            _bridge.update {
                it.copy(
                    active = false
                )
            }
        }
    }

    /**
     * Checks to see if this bridge is responding to its ip.  This tests
     * actual connection (not internal data), so it needs to be done in
     * a separate thread.
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
        return bridge.value?.connected ?: false
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
    private suspend fun loadRoomsFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : List<PhilipsHueRoomInfo>? {

        val bridgeRooms = PhilipsHueBridgeApi.getAllRoomsFromApi(
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )

        if (bridgeRooms.errors.isNotEmpty()) {
            Log.e(TAG, "loadRoomsFromApi() error getting rooms:")
            Log.e(TAG, "   err msg = ${bridgeRooms.errors[0].description}")
            return null
        }

        return PhilipsHueDataConverter.convertV2RoomAll(
            phV2Rooms = bridgeRooms,
            bridgeIp = bridgeIp,
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
                    val light = bridge.value?.let { findLightFromId(eventDatum.owner!!.rid, it) }
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

                        // todo: handle other changes to light
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
                            val room = bridge.value?.let { findRoomFromId(roomId, it) }
                            if (room != null) {
                                if (eventDatum.dimming != null) {
                                    room.brightness = eventDatum.dimming.brightness
                                }
                                if (eventDatum.on != null) {
                                    room.on = eventDatum.on.on
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
            bridge.value.copy(connected = isOpen)
        }
    }


    /**
     * This is pure side effect.  The [bridge] flow variable is updated
     * with new data to a room.
     */
    private fun updateBridgeRooms(modifiedRoom: PhilipsHueRoomInfo) {
        _bridge.update {
            val newRoomList = mutableListOf<PhilipsHueRoomInfo>()

            // replace the room matching the id with the modified room
            bridge.value?.rooms?.forEach { room ->
                if (room.id == modifiedRoom.id) {
                    newRoomList.add(modifiedRoom)
                }
                else {
                    newRoomList.add(room)
                }
            }

            // finally make a new version of the bridge using the new rooms
            it.copy(rooms = newRoomList.toSet(), )
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