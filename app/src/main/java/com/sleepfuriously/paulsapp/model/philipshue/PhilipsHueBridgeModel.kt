package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPut
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi.getBridgeApi
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueDataConverter.convertV2GroupedLightsAllToV2GroupedLights
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ADD
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_DELETE
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_ERROR
import com.sleepfuriously.paulsapp.model.philipshue.json.EVENT_UPDATE
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceServerSentEvent
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_BRIDGE_HOME
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_PRIVATE_GROUP
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_SCENE
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
 *  - Receive flow from this.  Once the bridge is setup, it will start a stateflow to
 *  [bridge], which will notify all listeners about the state of the bridge.
 *  Any changes will be sent through the flow.  Null means that the bridge is still
 *  gathering data.
 *
 *  - Connect SSE.  This tells the bridge to start receiving server-sent events
 *  to this class.  It's done automatically the first time if initialization was
 *  successful.  Disconnect with [disconnectSSE] and reconnect with [connectSSE].
 *  Use the [bridge] flow variable to see if the bridge is currently connected.
 *
 * @param   bridgeV2Id          The id used to differentiate this bridge.
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
    val bridgeV2Id: String,
    val bridgeIpAddress: String,
    val bridgeToken: String,
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------------
    //  flow data
    //-------------------------------------

    // dummy value to start
    private val _bridge = MutableStateFlow(
        PhilipsHueBridgeInfo(
            v2Id = bridgeV2Id,
            bridgeId = "",
            ipAddress = bridgeIpAddress,
            token = bridgeToken,
            active = false,
            connected = false,
            rooms = emptyList(),
            scenes = emptyList(),
            lights = emptyList(),
            zones = emptyList(),
            humanName = "default"
        )
    )
    /** Flow for this bridge. Collectors will be notified of changes to this bridge. */
    val bridge = _bridge.asStateFlow()

    //
    //  todo: The following could (should) be put in the bridge data!!!
    //

    private val _devices = MutableStateFlow<List<PHv2Device>>(emptyList())
    /** master list of all devices on this bridge */
    val devices = _devices.asStateFlow()

    private val _groupedLights = MutableStateFlow<List<PHv2GroupedLight>>(emptyList())
    /** master list of all the light groups for this bridge */
    val groupedLights = _groupedLights.asStateFlow()


    //-------------------------------------
    //  class data
    //-------------------------------------

    /** Access point to server-sent events from this bridge */
    private lateinit var phSse : PhilipsHueSSE

    //-------------------------------------
    //  constructors & initializers
    //-------------------------------------

    /**
     * Called every time this class is instantiated.  Does NOT do the
     * work of retrieving info from the long-term storage (that stuff
     * is done in [PhilipsHueBridgeStorage]) and it's assumed that it is
     * completed before instantiating; the params from that are required
     * to start this up anyway.
     */
    init {
        Log.d(TAG, "init() begin: id = $bridgeV2Id")
        coroutineScope.launch(Dispatchers.IO) {

            refresh()

            // exit if the bridge is not active as there's nothing more to do
            if (bridge.value.active == false) {
                return@launch
            }

            // initialize server-sent events.
            Log.d(TAG, "init(): initializing sse, bridgeId = $bridgeV2Id")
            phSse = PhilipsHueSSE(
                bridgeId = bridgeV2Id,
                bridgeIpAddress = bridgeIpAddress,
                bridgeToken = bridgeToken,
                coroutineScope = coroutineScope
            )
            connectSSE()

            // start collecting open/close events
            coroutineScope.launch {
                while (true) {
                    phSse.openEvent.collectLatest { openEvent ->
                        Log.d(TAG, "received openEvent: ${openEvent.toString().uppercase()} for bridge ${bridge.value.humanName}")
                        interpretOpenEvent(openEvent)
                    }
                }
            }

            // start collecting all other events
            coroutineScope.launch {
                while (true) {
                    phSse.serverSentEvent.collectLatest { sseEvent ->
                        Log.d(TAG, "received sseEvent for bridge ${bridge.value.humanName}:")
                        Log.d(TAG, "      type: ${sseEvent.type}")
                        Log.d(TAG, "      id:   ${sseEvent.eventId}")
                        Log.d(TAG, "      data size: ${sseEvent.data.size}")
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

        if (otherBridgeModel.bridgeV2Id  != bridgeV2Id) { return false }
        if (otherBridgeModel.bridgeIpAddress  != bridgeIpAddress) { return false }
        if (otherBridgeModel.bridgeToken  != bridgeToken) { return false }
        if (otherBridgeModel.bridge != bridge) { return false }
        if (otherBridgeModel.phSse != phSse) { return false }

        return true
    }

    override fun hashCode(): Int {
        var result = bridgeV2Id.hashCode()
        result = 31 * result + bridgeIpAddress.hashCode()
        result = 31 * result + bridgeToken.hashCode()
        result = 31 * result + bridge.hashCode()
        if (::phSse.isInitialized) {
            result = 31 * result + phSse.hashCode()
        }
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

            // smb - this seens to work, or does it?  Does this change really propogate upstream?
//            _lights.update {
//                val v2LightsList = PhilipsHueBridgeApi.getAllLightsFromApi(
//                    bridgeIp = bridgeIpAddress,
//                    bridgeToken = bridgeToken
//                )
//
//                val newLightList = mutableListOf<PhilipsHueLightInfo>()
//                v2LightsList.data.forEach { v2Light ->
//                    newLightList.add(convertV2LightToPhilipsHueLightInfo(v2Light, bridgeIpAddress))
//                }
//                newLightList
//            }

            _groupedLights.update {
                val apiGroup = PhilipsHueBridgeApi.getAllGroupedLightsFromApi(
                    bridgeIp = bridgeIpAddress,
                    bridgeToken = bridgeToken
                )

                if (apiGroup == null) {
                    Log.w(TAG, "refresh(): network error trying to find grouped lights!")
                    emptyList()
                }

                else if (apiGroup.errors.isNotEmpty()) {
                    Log.w(TAG, "refresh(): api error trying to find grouped lights!")
                    emptyList()
                }
                else {
                    convertV2GroupedLightsAllToV2GroupedLights(apiGroup)
                }
            }

            // load up the various groupings
            val rooms = loadRoomsFromApi() ?: emptyList()
            val scenes = loadScenesFromApi() ?: emptyList()
            val zones = loadZonesFromApi() ?: emptyList()
            val lights = loadLightsFromApi() ?: emptyList()

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
                    humanName = humanName,
                    lights = lights
                )
            }

        }
        else if (::phSse.isInitialized) {
            // no longer connected.  Remember to turn off sse (just in case)!
            phSse.stopSSE()
            _bridge.update {
                Log.v(TAG, "refresh() - no longer connected")
                it.copy(active = false, connected = false)
            }
        }
        else {
            Log.w(TAG, "refresh() - bridge is not active, so there's not much to do")
            _bridge.update {
                it.copy(active = false, connected = false)
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
        coroutineScope.launch {
            // need to refresh so we get the changes that may have happened while
            // disconnected
            refresh()
        }
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

    /**
     * Use this to turn on or off an entire room by sending a message to the
     * controlling bridge.
     *
     * @param   roomInfo        The room to modify.  It *really* should exist!
     *
     * @param   onStatus        The new On or Off state for this room.  Use NULL
     *                          if this should be unchanged.
     *
     * side effects
     *  The signal to change the room's on/off state will be sent.  The bridge will do the
     *  work of changing everything.  Then it will send us an sse with all the new details
     *  which will cause the [bridge] to update.  Long chain, but should work.
     */
    fun sendRoomOnOffStatusToBridge(
        roomInfo: PhilipsHueRoomInfo,
        onStatus: Boolean? = null,
        ) {
        // find the grouped light id
        val groupedLightId = findGroupedLightIdFromRoom(roomInfo)
        if (groupedLightId.isEmpty()) {
            Log.v(TAG, "setRoomOnOffStatus() - unable to find any grouped_lights for room ${roomInfo.name}")
            return
        }

        // construct the body. consists of on command in json format
        val body = "{\"on\": {\"on\": ${onStatus}}}"
        Log.d(TAG, "---> body = $body")

        // construct the url
        val url = PhilipsHueBridgeApi.createFullAddress(
            ip = bridgeIpAddress,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupedLightId"
        )

        coroutineScope.launch(Dispatchers.IO) {
            // send the PUT
            val response = synchronousPut(
                url = url,
                bodyStr = body,
                headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
                trustAll = true     // fixme when we have full security going
            )

            // did it work?
            if (response.isSuccessful) {
                Log.d(TAG, "setRoomOnOffStatus() PUT request successful!")
                Log.d(TAG, "   body = $body")
            } else {
                Log.e(TAG, "error sending PUT request in setRoomOnOffStatus()!")
                Log.e(TAG, "response:")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   headers = ${response.headers}")
                Log.e(TAG, "   body = ${response.body}")
            }
        }
    }

    /**
     * Use this to change the brightness of an entire room.  It'll tell the
     * bridge what to do.
     *
     * @param   roomInfo        The room to modify.  It *really* should exist!
     *
     * @param   brightness      The new brightness for this room.  Use NULL if
     *                          brightness is unchanged.
     *
     * side effects
     *  The signal to change the room's brightness will be sent.  The bridge will do the
     *  work of changing everything.  Then it will send us an sse with all the new details
     *  which will cause the [bridge] to update.  Long chain, but should work.
     */
    fun sendRoomBrightnessToBridge(
        roomInfo: PhilipsHueRoomInfo,
        brightness: Int
    ) {
        // find the grouped light id
        val groupedLightId = findGroupedLightIdFromRoom(roomInfo)
        if (groupedLightId.isEmpty()) {
            Log.v(TAG, "setRoomBrightness() - unable to find any grouped_lights for room id = ${roomInfo.name}.")
            return
        }

        // construct the body. consists of on and brightness in json format
        val body = "{\"dimming\": {\"brightness\": $brightness}}"
        Log.d(TAG, "---> body = $body")

        // construct the url
        val url = PhilipsHueBridgeApi.createFullAddress(
            ip = bridgeIpAddress,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupedLightId"
        )

        coroutineScope.launch(Dispatchers.IO) {
            // send the PUT
            val response = synchronousPut(
                url = url,
                bodyStr = body,
                headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
                trustAll = true     // fixme when we have full security going
            )

            // did it work?
            if (response.isSuccessful) {
                Log.d(TAG, "setRoomBrightness() PUT request successful!")
                Log.d(TAG, "   body = $body")
            } else {
                Log.e(TAG, "error sending PUT request in setRoomBrightness()!")
                Log.e(TAG, "response:")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   headers = ${response.headers}")
                Log.e(TAG, "   body = ${response.body}")
            }
        }
    }


    /**
     * Use this to tell the bridge to turn on or off an entire zone.  Very
     * similar to [sendRoomOnOffStatusToBridge].
     */
    fun sendZoneOnOffToBridge(
        zone: PhilipsHueZoneInfo,
        onStatus: Boolean? = null,
    ) {
        // find the grouped light id
        val groupedLightId = findGroupedLightIdFromZone(zone)
        if (groupedLightId.isEmpty()) {
            Log.v(TAG, "setZoneOnOffStatus() - unable to find any grouped_lights for zone ${zone.name}.")
            return
        }

        // construct the body. consists of on command in json format
        val body = "{\"on\": {\"on\": ${onStatus}}}"
        Log.d(TAG, "---> body = $body")

        // construct the url
        val url = PhilipsHueBridgeApi.createFullAddress(
            ip = bridgeIpAddress,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupedLightId"
        )

        coroutineScope.launch(Dispatchers.IO) {
            // send the PUT
            val response = synchronousPut(
                url = url,
                bodyStr = body,
                headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
                trustAll = true     // fixme when we have full security going
            )

            // did it work?
            if (response.isSuccessful) {
                Log.d(TAG, "setZoneOnOffStatus() PUT request successful!")
                Log.d(TAG, "   body = $body")
            } else {
                Log.e(TAG, "error sending PUT request in setZoneOnOffStatus()!")
                Log.e(TAG, "response:")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   headers = ${response.headers}")
                Log.e(TAG, "   body = ${response.body}")
            }
        }
    }

    /**
     * Very similar to [sendRoomBrightnessToBridge]
     */
    fun sendZoneBrightnessToBridge(
        zone: PhilipsHueZoneInfo,
        brightness: Int
    ) {
        // find the grouped light id
        val groupedLightId = findGroupedLightIdFromZone(zone)
        if (groupedLightId.isEmpty()) {
            Log.v(TAG, "setZoneBrightness() - unable to find grouped_lights for zone ${zone.name}.")
            return
        }

        // construct the body. consists of on and brightness in json format
        val body = "{\"dimming\": {\"brightness\": $brightness}}"
        Log.d(TAG, "---> body = $body")

        // construct the url
        val url = PhilipsHueBridgeApi.createFullAddress(
            ip = bridgeIpAddress,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupedLightId"
        )

        coroutineScope.launch(Dispatchers.IO) {
            // send the PUT
            val response = synchronousPut(
                url = url,
                bodyStr = body,
                headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
                trustAll = true     // fixme when we have full security going
            )

            // did it work?
            if (response.isSuccessful) {
                Log.d(TAG, "setZoneBrightness() PUT request successful!")
                Log.d(TAG, "   body = $body")
            } else {
                Log.e(TAG, "error sending PUT request in setZoneBrightness()!")
                Log.e(TAG, "response:")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   headers = ${response.headers}")
                Log.e(TAG, "   body = ${response.body}")
            }
        }
    }


    /**
     * Whenever a room has its scene set, call this function.  It will modify
     * the room so that it correctly reflects the new scene.  You can also call
     * it to indicate that NO scene is being displayed (like the user has made
     * changes, etc.).
     *
     * This is done here instead of through an sse because server-sent events
     * don't really identify the room that was changed--just the lights (as far
     * as I know, which is probably wrong).
     *
     * @param   room        The room that is currently undergoing a scene change.
     *
     * @param   scene       The details of the scene that is operating on the room.
     */
    fun updateRoomCurrentScene(
        room: PhilipsHueRoomInfo,
        scene: PHv2Scene
    ) {
        Log.d(TAG, "updateRoomCurrentScene() begin")
        Log.d(TAG, "    room.name = ${room.name}")
        Log.d(TAG, "    room.currentSceneName = ${room.currentSceneName}")
        Log.d(TAG, "    scene = ${scene.metadata.name}")

        // construct a new room with the data we want
        val newRoom = room.copy(
            currentSceneName = scene.metadata.name,
            on = true       // this could turn a room on that was off previously
        )

        // remake the room list for this bridge
        val newRoomList = mutableListOf<PhilipsHueRoomInfo>()
        bridge.value.rooms.forEach {
            if (it.v2Id == room.v2Id) {
                newRoomList.add(newRoom)
            }
            else {
                newRoomList.add(it)
            }
        }

        // get the flow going
        _bridge.update {
            it.copy(rooms = newRoomList)
        }
    }

    /** just like [updateRoomCurrentScene] for zones */
    fun updateZoneCurrentScene(
        zone: PhilipsHueZoneInfo,
        scene: PHv2Scene
    ) {
        // construct a new zone with the data we want
        val newZone = zone.copy(
            currentSceneName = scene.metadata.name,
            on = true
        )

        // remake the zone list for this bridge
        val newZoneList = mutableListOf<PhilipsHueZoneInfo>()
        bridge.value.zones.forEach {
            if (it.v2Id == zone.v2Id) {
                newZoneList.add(newZone)
            }
            else {
                newZoneList.add(it)
            }
        }

        // get the flow going
        _bridge.update {
            it.copy(zones = newZoneList)
        }
    }

    //-------------------------------------
    //  private functions
    //-------------------------------------

    /**
     * Finds the id of a light group from a specified room.  A room can have all
     * kinds of services, but only one is a [RTYPE_GROUP_LIGHT].  That's the
     * thing we want.
     *
     * @return      The rid of the grouped light in the given room.
     *              Returns an empty string if no light group is found.
     */
    private fun findGroupedLightIdFromRoom(roomInfo: PhilipsHueRoomInfo) : String {
        var groupedLightId = ""
        for (groupedLight in roomInfo.groupedLightServices) {
            if (groupedLight.rtype == RTYPE_GROUP_LIGHT) {
                groupedLightId = groupedLight.rid
                break
            }
        }
        return groupedLightId
    }

    /**
     * Similar to [findGroupedLightIdFromRoom] but for rooms!  hehe
     *
     * @return      The rid of the grouped light in the given zone.
     *              Returns an empty string if no light group is found.
     */
    private fun findGroupedLightIdFromZone(zone: PhilipsHueZoneInfo) : String {
        var groupedLightId = ""
        for (groupedLight in zone.groupedLightServices) {
            if (groupedLight.rtype == RTYPE_GROUP_LIGHT) {
                groupedLightId = groupedLight.rid
                break
            }
        }
        return groupedLightId
    }


    /**
     * Asks the bridge to get all the rooms.  Caller should know what to do with
     * this data.
     *
     * On error, returns null (probably the bridge is not connected).
     * Empty list means that there simply aren't any rooms.
     */
    private suspend fun loadRoomsFromApi() : List<PhilipsHueRoomInfo>? {

        val bridgeV2ResourceRoomsAll = PhilipsHueBridgeApi.getAllRoomsFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        if (bridgeV2ResourceRoomsAll.errors.isNotEmpty()) {
            Log.e(TAG, "loadRoomsFromApi() error getting rooms:")
            Log.e(TAG, "   err msg = ${bridgeV2ResourceRoomsAll.errors[0].description}")
            return null
        }

        return PhilipsHueDataConverter.convertPHv2ResourceRoomsAllToPhilipsHueRoomInfoList(
            phV2Rooms = bridgeV2ResourceRoomsAll,
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )
    }

    /**
     * Asks the bridge to get all its lights.  Returns null if the bridge isn't
     * working or some network error occurs.
     */
    private suspend fun loadLightsFromApi() : List<PhilipsHueLightInfo>?
            = withContext(Dispatchers.IO) {
        val v2Resourcelights = PhilipsHueBridgeApi.getAllLightsFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        if (v2Resourcelights.errors.isNotEmpty()) {
            Log.e(TAG, "loadLightsFromApi() error getting lights:")
            Log.e(TAG, "   err msg = ${v2Resourcelights.errors[0].description}")
            return@withContext null
        }

        val lightsList = mutableListOf<PhilipsHueLightInfo>()
        v2Resourcelights.data.forEach { v2Light ->
            lightsList.add(PhilipsHueDataConverter.convertV2LightToPhilipsHueLightInfo(v2Light, bridgeIpAddress))
        }

        return@withContext lightsList
    }


    /**
     * Asks the bridge to get all its zones.  Will return null if the bridge
     * isn't working.  Note that this could be an empty list.
     */
    private suspend fun loadZonesFromApi() : List<PhilipsHueZoneInfo>?
            = withContext(Dispatchers.IO) {
        val v2AllZones = PhilipsHueBridgeApi.getAllZonesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        if (v2AllZones.errors.isNotEmpty()) {
            return@withContext null
        }

        return@withContext PhilipsHueDataConverter.convertPHv2ZonesAllToPhilipsHueZoneInfoList(
            v2ZonesAll = v2AllZones,
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )
    }

    /**
     * Finds all the scenes from the bridge.  Returns null if bridge is not
     * responding.  Could be an empty list.
     */
    private suspend fun loadScenesFromApi() : List<PHv2Scene>? {
        val v2AllScenes = PhilipsHueBridgeApi.getAllScenesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        if (v2AllScenes.errors.isNotEmpty()) {
            return null
        }

        return PhilipsHueDataConverter.convertV2ScenesAllToV2Scene(v2AllScenes)
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
        Log.d(TAG, "interpretEvent()  event.data.size = ${event.data.size}")
        Log.d(TAG, "interpretEvent()  event.data = ${event.data}")

        // what kind of event?
        when (event.type) {
            EVENT_UPDATE -> {
                interpretUpdateEvent(event)
            }

            EVENT_ADD -> {
                coroutineScope.launch {
                interpretAddEvent(event)
                    }
            }

            EVENT_DELETE -> {
                interpretDeleteEvent(event)
            }

            EVENT_ERROR -> {
                Log.e(TAG, "interpretEvent() - can't do anything with an error, skipping!")
            }

            else -> {
                Log.e(TAG, "Unknown event type!!! Aborting!")
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
        Log.d(TAG, "interpretUpdateEvent() $event")
        for (eventDatum in event.data) {
            // What kind of device changed?
            when (eventDatum.type) {
                RTYPE_LIGHT -> {
                    Log.d(TAG, "interpretUpdateEvent() - RTYPE_LIGHT")
                    var foundLight = findLightFromId(eventDatum.owner!!.rid, bridge.value)
                    if (foundLight == null) {
                        Log.e(TAG, "unable to find changed light in interpretEvent()!")
                        break
                    }
                    // yep, there is indeed a light. What changed?
                    Log.d(TAG, "updating light event (id = ${foundLight.lightId}, deviceId = ${foundLight.deviceId})")
                    if (eventDatum.on != null) {
                        // On/Off changed.  Set the light approprately.
                        val tmpNewState = foundLight.state.copy(on = eventDatum.on.on)
                        foundLight = foundLight.copy(state = tmpNewState)
                    }

                    if (eventDatum.dimming != null) {
                        // Did the dimming change?
                        val tmpNewState = foundLight.state.copy(bri = eventDatum.dimming.brightness)
                        foundLight = foundLight.copy(state = tmpNewState)
                    }

                    // save the changed light in our lights list flow
//                    _lights.update {
//                        val newLightsList = mutableListOf<PhilipsHueLightInfo>()
//                        it.forEach { light ->
//                            if (foundLight.lightId == light.lightId) {
//                                newLightsList.add(foundLight)
//                            }
//                            else {
//                                newLightsList.add(light)
//                            }
//                        }
//                        newLightsList
//                    }

                    // save the light in the bridge too
                    _bridge.update {
                        val newLightsList = mutableListOf<PhilipsHueLightInfo>()
                        it.lights.forEach { light ->
                            if (foundLight.lightId == light.lightId) {
                                newLightsList.add(foundLight)
                            }
                            else {
                                newLightsList.add(light)
                            }
                        }
                        it.copy(lights = newLightsList)
                    }
                    Log.d(TAG, "light change detected and recorded")
                }

                //--------
                // There are 3 types of light groups.
                //  RTYPE_ROOM - The most significant for this app, define the
                //    lights for a given room.
                //  RTYPE_BRIDGE_HOME - seems to be kind of the top-level of
                //    a bridge.  It references a list of services and a list
                //    of children.  Within the children are ALL the lights for
                //    this bridge.
                //  RTYPE_PRIVATE_GROUP - not sure about this one at all. Can't
                //    even find it in the docs.  Why did I make it?
                //
                RTYPE_GROUP_LIGHT -> {
                    Log.d(TAG, "interpretUpdateEvent() - RTYPE_GROUP_LIGHT")
                    Log.d(TAG, "   updating grouped_light event. owner = ${eventDatum.owner}")

                    // figure out what rtype the owner of this grouped_light event is
                    when (eventDatum.owner?.rtype) {
                        RTYPE_PRIVATE_GROUP -> {
                            // Not interesting for this app.
                            Log.w(TAG, "RTYPE_GROUP_LIGHT -> RTYPE_PRIVATE_GROUP found. Ignoring")
                        }

                        RTYPE_BRIDGE_HOME -> {
                            Log.w(TAG, "RTYPE_GROUP_LIGHT -> RTYPE_BRIDGE_HOME found. Ignoring")
                        }

                        RTYPE_ROOM -> {
                            Log.d(TAG, "interpretUpdateEvent() RTYPE_GROUP_LIGHT / RTYPE_ROOM")
                            // Ah, the owner is a room.  Signal that room to change according to the event.
                            val roomId = eventDatum.owner.rid
                            var room = bridge.value.getRoomById(roomId)
                            if (room != null) {
                                // what changed?  Did the room dim or was it turned on/off?
                                if (eventDatum.dimming != null) {
                                    room = room.copy(brightness = eventDatum.dimming.brightness)
                                }
                                if (eventDatum.on != null) {
                                    room = room.copy(on = eventDatum.on.on)
                                }

                                updateBridgeRooms(room)

                            }
                            else {
                                Log.e(TAG,"Error in interpretUpdateEvent()--can't find room that owns grouped_light event. Aborting!")
                            }
                        }
                        RTYPE_ZONE -> {
                            Log.d(TAG, "interpretUpdateEvent() RTYPE_GROUP_LIGHT / RTYPE_ZONE")
                            // now the owner is a zone. signal the zone change according to the event.
                            val zoneId = eventDatum.owner.rid
                            var zone = bridge.value.getZoneById(zoneId)
                            if (zone != null) {
                                // so what changed? did it dim or turn on/off?
                                if (eventDatum.dimming != null) {
                                    zone = zone.copy(brightness = eventDatum.dimming.brightness)
                                }
                                if (eventDatum.on != null) {
                                    zone = zone.copy(on = eventDatum.on.on)
                                }

                                updateBridgeZones(zone)
                            }
                            else {
                                Log.e(TAG, "interpretUpdateEvent() error: can't find zone in grouped_light event!")
                            }
                        }

                        // all other cases
                        else -> {
                            Log.e(TAG, "interpretUpdateEvent() unknown rtype for eventDatum.owner! rtype = ${eventDatum.owner!!.rtype}")
                        }
                    }
                }

                RTYPE_ROOM -> {
                    Log.d(TAG, "interpretUpdateEvent() - RTYPE_ROOM")
                    if (eventDatum.owner == null) {
                        Log.e(TAG, "interpretUpdateEvent() - can't find the owner of the room! aborting.")
                        continue
                    }
                    // ok, so a room has changed. which one?
                    val changedRoomId = eventDatum.owner.rid
                    val changedRoom = bridge.value.getRoomById(changedRoomId)
                    if (changedRoom == null) {
                        Log.e(TAG, "interpretUpdateEvent() - can't find the room that changed! aborting.")
                        continue
                    }

                    // Check to event and figure out what changed
                    var newRoom = changedRoom
                    if (eventDatum.on != null) {
                        newRoom = changedRoom.copy(on = eventDatum.on.on)
                    }

                    if (eventDatum.dimming != null) {
                        newRoom = changedRoom.copy(brightness = eventDatum.dimming.brightness)
                    }

                    // update the bridge to hold the newly changed room
                    _bridge.update {
                        // redo the rooms list
                        val newRoomList = mutableListOf<PhilipsHueRoomInfo>()
                        it.rooms.forEach { room ->
                            if (room.v2Id == changedRoomId) {
                                newRoomList.add(newRoom)
                            }
                            else { newRoomList.add(room) }
                        }
                        it.copy(rooms = newRoomList)
                    }
                }

                RTYPE_ZONE -> {
                    Log.d(TAG, "interpretUpdateEvent() - RTYPE_ZONE")
                    if (eventDatum.owner == null) {
                        Log.e(TAG, "interpretUpdateEvent() - can't find the owner of the zone! aborting.")
                        continue
                    }
                    // ok, so a zone has changed. which one?
                    val changedZoneId = eventDatum.owner.rid
                    val changedZone = bridge.value.getZoneById(changedZoneId)
                    if (changedZone == null) {
                        Log.e(TAG, "interpretUpdateEvent() - can't find the zone that changed! aborting.")
                        continue
                    }

                    // Check event and figure out what changed
                    var newZone = changedZone
                    if (eventDatum.on != null) {
                        newZone = changedZone.copy(on = eventDatum.on.on)
                    }

                    if (eventDatum.dimming != null) {
                        newZone = changedZone.copy(brightness = eventDatum.dimming.brightness)
                    }

                    // update the bridge to hold the newly changed zone
                    _bridge.update {
                        // redo the zones list
                        val newZoneList = mutableListOf<PhilipsHueZoneInfo>()
                        it.zones.forEach { zone ->
                            if (zone.v2Id == changedZoneId) {
                                newZoneList.add(newZone)
                            }
                            else { newZoneList.add(zone) }
                        }
                        it.copy(zones = newZoneList)
                    }
                }

                RTYPE_SCENE -> {
                    Log.e(TAG, "todo: implement handling updating a scene!")
                }

                else -> {
                    Log.e(TAG, "interpretUpdateEvent() - ${eventDatum.type} is an unknown type of update event!")
                }

            } // when (eventDatum.type)
        }
        Log.d(TAG, "interpretUpdateEvent() done - bridge = ${bridge.value}")    // simple light state looked unchanged
    } // interpretUpdateEvent()

    /**
     * Interprets an ADD sse that was sent from a bridge.
     *
     * @param   event       The event it sent (should be an ADD)
     *
     * side effects
     *  - aspects of this bridge will change based on the event.  For
     *  example: a room's brightness and the status of its light can change.
     *
     * todo: test me!!!
     */
    private suspend fun interpretAddEvent(event: PHv2ResourceServerSentEvent) {
        Log.d(TAG, "interpretAddEvent() $event")

        // go through the data and work on each
        for (eventDatum in event.data) {
            when (eventDatum.type) {
                RTYPE_LIGHT -> {
                    // get all the info that the bridge knows about the light
                    val newLightV2 = PhilipsHueBridgeApi.getLightInfoFromApi(
                        lightId = eventDatum.owner!!.rid,
                        bridgeIp = bridgeIpAddress,
                        bridgeToken = bridgeToken
                    )

                    // check for errors
                    if (newLightV2 == null) {
                        Log.e(TAG, "interpretAddEvent() light cannot be found--ignoring!")
                        continue
                    }
                    if (newLightV2.errors.isNotEmpty()) {
                        Log.e(TAG, "interpretAddEvent() light request found erros:")
                        Log.e(TAG, "    ${newLightV2.errors[0].description}")
                        continue
                    }

//                    _lights.update {
//                        // convert this into our data structure
//                        val newLight = PhilipsHueLightInfo(
//                            lightId = newLightV2.data[0].id,
//                            deviceId = newLightV2.data[0].owner.rid,
//                            name = newLightV2.data[0].metadata.name,
//                            state = PhilipsHueLightState(
//                                on = newLightV2.data[0].on.on,
//                                bri = newLightV2.data[0].dimming.brightness
//                            ),
//                            type = newLightV2.data[0].type,
//                            bridgeIpAddress = bridgeIpAddress
//                        )
//                        Log.d(TAG, "interpretAddEvent() adding light: $newLightV2")
//                        it + newLight
//                    }
                }

                RTYPE_GROUP_LIGHT -> {
                    Log.d(TAG, "interpretAddEvent() adding grouped_light: rtype = ${eventDatum.owner!!.rtype}")
                    when (eventDatum.owner.rtype) {
                        RTYPE_PRIVATE_GROUP -> {
                            // Not interesting for this app.
                            Log.w(TAG, "    RTYPE_GROUP_LIGHT -> RTYPE_PRIVATE_GROUP found. Ignoring")
                        }

                        RTYPE_BRIDGE_HOME -> {
                            Log.w(TAG, "    RTYPE_GROUP_LIGHT -> RTYPE_BRIDGE_HOME found. Ignoring")
                        }

                        RTYPE_ROOM -> {
                            Log.d(TAG, "interpretAddEvent() RTYPE_GROUP_LIGHT / RTYPE_ROOM")
                            // Ah, the owner is a room.  Signal that room is added.
                            val roomId = eventDatum.owner.rid
                            val room = bridge.value.getRoomById(roomId)
                            if (room == null) {
                                Log.e(TAG, "interpretAddEvent()--can't find the added room! id = $roomId")
                                continue
                            }

                            // create a new list of rooms
                            val newRoomsList = mutableListOf<PhilipsHueRoomInfo>()
                            bridge.value.rooms.forEach {
                                newRoomsList.add(it)
                            }
                            newRoomsList.add(room)

                            // add the room to the bridge
                            _bridge.update {
                                it.copy(rooms = newRoomsList)
                            }
                        }

                        RTYPE_ZONE -> {
                            Log.d(TAG, "interpretAddEvent() RTYPE_GROUP_LIGHT / RTYPE_ZONE")
                            // Ah, the owner is a zone.  Signal that a zone is added.
                            val zoneId = eventDatum.owner.rid
                            val zone = bridge.value.getZoneById(zoneId)
                            if (zone == null) {
                                Log.e(TAG, "interpretAddEvent()--can't find the added zone! id = $zoneId")
                                continue
                            }

                            // create a new list of zones
                            val newZonesList = mutableListOf<PhilipsHueZoneInfo>()
                            bridge.value.zones.forEach {
                                newZonesList.add(it)
                            }
                            newZonesList.add(zone)

                            // add the zone to the bridge
                            _bridge.update {
                                it.copy(zones = newZonesList)
                            }
                        }

                    }
                }

                RTYPE_ROOM -> {
                    // todo: add room. NECESSARY!  Needed when adding a room.
                    Log.e(TAG, "interpretAddEvent() adding room not implemented")
                }

                RTYPE_ZONE -> {
                    // todo add zone
                    Log.e(TAG, "interpretAddEvent() adding zone not implemented")
                }
            }
        }

        Log.d(TAG, "interpretAddEvent() done - bridge = ${bridge.value}")
    } // interpretAddEvent()

    /**
     * Similar to [interpretUpdateEvent] and [interpretAddEvent], but for
     * delete events.
     *
     * todo: test me!!!
     */
    private fun interpretDeleteEvent(event: PHv2ResourceServerSentEvent) {
        Log.d(TAG, "interpretDeleteEvent() $event")

        if (devices.value.isEmpty()) {
            Log.e(TAG, "interpretDeleteEvent() is trying to remove a device, but we have no devices!!! Aborting.")
            return
        }

        // go through the data and work on each
        for (eventDatum in event.data) {
            // make sure that there IS a device is at all
            if (eventDatum.owner == null) {
                Log.e(TAG, "interpretDeleteEvent() is trying to remove a device with NULL as the id! aborting.")
                continue
            }
            when (eventDatum.type) {
                RTYPE_LIGHT -> {
                    // find the deleted light and go through all the different things
                    // that could be using it.  Update those things.
                    Log.d(TAG, "interpretDeleteEvent() - light")
                    val lightToDeleteId = eventDatum.owner
                    var deviceToDelete = devices.value[0]
                    for(device in devices.value) {
                        if (device.id == lightToDeleteId.rid) {
                            deviceToDelete = device
                        }
                    }

                    // remove from devices list
                    _devices.update {
                        it - deviceToDelete
                    }

//                    // remove from light list
//                    for(light in lights.value) {
//                        if (light.lightId == lightToDeleteId.rid) {
//                            _lights.update {
//                                it - light
//                            }
//                            break
//                        }
//                    }

                    // find the room that needs updating
                    val roomToUpdate = bridge.value.rooms.find { it.v2Id == deviceToDelete.id }
                    if (roomToUpdate == null) {
                        Log.e(TAG, "interpretDeleteEvent() can't find the room to update! aborting")
                        continue
                    }

                    // zones
                    val zoneToUpdate = bridge.value.zones.find { it.v2Id == deviceToDelete.id }
                    if (zoneToUpdate == null) {
                        Log.e(TAG, "interpretDeleteEvent() can't find the zone to update! aborting")
                        continue
                    }

                    // scenes
                    val sceneToUpdate = bridge.value.scenes.find { it.id == deviceToDelete.id }
                    if (sceneToUpdate == null) {
                        Log.e(TAG, "interpretDeleteEvent() can't find the scene to update! aborting")
                        continue
                    }

                    // remove from bridge
                    _bridge.update { currentBridge ->

                        // any room with this light needs to have this light removed from it
                        val newRoomsList = mutableListOf<PhilipsHueRoomInfo>()
                        currentBridge.rooms.forEach {
                            if (it.v2Id == roomToUpdate.v2Id) {
                                newRoomsList.add(roomToUpdate)
                            }
                            else { newRoomsList.add(it) }
                        }

                        // same for zones
                        val newZoneList = mutableListOf<PhilipsHueZoneInfo>()
                        currentBridge.zones.forEach {
                            if (it.v2Id == zoneToUpdate.v2Id) {
                                newZoneList.add(zoneToUpdate)
                            }
                            else { newZoneList.add(it) }
                        }

                        // scenes
                        val newScenesList = mutableListOf<PHv2Scene>()
                        currentBridge.scenes.forEach {
                            if (it.id == sceneToUpdate.id) {
                                newScenesList.add(sceneToUpdate)
                            }
                            else { newScenesList.add(it) }
                        }

                        val newLightList = mutableListOf<PhilipsHueLightInfo>()
                        currentBridge.lights.forEach {
                            if (it.lightId != lightToDeleteId.rid) {
                                newLightList.add(it)
                            }
                        }

                        currentBridge.copy(
                            rooms = newRoomsList,
                            zones = newZoneList,
                            scenes = newScenesList,
                            lights = newLightList
                        )
                    }

                }

                RTYPE_GROUP_LIGHT -> {
                    Log.d(TAG, "interpretDeleteEvent() - grouped_light")
                    // todo: delete grouped light
                    Log.e(TAG, "interpretDeleteEvent() deleting grouped light not implemented")
                }

                RTYPE_ROOM -> {
                    Log.d(TAG, "interpretDeleteEvent() - room")
                    // todo: delete room. NECESSARY!  Needed when adding a room.
                    Log.e(TAG, "interpretDeleteEvent() deleting room not implemented")
                }

                RTYPE_ZONE -> {
                    Log.d(TAG, "interpretDeleteEvent() - zone")
                    // todo add zone
                    Log.e(TAG, "interpretDeleteEvent() adding zone not implemented")
                }
            }
        }

        Log.d(TAG, "interpretDeleteEvent() done - bridge = ${bridge.value}")

    } // interpretDeleteEvent()

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
        Log.d(TAG, "interpretOpenEvent() done - bridge = ${bridge.value}")
    }


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
            it.rooms.forEach { room ->
                if (room.v2Id == modifiedRoom.v2Id) {
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


    /**
     * Update the bridge flow with a modified scene.
     *
     * side effect:
     *  bridge, _bridge
     */
    private fun updateBridgeScenes(modifiedScene: PHv2Scene) {
        Log.d(TAG, "updateBridgeScenes() BEGIN  bridge ${bridge.value.humanName}")

        val newSceneList = mutableListOf<PHv2Scene>()
        _bridge.update {
            // same story: replace the matching id with the modified scene
            it.scenes.forEach { scene ->
                if (scene.id == modifiedScene.id) {
                    newSceneList.add(modifiedScene)
                }
                else { newSceneList.add(scene) }
            }
            it.copy(scenes = newSceneList)
        }
    }

    /**
     * Just like [updateBridgeRooms] and [updateBridgeScenes].
     */
    private fun updateBridgeZones(modifiedZone: PhilipsHueZoneInfo) {
        Log.d(TAG, "updateBridgeZones() begin.  bridge = ${bridge.value.humanName}")
        val newZoneList = mutableListOf<PhilipsHueZoneInfo>()
        _bridge.update {
            it.zones.forEach { zone ->
                if (zone.v2Id == modifiedZone.v2Id) {
                    newZoneList.add(modifiedZone)
                }
                else { newZoneList.add(zone) }
            }
            it.copy(zones = newZoneList)
        }
    }

}

//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueBridgeModel"