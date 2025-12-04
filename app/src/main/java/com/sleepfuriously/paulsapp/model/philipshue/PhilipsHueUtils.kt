package com.sleepfuriously.paulsapp.model.philipshue

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.utils.OkHttpUtils.synchronousGet
import com.sleepfuriously.paulsapp.utils.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightState
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceIdentifier
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_LIGHT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.streams.asSequence

/**
 * Collection of general utilities that pertain to dealing with
 * Philips Hue devices.
 */

//-------------------------------------
//  functions
//-------------------------------------

/**
 * Helper function.  From a list of [PHv2ResourceIdentifier]s, this finds the
 * first that is of type [RTYPE_LIGHT], finds it's full info, and returns its
 * [com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo] form.
 *
 * @return  Null if a light can't be found or there was some sort of error.
 */
private suspend fun getLightInfoFromServiceList(
    serviceList: List<PHv2ResourceIdentifier>,
    bridge: PhilipsHueBridgeInfo
) : PhilipsHueLightInfo? {

    serviceList.forEach { service ->
        if (service.rtype == RTYPE_LIGHT) {
            // yep found it!
            val v2ApiLight = PhilipsHueApi.getLightInfoFromApi(
                lightId = service.rid,
                bridgeIp = bridge.ipAddress,
                bridgeToken = bridge.token
            )
            if (v2ApiLight == null) {
                Log.w(TAG, "Problem getting light in getLightInfoFromServiceList()!  rid = ${service.rid}")
                return null
            }

            if (v2ApiLight.errors.isNotEmpty()) {
                Log.w(TAG, "Error occurred getting light in getLightInfoFromServiceList()!")
                Log.w(TAG, "   error = ${v2ApiLight.errors[0]}")
            }

            // convert to our PhilipsHueLightInfo
            return PhilipsHueLightInfo(
                lightId = v2ApiLight.data[0].id,
                deviceId = v2ApiLight.data[0].owner.rid,
                name = v2ApiLight.data[0].metadata.name,
                state = PhilipsHueLightState(
                    on = v2ApiLight.data[0].on.on,
                    bri = v2ApiLight.data[0].dimming.brightness
                ),
                type = v2ApiLight.data[0].type,
                bridgeIpAddress = bridge.ipAddress
            )
        }
    }
    // nothing found
    return null
}


/**
 * Another helper.  This finds the room that a light resides in.
 * Returns null if not found.
 */
private fun findRoomFromLight(
    light: PhilipsHueLightInfo,
    bridge: PhilipsHueBridgeInfo
) : PhilipsHueRoomInfo? {
    bridge.rooms.forEach { room ->
        room.lights.forEach { maybeThisLight ->
            if (maybeThisLight.lightId == light.lightId) {
                Log.d(TAG, "found the room that holds light (id = ${light.lightId}")
                return room
            }
        }
    }
    Log.d(TAG, "Could not find the room that holds light (id = ${light.lightId}. Sorry.")
    return null
}


/**
 * Goes through all the lights in this bridge and returns the one that
 * matches the given id.  It also checks device ids FOR lights (as they
 * are sometimes used interchangeably).
 *
 * Returns null if not found.
 */
fun findLightFromId(
    id: String,
    bridge: PhilipsHueBridgeInfo
) : PhilipsHueLightInfo? {

    // go through all the rooms
    bridge.rooms.forEach { room ->
        room.lights.forEach { light ->
            if (light.lightId == id) {
                Log.d(TAG, "Found the light in findLightFromId(id = $id), yay!")
                return light
            }
            if (light.deviceId == id) {
                Log.d(TAG, "Found the light from its deviceId in findLightFromId(id = $id), whew!")
                return light
            }
        }
    }
    Log.d(TAG, "did not find light (id = $id) in findLightFromId().  Sigh.")
    return null
}


/**
 * Finds out if a bridge currently is active and responds to its token.
 *
 * @param       bridgeIp    Ip of the bridge in question.  Does not check to see
 *                          if everything is right.
 *
 * @param       token       The token (username) to test.
 *
 * @return      True if the bridge is working and responds positively.
 *              False otherwise.
 */
suspend fun doesBridgeAcceptToken(bridgeIp: String, token: String) :
        Boolean = withContext(Dispatchers.IO) {

    // try getting the bridge's general info to see if the token works
    val fullAddress = PhilipsHueApi.createFullAddress(
        ip = bridgeIp,
        suffix = SUFFIX_GET_BRIDGE,
    )

    // Just want a response, nothing fancy
    val response = synchronousGet(
        fullAddress,
        header = Pair(HEADER_TOKEN_KEY, token),
        trustAll = true
    )
    return@withContext response.isSuccessful
}

/**
 * Alternate version of [doesBridgeAcceptToken]
 */
suspend fun doesBridgeAcceptToken(bridge: PhilipsHueBridgeInfo) :
        Boolean = withContext(Dispatchers.IO) {
    return@withContext doesBridgeAcceptToken(bridge.ipAddress, bridge.token)
}

