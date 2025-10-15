package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.google.gson.Gson
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi.getAllDevicesFromApi
import com.sleepfuriously.paulsapp.model.philipshue.json.BRIDGE_V2
import com.sleepfuriously.paulsapp.model.philipshue.json.BRIDGE_V3
import com.sleepfuriously.paulsapp.model.philipshue.json.DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.PHBridgePostTokenResponse
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Bridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Light
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceBridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceGroupedLightsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceLightsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceRoomsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Room
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2RoomIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceScenesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceZoneIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceZonesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.ZONE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use this to convert between different data types (primarily
 * between PHv2... and PhilipsHue...
 */
object PhilipsHueDataConverter {

    /**
     * This takes a string, converts it to Gson, then looks around to
     * find the token (name) part.  This token is then returned.
     *
     * @param       bodyStr     The body as returned from a POST or GET
     *                          response.
     *
     * @return      The token within the contents of the string.
     *              Null if not found.
     */
    fun getTokenFromBodyStr(bodyStr: String) : String? {

        Log.d(TAG, "getTokenFromBodyStr() started. bodyStr = $bodyStr")

        // convert to our kotlin data class
        val bridgeResponseData = Gson().fromJson(bodyStr, PHBridgePostTokenResponse::class.java)
            ?: return null      // just return null if bridgeResponse is null

        if (bridgeResponseData[0].success != null) {
            // looks like there was a success
            val success = bridgeResponseData[0].success!!
            val token = success.username
            val clientKey = success.clientkey
            Log.d(TAG, "getTokenFromBodyStr() successful. token = $token, clientKey = $clientKey")

            if (token.isEmpty() || clientKey.isEmpty()) {
                Log.e(TAG, "getTokenFromBodyStr()--thought we were successful, but token or clientKey is empty!")
                return null
            }
            return token
        }
        else {
            Log.e(TAG, "Error in getTokenFromBodyStr()! Unable to parse bodyStr = $bodyStr")
            return null
        }
    }

    //------------------
    //  bridges
    //------------------

    /**
     * Converts a [PHv2ResourceBridge] to [PhilipsHueBridgeInfo]
     *
     * @param   v2Bridge        The data struct that was returned by the bridge
     * @param   bridgeIp        Ip used to contact this bridge
     * @param   token           Token (password) used to access this bridge
     * @param   active          Is this bridge active? defaults to True.
     * @param   connected       Is this bridge sending sse? Defaults to False.
     *
     * Note: Scenes, Rooms, and Zones are completely ignored as this info is not
     * in [PHv2ResourceBridge].
     */
    suspend fun convertV2BridgeToPhilipsHueBridgeInfo(
        v2Bridge: PHv2Bridge,
        bridgeIp: String,
        token: String,
        active: Boolean = true,
        connected: Boolean = false
    ) : PhilipsHueBridgeInfo = withContext(Dispatchers.IO) {

        val username = getBridgeUsername(
            bridgeIp = bridgeIp,
            bridgeToken = token
        )

        return@withContext PhilipsHueBridgeInfo(
            v2Id = v2Bridge.id,
            ipAddress = bridgeIp,
            humanName = username,
            bridgeId = v2Bridge.printedNameOnDevice,
            token = token,
            active = active,
            connected = connected
        )
    }

    /**
     * Gets the username for a given bridge.  Overkill, but should work.
     * If you already have a list of devices for this bridge, call [convertDevicesToBridgeUsername]
     * which will be MUCH faster.
     */
    suspend fun getBridgeUsername(
        bridgeIp: String,
        bridgeToken: String
    ) : String = withContext(Dispatchers.IO) {
        val allDevices = getAllDevicesFromApi(
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )
        if (allDevices.isEmpty()) {
            Log.e(TAG, "getBridgeUsername() - problem getting devices! returning 'error bridge'.")
            return@withContext "error bridge"
        }

        return@withContext convertDevicesToBridgeUsername(allDevices)
    }

    //------------------
    //  lights
    //------------------

    /**
     * Converts from a [PHv2Light] to a [PhilipsHueLightInfo].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun convertV2LightToPhilipsHueLightInfo(
        v2Light: PHv2Light
    ) : PhilipsHueLightInfo {

        val state = PhilipsHueLightState(
            on = v2Light.on.on,
            bri = v2Light.dimming.brightness,
        )

        val light = PhilipsHueLightInfo(
            lightId = v2Light.id,
            deviceId = v2Light.owner.rid,
            name = v2Light.metadata.name,
            state = state,
            type = v2Light.type,
        )

        return light
    }

    /**
     * Takes a [PHv2ResourceLightsAll] and converts it to a List of
     * [PhilipsHueLightInfo].  If there are errors, the list will
     * be empty.
     */
    @Suppress("unused")
    private fun convertV2ResourceLightsAllToLightList(
        v2ResourceLight: PHv2ResourceLightsAll,
    ) : List<PhilipsHueLightInfo> {

        val newLightList = mutableListOf<PhilipsHueLightInfo>()

        // check for errors
        if (v2ResourceLight.errors.isNotEmpty()) {
            // errors!
            Log.e(TAG, "errors found in convertV2ResourceLightsAllToLightList()!")
            Log.e(TAG, "   error = ${v2ResourceLight.errors[0].description}")
            return newLightList
        }

        if (v2ResourceLight.data.isEmpty()) {
            // nothing here!
            Log.e(TAG, "no data found in convertV2ResourceLightsAllToLightList()!")
            return newLightList
        }

        // Loop through all the data.  For each light found,
        // add it to our list.
        v2ResourceLight.data.forEach { data ->
            if (data.type == LIGHT) {
                newLightList.add(convertV2LightToPhilipsHueLightInfo(data))
            }
        }
        return newLightList
    }

    /**
     * Children of things like Zones and Rooms often have lists of lights,
     * which are useful to know.  This helper function finds those lights
     * and converts them to a List of [PhilipsHueLightInfo].
     *
     * @param   children        List of [PHv2ItemInArray] items.
     *
     * @return      A useful list of [PhilipsHueLightInfo].  Will be empty
     *              if none are found.
     */
    private suspend fun getLightListFromV2ItemInArrayList(
        children:  List<PHv2ItemInArray>,
        bridgeIp: String,
        bridgeToken: String
    ) :  List<PhilipsHueLightInfo> {

        val regularLightList = mutableListOf<PhilipsHueLightInfo>()
        children.forEach { child ->
            if (child.rtype == RTYPE_DEVICE) {
                // Oddly, lights are listed here as devices.  But to distinguish
                // it from others, we have to search its services.
                val device = PhilipsHueBridgeApi.getDeviceIndividualFromApi(
                    deviceRid = child.rid,
                    bridgeIp = bridgeIp,
                    bridgeToken = bridgeToken
                )
                if (device.data.isNotEmpty() && (device.data[0].type == DEVICE)) {
                    // yes it's a device and probably a light, but to be sure we search its services
                    device.data[0].services.forEach { service ->
                        if (service.rtype == RTYPE_LIGHT) {
                            val v2light = PhilipsHueBridgeApi.getLightInfoFromApi(
                                lightId = service.rid,
                                bridgeIp = bridgeIp,
                                bridgeToken = bridgeToken
                            )
                            if (v2light != null) {
                                val regularLight = PhilipsHueLightInfo(
                                    lightId = v2light.data[0].id,
                                    deviceId = v2light.data[0].owner.rid,
                                    name = v2light.data[0].metadata.name,
                                    state = PhilipsHueLightState(
                                        on = v2light.data[0].on.on,
                                        bri = v2light.data[0].dimming.brightness
                                    ),
                                    type = v2light.data[0].type
                                )
                                regularLightList.add(regularLight)
                            }
                            else {
                                Log.e(TAG, "getLightListFromV2ItemInArrayList() problem finding light!!!")
                            }
                        }
                    }
                }
            }
        }
        return regularLightList
    }

    //------------------
    //  grouped lights
    //------------------

    /**
     * Takes a [PHv2ResourceGroupedLightsAll] and deconstructs into a list
     * of [PHv2GroupedLight]s.  This is way too easy!
     */
    fun convertV2GroupedLightsAllToV2GroupedLights(
        v2GroupedLists: PHv2ResourceGroupedLightsAll
    ) : List<PHv2GroupedLight> {
        return v2GroupedLists.data
    }

    //------------------
    //  rooms
    //------------------

    /**
     * Converts a [PHv2Room] datum into a [PhilipsHueRoomInfo]
     * instance.  This may require gathering more info from the
     * bridge, hence the suspend.  It's generally part of a larger
     * data call like [PHv2RoomIndividual] or [PHv2ResourceRoomsAll].
     *
     * @param   v2Room      A room on the bridge as returned directly
     *                      from the API call.
     *
     * @param   bridgeIp    The bridge that this is attached to.
     *                      It better be active!
     *
     * @param   bridgeToken Token for this bridge
     *
     * @return  A converted room.
     *          On error, the room will have a v2Id and name of empty strings.
     */
    suspend fun convertPHv2RoomToPhilipsHueRoomInfo(
        v2Room: PHv2Room,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueRoomInfo = withContext(Dispatchers.IO) {

        // Do we have any grouped_lights?  This is a great short-cut.
        val groupedLightServices = mutableListOf<PHv2ItemInArray>()
        v2Room.services.forEach { service ->
            if (service.rtype == RTYPE_GROUP_LIGHT) {
                Log.v(TAG, "convertV2RoomToPhilipsHueRoomInfo() found a grouped_light, yay! (id = ${service.rid})")
                groupedLightServices.add(service)
            }
        }

        if (groupedLightServices.isNotEmpty()) {
            // Yep, we have some light groups.  But a room should have ONLY one.
            if (groupedLightServices.size > 1) {
                Log.e(TAG, "too many grouped light services in convertV2RoomToPhilipsHueRoomInfo!  Aborting!")
                return@withContext PhilipsHueRoomInfo(
                    v2Id = EMPTY_STRING,
                    name = EMPTY_STRING,
                    lights = mutableListOf(),
                    groupedLightServices = groupedLightServices
                )
            }

            // The grouped_light has info on on/off and brightness
            val groupedLight = PhilipsHueBridgeApi.getGroupedLightFromApi(
                groupId = groupedLightServices[0].rid,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            var onOff = false
            var brightness = 0
            if ((groupedLight != null) && (groupedLight.errors.isEmpty())) {
                onOff = groupedLight.data[0].on.on
                brightness = groupedLight.data[0].dimming.brightness
            }

            // get the lights
            val regularLightList = getLightListFromV2ItemInArrayList(
                children = v2Room.children,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )

            // everything worked out! return that data
            val newRoom = PhilipsHueRoomInfo(
                v2Id = v2Room.id,
                name = v2Room.metadata.name,
                on = onOff,
                brightness = brightness,
                lights = regularLightList,
                groupedLightServices = groupedLightServices
            )
            return@withContext newRoom
        }

        // at this point, there's a room, but no lights
        val newRoom = PhilipsHueRoomInfo(
            v2Id = v2Room.id,
            name = v2Room.metadata.name,
            on = false,
            brightness = 0,
            lights = mutableListOf(),
            groupedLightServices = groupedLightServices
        )

        return@withContext newRoom
    } // convertPHv2RoomToPhilipsHueRoomInfo()

    /**
     * Converts a [PHv2ResourceRoomsAll] into a list of [PhilipsHueRoomInfo].
     *
     * @param   phV2Rooms       The data structure returned from the bridge about
     *                          its rooms.
     * @param   bridgeIp        Ip of this bridge
     * @param   bridgeToken     Token (password) for this bridge
     *
     * @return  A List of information about the rooms translated from the input.
     */
    suspend fun convertPHv2ResourceRoomsAllToPhilipsHueRoomInfoList(
        phV2Rooms: PHv2ResourceRoomsAll,
        bridgeIp: String,
        bridgeToken: String
    ) : List<PhilipsHueRoomInfo> = withContext(Dispatchers.IO) {

        val newRoomList = mutableListOf<PhilipsHueRoomInfo>()

        // check for the case that there is no rooms
        if (phV2Rooms.data.isEmpty()) {
            return@withContext newRoomList
        }

        phV2Rooms.data.forEach { v2Room ->
            // check correct type error (should never happen)
            if (v2Room.type != ROOM) {
                Log.e(TAG, "Error!  Room (id = ${v2Room.id} has wrong type! Type = ${v2Room.type}, but should be 'room'!")
                return@forEach      // this is the equivalent of continue (will skip this iteration)
            }

            val v2RoomIndividual = PhilipsHueBridgeApi.getRoomIndividualFromApi(
                roomId = v2Room.id,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            if (v2RoomIndividual.errors.isNotEmpty()) {
                Log.e(TAG, "convertV2RoomAllToPhilipsHueRoomInfoList(): error getting individual room!")
            }
            else {
                val room = convertPHv2RoomIndividualToPhilipsHueRoomInfo(
                    v2RoomIndividual = v2RoomIndividual,
                    bridgeIp = bridgeIp,
                    bridgeToken = bridgeToken
                )
                if (room != null) {
                    newRoomList.add(room)
                }
            }
        }

        return@withContext newRoomList
    }

    /**
     * Takes a [PHv2RoomIndividual] and converts it to a
     * [PhilipsHueRoomInfo].
     *
     * If [v2RoomIndividual] is in error state (no data), null is returned.
     */
    private suspend fun convertPHv2RoomIndividualToPhilipsHueRoomInfo(
        v2RoomIndividual: PHv2RoomIndividual,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueRoomInfo? = withContext(Dispatchers.IO) {

        // first check to make sure original data is valid.
        if (v2RoomIndividual.data.isEmpty()) {
            return@withContext null
        }

        return@withContext convertPHv2RoomToPhilipsHueRoomInfo(
            v2Room = v2RoomIndividual.data[0],
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )
    }

    //------------------
    //  zones
    //------------------

    /**
     * Converts a [PHv2ResourceZonesAll] to a List of [PHv2Zone].  Could be
     * empty if no zones exist.
     */
    fun convertPHv2ZonesAllToPHv2ZoneList(
        v2ZonesAll: PHv2ResourceZonesAll
    ) : List<PHv2Zone> {

        val zones = mutableListOf<PHv2Zone>()

        // being very careful
        if (v2ZonesAll.errors.isNotEmpty()) {
            Log.e(TAG, "convertV2ZonesAllToV2Zone() is trying to convert data with an error!")
            Log.e(TAG, "   error = ${v2ZonesAll.errors[0].description}")
            return zones
        }

        return v2ZonesAll.data
    }

    /**
     * Converts the class read for all the zones, [PHv2ResourceZonesAll] into
     * a list of [PhilipsHueZoneInfo].  This is the zone equivalent of
     * [convertPHv2ResourceRoomsAllToPhilipsHueRoomInfoList].
     */
    suspend fun convertPHv2ZonesAllToPhilipsHueZoneInfoList(
        v2ZonesAll: PHv2ResourceZonesAll,
        bridgeIp: String,
        bridgeToken: String
    ) : List<PhilipsHueZoneInfo> = withContext(Dispatchers.IO) {

        val newZoneList = mutableListOf<PhilipsHueZoneInfo>()

        // go through the data and find the zones
        v2ZonesAll.data.forEach { zone ->
            if (zone.type != ZONE) {
                // idiot check--should never happen
                Log.e(TAG, "convertPHv2ZonesAllToPhilipsHueZoneInfoList() - room (id = ${zone.id} has wrong type!")
                Log.e(TAG, "    Type = ${zone.type}, but should be '$ZONE'!")
                return@forEach      // (aka continue)
            }

            val v2ZoneIndividual = PhilipsHueBridgeApi.getZoneIndividualFromApi(
                zoneId = zone.id,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            if (v2ZoneIndividual.errors.isNotEmpty()) {
                Log.e(TAG, "convertPHv2ZonesAllToPhilipsHueZoneInfoList() error getting individual zone!")
                Log.e(TAG, "    error: ${v2ZoneIndividual.errors[0].description}")
                return@forEach      // (aka continue)
            }

            // so far so good
            val zone = convertPHv2ZoneIndividualToPhilipsHueZoneInfo(
                v2ZoneIndividual = v2ZoneIndividual,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            if (zone != null) {
                newZoneList.add(zone)
            }
        }
        return@withContext newZoneList
    }

    /**
     * Takes a [PHv2ResourceZoneIndividual] and converts it to a [PhilipsHueZoneInfo].
     * If [v2ZoneIndividual] is in error, null is returned.
     */
    suspend fun convertPHv2ZoneIndividualToPhilipsHueZoneInfo(
        v2ZoneIndividual: PHv2ResourceZoneIndividual,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueZoneInfo? = withContext(Dispatchers.IO) {

        // first check to make sure original data is valid
        if (v2ZoneIndividual.data.isEmpty()) {
            Log.e(TAG, "convertPHv2ZoneIndividualToPhilipsHueZoneInfo() error with data!")
            Log.e(TAG, "    error msg: ${v2ZoneIndividual.errors[0].description}")
            return@withContext null
        }

        return@withContext convertPHv2ZoneToPhilipsHueZoneInfo(
            v2Zone = v2ZoneIndividual.data[0],
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )
    }

    /**
     * Converts a [PHv2Zone] to [PhilipsHueZoneInfo].
     */
    suspend fun convertPHv2ZoneToPhilipsHueZoneInfo(
        v2Zone: PHv2Zone,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueZoneInfo = withContext(Dispatchers.IO) {

        // Are there any grouped lights?  This should be a nice short-cut.
        val groupedLightServices = mutableListOf<PHv2ItemInArray>()
        v2Zone.services.forEach { service ->
            if (service.rtype == RTYPE_GROUP_LIGHT) {
                Log.v(TAG, "convertPHv2ZoneToPhilipsHueZoneInfo() found a grouped light  :)")
                groupedLightServices.add(service)
            }
        }

        if (groupedLightServices.isNotEmpty()) {
            // I believe a grouped light should have only 1 service.
            if (groupedLightServices.size > 1) {
                Log.e(TAG, "convertPHv2ZoneToPhilipsHueZoneInfo() found too many grouped light services! Aborting!")
                return@withContext PhilipsHueZoneInfo(
                    v2Id = EMPTY_STRING,
                    name = EMPTY_STRING,
                    lights = listOf(),
                    groupedLightServices = listOf()
                )
            }

            // the light group has info about its on/off status and brightness
            val groupedLight = PhilipsHueBridgeApi.getGroupedLightFromApi(
                groupId = groupedLightServices[0].rid,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            var onOff = false
            var brightness = 0
            if ((groupedLight != null) && (groupedLight.errors.isEmpty())) {
                onOff = groupedLight.data[0].on.on
                brightness = groupedLight.data[0].dimming.brightness
            }

            // get all the individual lights
            val regularLightList = getLightListFromV2ItemInArrayList(
                children = v2Zone.children,
                bridgeIp = bridgeIp,
                bridgeToken = bridgeToken
            )
            return@withContext PhilipsHueZoneInfo(
                v2Id = v2Zone.id,
                name = v2Zone.metadata.name,
                on = onOff,
                brightness = brightness,
                lights = regularLightList,
                groupedLightServices = groupedLightServices
            )

        }
        else {
            // We have a zone, but it has no lights
            return@withContext PhilipsHueZoneInfo(
                v2Id = v2Zone.id,
                name = v2Zone.metadata.name,
                lights = mutableListOf(),
                groupedLightServices = groupedLightServices
            )
        }
    } // convertPHv2ZoneToPhilipsHueZoneInfo()

    //------------------
    //  scenes
    //------------------

    /**
     * Converts a [PHv2ResourceScenesAll] to a list of [PHv2Scene].
     *
     * @return  All the scenes.  Could be an empty list.
     */
    fun convertV2ScenesAllToV2Scene(v2ScenesAll: PHv2ResourceScenesAll) : List<PHv2Scene> {

        // the final return
        val scenes = mutableListOf<PHv2Scene>()

        // check for proper initial resource
        if (v2ScenesAll.errors.isNotEmpty()) {
            Log.e(TAG, "convertV2ScenesAllToV2Scene() is trying to convert data with an error!")
            Log.e(TAG, "   error = ${v2ScenesAll.errors[0].description}")
            return scenes
        }

        return v2ScenesAll.data
    }

    //------------------
    //  devices
    //------------------

    /**
     * Sifts through all the given devices until a bridge is found (uses metadata).
     *
     * @return  The user name for this bridge.  Error will return empty string.
     */
    fun convertDevicesToBridgeUsername(devices: List<PHv2Device>) : String {
        var username = ""

        for (device in devices) {
            if ((device.metadata.archetype == BRIDGE_V2) ||
                (device.metadata.archetype == BRIDGE_V3)) {
                username = device.metadata.name
                break
            }
        }
        if (username.isEmpty()) {
            Log.e(TAG, "convertDevicesToBridgeUsername() cannot find bridge in devices! Using 'foo bridge'.")
            username = "foo bridge"
        }
        return username
    }

}

private const val TAG = "PhilipsHueDataConverter"