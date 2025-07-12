package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.MyResponse
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousGet
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPut
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Error
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLightIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2LightIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceBridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceDeviceIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceGroupedLightsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceRoomsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceSceneIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceScenesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceZoneIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceZonesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Room
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2RoomIndividual
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_GROUP_LIGHT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Basic control access to the bridge api.
 *
 * These CRUD functions are suspend functions and will return values.
 * How the data is handled and passed along is up to the caller.
 *
 * NOTE
 *  The handling of first connecting to a bridge is done in a different
 *  place (right now [PhilipsHueModel]).
 */
object PhilipsHueBridgeApi {

    //-------------------------
    //  create
    //-------------------------

    //-------------------------
    //  read
    //-------------------------

    //------------
    //  bridge
    //

    /**
     * Gets all the information about a particular bridge.  This is straight
     * from the bridge itself.  It can easily be parsed with [PHv2ResourceBridge].
     *
     * @return      The JSON string that was returned in the body of the api
     *              call to the bridge.
     *              Returns empty string if the bridge doesn't exist, token
     *              is not recognized, or some other error.
     */
    suspend fun getBridgeDataStrFromApi(bridgeIp: String, token: String) : String {

        val fullAddress = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_BRIDGE
        )

        val response = synchronousGet(
            url = fullAddress,
            header = Pair(HEADER_TOKEN_KEY, token),
            trustAll = true     // fixme: when we start higher security
        )

