package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.google.gson.Gson
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
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceZonesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
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
    fun convertV2Bridge(
        v2Bridge: PHv2Bridge,
        bridgeIp: String,
        token: String,
        active: Boolean = true,
        connected: Boolean = false
    ) : PhilipsHueBridgeInfo {
        return PhilipsHueBridgeInfo(
            v2Id = v2Bridge.id,
            ipAddress = bridgeIp,
            labelName = v2Bridge.printedNameOnDevice,
            token = token,
            active = active,
            connected = connected
        )
    }


    /**
     * Converts from a [PHv2Light] to a [PhilipsHueLightInfo].
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun convertV2Light(v2Light: PHv2Light) : PhilipsHueLightInfo {

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
     * Takes a [PHv2ResourceLightsAll] and converts it to a Set of
     * [PhilipsHueLightInfo].  If there are errors, the set will
     * be empty.
     */
    @Suppress("unused")
    private fun convertV2ResourceLightsAllToLightSet(
        v2ResourceLight: PHv2ResourceLightsAll,
    ) : Set<PhilipsHueLightInfo> {

        val newLightSet = mutableSetOf<PhilipsHueLightInfo>()

        // check for errors
        if (v2ResourceLight.errors.isNotEmpty()) {
            // errors!
            Log.e(TAG, "errors found in convertV2ResourceLightToLightSet()!")
            Log.e(TAG, "   error = ${v2ResourceLight.errors[0].description}")
            return newLightSet
        }

        if (v2ResourceLight.data.isEmpty()) {
            // nothing here!
            Log.e(TAG, "no data found in convertV2ResourceLightToLightSet()!")
            return newLightSet
        }

        // Loop through all the data.  For each light found,
        // add it to our set.
        v2ResourceLight.data.forEach { data ->
            if (data.type == LIGHT) {
                newLightSet.add(convertV2Light(data))
            }
        }
        return newLightSet
    }

    /**
     * Takes a [PHv2ResourceGroupedLightsAll] and deconstructs into a list
     * of [PHv2GroupedLight]s.  This is way too easy!
     */
    fun convertV2GroupedLights(
        v2GroupedLists: PHv2ResourceGroupedLightsAll,
        bridgeIp: String,
        bridgeToken: String
    ) : List<PHv2GroupedLight> {

        return v2GroupedLists.data
    }

    /**
     * Converts a [PHv2Room] datum into a [PhilipsHueRoomInfo]
     * instance.  This may require gathering more info from the
     * bridge, hence the suspend.
     *
     * @param   v2Room      A room on the bridge as returned directly
     *                      from the API call.
     *
     * @param   bridgeIp    The bridge that this is attached to.
     *                      It better be active!
     *
     * @param   bridgeToken Token for this bridge
     */
    suspend fun convertPHv2Room(
        v2Room: PHv2Room,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueRoomInfo = withContext(Dispatchers.IO) {

        // Do we have any grouped_lights?  This is a great short-cut.
        val groupedLightServices = mutableListOf<PHv2ItemInArray>()
        v2Room.services.forEach { service ->
            if (service.rtype == RTYPE_GROUP_LIGHT) {
                Log.d(TAG, "convertPHv2Room() found a grouped_light, yay! (id = ${service.rid})")
                groupedLightServices.add(service)
            }
        }

        if (groupedLightServices.isNotEmpty()) {
            // Yep, we have some light groups.  But a room should have ONLY one.
            if (groupedLightServices.size > 1) {
                Log.e(TAG, "too many grouped light services in convertPHv2Room!  Aborting!")
                return@withContext PhilipsHueRoomInfo(
                    id = "-1",
                    name = EMPTY_STRING,
                    lights = mutableSetOf(),
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
            val regularLightSet = mutableSetOf<PhilipsHueLightInfo>()
            v2Room.children.forEach { child ->
                if (child.rtype == RTYPE_DEVICE) {
                    // is this device a light?  It's a light iff one of its services is rtype = "light".
                    val device = PhilipsHueBridgeApi.getDeviceIndividualFromApi(
                        deviceRid = child.rid,
                        bridgeIp = bridgeIp,
                        bridgeToken = bridgeToken
                    )
                    if (device.data.isNotEmpty() && (device.data[0].type == DEVICE)) {
                        // yes, it's a device (probably a light)!  To make sure, search its services.
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
                                    regularLightSet.add(regularLight)
                                }
                                else {
                                    Log.e(TAG, "problem finding light in convertPHv2Room()!!!")
                                }
                            }
                        }
                    }
                }
            }

            // everything worked out! return that data
            val newRoom = PhilipsHueRoomInfo(
                id = v2Room.id,
                name = v2Room.metadata.name,
                on = onOff,
                brightness = brightness,
                lights = regularLightSet,
                groupedLightServices = groupedLightServices
            )
            return@withContext newRoom
        }

        // at this point, there's a room, but no lights
        val newRoom = PhilipsHueRoomInfo(
            id = v2Room.id,
            name = v2Room.metadata.name,
            on = false,
            brightness = 0,
            lights = mutableSetOf(),
            groupedLightServices = groupedLightServices
        )

        return@withContext newRoom
    }

    /**
     * Takes a [PHv2RoomIndividual] and converts it to a
     * [PhilipsHueRoomInfo].
     *
     * If [v2RoomIndividual] is in error state (no data), null is returned.
     */
    private suspend fun convertPHv2RoomIndividual(
        v2RoomIndividual: PHv2RoomIndividual,
        bridgeIp: String,
        bridgeToken: String
    ) : PhilipsHueRoomInfo? = withContext(Dispatchers.IO) {

        // first check to make sure original data is valid.
        if (v2RoomIndividual.data.isEmpty()) {
            return@withContext null
        }

        return@withContext convertPHv2Room(
            v2Room = v2RoomIndividual.data[0],
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )
    }


    /**
     * Converts a [PHv2ResourceRoomsAll] into a set of [PhilipsHueRoomInfo].
     *
     * @param   phV2Rooms       The data structure returned from the bridge about
     *                          its rooms.
     * @param   bridgeIp        Ip of this bridge
     * @param   bridgeToken     Token (password) for this bridge
     *
     * @return  A Set of information about the rooms translated from the input.
     */
    suspend fun convertV2RoomAll(
        phV2Rooms: PHv2ResourceRoomsAll,
        bridgeIp: String,
        bridgeToken: String
    ) : Set<PhilipsHueRoomInfo> = withContext(Dispatchers.IO) {

        val newRoomSet = mutableSetOf<PhilipsHueRoomInfo>()

        // check for the case that there is no rooms
        if (phV2Rooms.data.isEmpty()) {
            return@withContext newRoomSet
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
                Log.e(TAG, "convertV2RoomAll(): error getting individual room!")
            }
            else {
                val room = convertPHv2RoomIndividual(
                    v2RoomIndividual = v2RoomIndividual,
                    bridgeIp = bridgeIp,
                    bridgeToken = bridgeToken
                )
                if (room != null) {
                    newRoomSet.add(room)
                }
            }
        }

        return@withContext newRoomSet
    }


    /**
     * Converts a [PHv2ResourceScenesAll] to a list of [PHv2Scene].
     *
     * @return  All the scenes.  Could be an empty list.
     */
    fun convertV2ScenesAll(v2ScenesAll: PHv2ResourceScenesAll) : List<PHv2Scene> {

        // the final return
        val scenes = mutableListOf<PHv2Scene>()

        // check for proper initial resource
        if (v2ScenesAll.errors.isNotEmpty()) {
            Log.e(TAG, "convertV2ScenesAll() is trying to convert data with an error!")
            Log.e(TAG, "   error = ${v2ScenesAll.errors[0].description}")
            return scenes
        }

        return v2ScenesAll.data
    }


    /**
     * Converts a [PHv2ResourceZonesAll] to a list of [PHv2Zone].  Could be
     * empty if no zones exist.
     */
    fun convertV2ZonesAll(v2ZonesAll: PHv2ResourceZonesAll) : List<PHv2Zone> {

        val zones = mutableListOf<PHv2Zone>()

        // being very careful
        if (v2ZonesAll.errors.isNotEmpty()) {
            Log.e(TAG, "convertV2SZonesAll() is trying to convert data with an error!")
            Log.e(TAG, "   error = ${v2ZonesAll.errors[0].description}")
            return zones
        }

        return v2ZonesAll.data
    }
}

private const val TAG = "PhilipsHueDataConverter"