/**
 * Tests to see if a bridge is at the given ip.
 *
 * This just tests to see if the basic debug screen appears.
 * The url is  http://<ip>/debug/clip.html
 *
 * @param   ip          The ip that may point to a philips hue bridge
 *
 * WARNING:
 *  This must be called off the main thread as it access
 *  the network.
 */
suspend fun doesBridgeRespondToIp(ip: String) : Boolean {

    // exit with false if no ip or wrong format
    if (ip.isEmpty() || (isValidBasicIp(ip) == false)){
        Log.d(TAG, "doesBridgeRespondToIp($ip) - ip is empty or not valid! Aborting!")
        return false
    }

    try {
        val fullIp = PhilipsHueApi.createFullAddress(
//                prefix = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
            prefix = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
            ip = ip,
            suffix = PHILIPS_HUE_BRIDGE_TEST_SUFFIX
        )
        Log.v(TAG, "doesBridgeRespondToIp() requesting response from $fullIp")

        val myResponse = synchronousGet(fullIp)

        if (myResponse.isSuccessful) {
            return true
        }
    }
    catch (e: IOException) {
        Log.e(TAG, "error when testing the bridge (IOException).  The ip ($ip) is probably bad.")
        e.printStackTrace()
        return false
    }
    catch (e: IllegalArgumentException) {
        Log.e(TAG, "error when testing the bridge (IllegalArgumentException).  The ip (\"$ip\") is probably bad.")
        e.printStackTrace()
        return false
    }

    return false
}

/**
 * Alternate version that takes a bridge instead of an IP.
 */
suspend fun doesBridgeRespondToIp(bridge: PhilipsHueBridgeInfo) : Boolean {
    return doesBridgeRespondToIp(bridge.ipAddress)
}



/**
 * Use this to generate the body for when trying to get a new
 * token (username) from the bridge.
 *
 * @return  The json we're constructing will look something like this:
 *      {"devicetype":"appName#instanceName", "generateclientkey":true}
 *
 * @param   appName         The name of the app (defaults to Pauls App)
 *
 * @param   instanceName    The name of the instance of this app.
 *                          Since mulitple copies of this app may be
 *                          accessing the same bridge, this needs to be
 *                          a unique number.  Default should generate a
 *                          very good one.
 */
fun generateGetTokenBody(
    appName: String? = null,
    instanceName: String? = null,
    ctx: Context
) : String {
    val name = appName ?: getDefaultAppName(ctx)

    val instance = instanceName ?: getDefaultInstanceName(ctx)
    return """{"devicetype": "$name#$instance", "generateclientkey":true}"""
}

/**
 * Figures out the default app name for this app.
 */
fun getDefaultAppName(ctx: Context) : String {
    return ctx.getString(R.string.app_name)
}

/**
 * Figures out the default instance for this app.  Uses hardware stuff
 * so special permission is needed.
 */
@SuppressLint("HardwareIds")
fun getDefaultInstanceName(ctx: Context) : String {

    // This tries to use the ANDROID_ID.  If it's null, then I just
    // throw in a JellyBean (should only happen on jelly bean devices, hehe).
    val instanceName = Secure.getString(ctx.contentResolver, Secure.ANDROID_ID)
        ?: "jellybean"
    return instanceName
}


/**
 * Checks to see if the given bridge is active.
 */
suspend fun isBridgeActive(bridge: PhilipsHueBridgeInfo) : Boolean = withContext(Dispatchers.IO) {

    if (doesBridgeRespondToIp(bridge) && doesBridgeAcceptToken(bridge)) {
        return@withContext true
    }
    return@withContext false
}

/**
 * Alternate version of [isBridgeActive].  This version only needs
 * the ip and token (not the whole bridge).
 */
suspend fun isBridgeActive(
    bridgeIpStr: String,
    bridgeToken: String
) : Boolean = withContext(Dispatchers.IO) {
    if ((doesBridgeRespondToIp(ip = bridgeIpStr)) &&
        (doesBridgeAcceptToken(bridgeIpStr, bridgeToken))) {
        return@withContext true
    }

    return@withContext false
}


/**
 * Use this handy function to create an almost-certainly unique id.  It'll
 * be in the form that the API expects, and the chances that it's a repeat
 * are astronomical.
 */
fun generateV2Id() : String {
    val p1 = generateIdHelper(8)
    val p2 = generateIdHelper(4)
    val p3 = generateIdHelper(4)
    val p4 = generateIdHelper(4)
    val p5 = generateIdHelper(12)
    return "$p1-$p2-$p3-$p4-$p5"
}

/**
 * found here:
 *      https://stackoverflow.com/a/46944275/624814
 */
private fun generateIdHelper(length: Int) : String {
    val source = "0123456789abcdef"
    return java.util.Random().ints(length.toLong(), 0, source.length)
        .asSequence()
        .map(source::get)
        .joinToString("")
}

//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueUtils"

/** append this to the bridge's ip to get the debug screen */
const val PHILIPS_HUE_BRIDGE_TEST_SUFFIX = "/debug/clip.html"