        if (response.isSuccessful) {
            return response.body
        }
        return EMPTY_STRING
    }

    /**
     * Gets the [PHv2ResourceBridge] data from a bridge.  According to
     * the docs, this is IDENTICAL to getting a bridge with the bridge id.
     * Makes sense as a bridge can only know about itself.
     *
     * This call is useful when you don't much info about the bridge yet.
     * You'll get that info here!
     */
    suspend fun getBridge(
        bridgeIp: String,
        token: String
    ) : PHv2ResourceBridge = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_BRIDGE
        )
        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, token),
            trustAll = true
        )

        if (response.isSuccessful) {
            return@withContext PHv2ResourceBridge(response.body)
        }
        else {
            Log.e(TAG, "unable to get bridge info (ip = ${bridgeIp}!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2ResourceBridge(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    //------------
    //  devices
    //
    /**
     * Given a device's id (or RID), this will retrieve the data from
     * the bridge.  Essentially just sets up the API call for you.
     *
     * If there's an error, the error portion is filled in.
     */
    suspend fun getDeviceIndividualFromApi(
        deviceRid: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceDeviceIndividual = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_DEVICE/$deviceRid"
        )
        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return@withContext PHv2ResourceDeviceIndividual(response.body)
        }
        else {
            Log.e(TAG, "getDeviceIndividualFromApi() unable to get device from bridge (ip = ${bridgeIp}!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2ResourceDeviceIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    //------------
    //  lights & grouped_lights
    //

    /**
     * Asks the bridge to return ALL the grouped lights that it knows about.
     *
     * @return  Will return null on network error (bridge may be turned off, etc.)
     *          Otherwise a grouped light resource is returned.  It may have errors
     *          of its own, which will be described in the data structure.
     */
    suspend fun getAllGroupedLightsFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceGroupedLightsAll? {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_GROUPED_LIGHTS
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true
        )

        // check for html errors
        if (response.isSuccessful == false) {
            Log.e(TAG, "getAllGroupedLightsFromApi() unsuccessful attempt at getting ALL grouped_lights!  bridge ip = ${bridgeIp}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            return null
        }

        val groupedLights = PHv2ResourceGroupedLightsAll(JSONObject(response.body))
        return groupedLights
    }

    /**
     * Given an id for a light group, this goes to the bridge and
     * gets all the info it can about it.
     *
     * note
     *      AFAIK, grouped_lights doesn't know about the lights that it controls.  It's a kind
     *      of setting for a bunch of lights.  The room (or zone?) applies this to all
     *      the lights that it controls.
     *
     * @return  [PHv2GroupedLightIndividual] instance with all the info on this light group.
     */
    suspend fun getGroupedLightFromApi(
        groupId: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2GroupedLightIndividual? = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true
        )

        if (response.isSuccessful == false) {
            Log.e(TAG, "unsuccessful attempt at getting grouped_lights!  groupId = $groupId, bridge ip = ${bridgeIp}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            // returning null
            return@withContext null
        }

        val group = PHv2GroupedLightIndividual(JSONObject(response.body))
        return@withContext group
    }

    /**
     * Returns the first grouped light found in the given room.
     * As a room, it shouldn't have more than one group.
     *
     * @param   v2Room      This is a [PHv2Room].  That is, it's
     *                      taken from the data section of the
     *                      class that's actually returned from the
     *                      API.
     *
     * @return  The [PHv2GroupedLight] that's within.  Null will
     *          be returned if no grouped lights are found.
     */
    private suspend fun getGroupedLightFromRoom(
        v2Room: PHv2Room,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2GroupedLight? = withContext(Dispatchers.IO) {
        v2Room.children.forEach { child ->
            if (child.rtype == RTYPE_GROUP_LIGHT) {
                val groupedLightIndividual = getGroupedLightFromApi(
                    groupId = child.rid,
                    bridgeIp = bridgeIp,
                    bridgeToken = bridgeToken
                )

                // quick sanity check
                if ((groupedLightIndividual != null) && (groupedLightIndividual.data.isNotEmpty())) {
                    val groupedLight = groupedLightIndividual.data[0]
                    return@withContext groupedLight
                }
            }
        }
        // not found
        return@withContext null
    }

    /**
     * Retrieves info about a specific light, given its id.
     * Returns null on error (light can't be found).
     */
    suspend fun getLightInfoFromApi(
        lightId: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2LightIndividual? = withContext(Dispatchers.IO) {
        // construct the url
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_LIGHTS/$lightId"
        )

        val response = synchronousGet(
            url = url,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
            trustAll = true     // todo: be paranoid in the future
        )

        if (response.isSuccessful == false) {
            Log.e(TAG, "getLightInfoFromApi() unsuccessful attempt at getting light data!  lightId = $lightId, bridge ip = ${bridgeIp}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            return@withContext null
        }

        val lightData = PHv2LightIndividual(response.body)
        return@withContext lightData
    }

    //------------
    //  rooms
    //

    /**
     * Finds all the rooms with the associated bridge.
     *
     * @return      Set of all rooms found with this bridge
     */
    suspend fun getAllRoomsFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceRoomsAll {

        val fullAddress = createFullAddress(
            prefix = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
            ip = bridgeIp,
            suffix = SUFFIX_GET_ROOMS
        )

        // get info from bridge
        val response = synchronousGet(
            url = fullAddress,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true
        )

        Log.d(TAG, "getting response for url: $fullAddress")
        val roomsData = PHv2ResourceRoomsAll(response.body)
        Log.d(TAG, "got rooms: $roomsData")

        return roomsData
    }

    /**
     * Given the id (v2) of a room, this gets the individual info
     * from the bridge about that room.
     *
     * Note that if there's an error, then that info will also be in
     * the data that's returned (in the error json array).
     */
    suspend fun getRoomIndividualFromApi(
        roomId: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2RoomIndividual = withContext(Dispatchers.IO) {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_ROOMS/$roomId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return@withContext PHv2RoomIndividual(response.body)
        }
        else {
            Log.e(TAG, "getRoomIndividual() unable to get room from bridge!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2RoomIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    //------------
    //  scenes
    //

    /**
     * Gets just one scene from a bridge.
     *
     * @param   sceneId     The id for this scene
     *
     * @param   bridge      The bridge to question about this scene
     *
     * @return      The scene.  If an error, then the error component of the
     *              data is filled in.
     */
    suspend fun getSceneIndividualFromApi(
        sceneId: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceSceneIndividual {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_SCENES/$sceneId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return PHv2ResourceSceneIndividual(JSONObject(response.body))
        }
        else {
            Log.e(TAG, "getSceneIndividualFromApi unable to get scene from bridge (ip = ${bridgeIp})!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return PHv2ResourceSceneIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    /**
     * Returns the [PHv2ResourceScenesAll] from the specified bridge.  Errors
     * will be embedded in the  returned data.
     */
    suspend fun getAllScenesFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceScenesAll {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_SCENES
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return PHv2ResourceScenesAll(JSONObject(response.body))
        }
        else {
            Log.e(TAG, "getAllScenesFromApi() unable to get scenes from bridge (ip = ${bridgeIp})!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return PHv2ResourceScenesAll(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    //------------
    //  zones
    //

    /**
     * Returns a [PHv2ResourceZonesAll] from the specified bridge.  Errors
     * will be embedded in the  returned data.
     */
    suspend fun getAllZonesFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceZonesAll {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_ZONES
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return PHv2ResourceZonesAll(JSONObject(response.body))
        }
        else {
            Log.e(TAG, "getAllZonesFromApi unable to get zones from bridge (ip = ${bridgeIp})!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return PHv2ResourceZonesAll(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    /**
     * Gets just one zone from a bridge.
     *
     * @param   zoneId      The id for this zone
     *
     * @param   bridge      The bridge to question about this zone
     *
     * @return      The zone.  If an error, then the error component of the
     *              data is filled in.
     */
    suspend fun getZoneIndividualFromApi(
        zoneId: String,
        bridgeIp: String,
        bridgeToken: String
    ) : PHv2ResourceZoneIndividual {

        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_GET_ZONES/$zoneId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridgeToken),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return PHv2ResourceZoneIndividual(JSONObject(response.body))
        }
        else {
            Log.e(TAG, "getZoneIndividualFromApi() unable to get zone from bridge (ip = ${bridgeIp})!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return PHv2ResourceZoneIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }


    //-------------------------
    //  update
    //-------------------------

    /**
     * Sends a PUT request to the bridge to make this scene active.
     * The scene knows which room (group) it's in.
     *
     * @return  The response from the PUT request.  No analysis is done.
     */
    suspend fun sendSceneToRoom(
        bridgeIp: String,
        bridgeToken: String,
        sceneToDisplay: PHv2Scene
    ) : MyResponse {
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = "$SUFFIX_PUT_ACTIVATE_SCENE/${sceneToDisplay.id}"
        )

        val response = synchronousPut(
            url = url,
            bodyStr = UPDATE_SCENE_BODY,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, bridgeToken)),
            trustAll = true     // fixme: change when using secure stuff
        )
        return response
    }

    //-------------------------
    //  delete
    //-------------------------

    //-------------------------
    //  helpers
    //-------------------------

    /**
     * Constructs the complete address from the constituent parts.
     *
     * @param   ip      The main part of the address.  This is generally thought
     *                  to be the domain name + ".com" or whatever domain suffix
     *                  is appropriate.  In our use, this also can contain the
     *                  prefix, which is usually "http://" or "https://" as well.
     *                  Also in our cases, the domain is rarely used--we use the
     *                  translated ip number, like "196.192.86.1".  So the most
     *                  common case is:  "http://196.192.86.1"
     *
     * @param   prefix  If the prefix is not contained in the ip, then this holds
     *                  it.  Generally this will be "https://" or "http://".
     *
     * @param   suffix  Anything that goes AFTER the ip.  This is the directory
     *                  or parameters or whatever.  Remember that this should start
     *                  with a backslash ('/').
     */
    fun createFullAddress(
        ip: String,
        prefix: String = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
//        prefix: String = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
        suffix: String
    ) : String {

        val fullAddress = "${prefix}$ip${suffix}"
        Log.i(TAG,"createFullddress() -> $fullAddress")
        return fullAddress
    }


}

//------------------------------------
//  constants
//------------------------------------

private const val TAG = "PhilipsHueBridgeApi"

//----------------
//  prefixes
//
/** this prefix should appear before the numbers of the bridge's ip address when testing */
const val PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX = "https://"
const val PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX = "http://"

//----------------
//  headers
//
/** The header key when using a token/username for Philips Hue API calls */
const val HEADER_TOKEN_KEY = "hue-application-key"

//----------------
//  suffixes
//
/** This is v1. It should ONLY be used when getting the token/username from bridge. */
const val SUFFIX_API = "/api/"

/** Used to get all the rooms associated with a bridge.  Goes AFTER the token */
const val SUFFIX_GET_ROOMS = "/clip/v2/resource/room"

/** Suffix for gathering info about a scene from a bridge. */
const val SUFFIX_GET_SCENES = "/clip/v2/resource/scene"

/** Suffix for getting zone info from a bridge */
const val SUFFIX_GET_ZONES = "/clip/v2/resource/zone"



/**
 * Used to get info about a the bridge's lights.
 *
 * To get info about a specific light, add a slash and put the id AFTER!
 */
const val SUFFIX_GET_LIGHTS = "/clip/v2/resource/light"

/**
 * Gets all the devices.  The usual addition to get just one device details.
 */
const val SUFFIX_GET_DEVICE = "/clip/v2/resource/device"

/** Gets info about the bridge */
const val SUFFIX_GET_BRIDGE = "/clip/v2/resource/bridge"

/**
 * Gets all the light groups for this bridge.
 *
 * For a specific light group, add a slash and then the id.
 */
const val SUFFIX_GET_GROUPED_LIGHTS = "/clip/v2/resource/grouped_light"

/** Suffix for making a scene active.  Needs to be followed by "/<scene_id>". */
const val SUFFIX_PUT_ACTIVATE_SCENE = "/clip/v2/resource/scene"

//----------------
//  bodies (for POSTs and PUTs)
//

/**
 * This body tells a scene to make itself active.
 */
const val UPDATE_SCENE_BODY = "{\"recall\": {\"action\": \"active\"}}"