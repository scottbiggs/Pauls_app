package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.OkHttpUtils
import com.sleepfuriously.paulsapp.model.OkHttpUtils.getCodeFromResponse
import com.sleepfuriously.paulsapp.model.getPrefsString
import com.sleepfuriously.paulsapp.model.setPrefsString

/***********************
 * Suite of functions and variables that involve the Philips Hue
 * Bridge.
 *
 * todo:  alter for multiple bridges (both active and inactive)
 */

//-----------------------------------
//  private variables
//-----------------------------------

/**
 * Holds the token needed to access the bridge.  The first
 * call to [getBridgeToken] will set this variable
 * (or it will remain null if no token has been created yet).
 *
 * This is made purely for speed as the token can always be
 * retrieved from the prefs if necessary.
 */
private var philipsHueBridgeToken : String? = null


/**
 * String representation of the local ip for the philips hue bridge.
 * The first call the [getBridgeIPStr] will set this variable
 * (assuming it is stored in the prefs).
 *
 * Null means that no ip has been found yet.
 */
private var philipsHueBridgeIp : String? = null


//-----------------------------------
//  public functions
//-----------------------------------

/**
 * Utilities associated with the Philips Hue bridge device.
 *
 * Some of these functions may take an indeterminate time to
 * complete and should be done within a coroutine on a thread
 * different from the main ui.
 *
 * DETAILS
 *
 * The Philips Hue bridge is the central device of the Philips Hue
 * lights.  It contains all the information about the light system
 * and is the sole point of contact for this app.
 *
 * The initalization process requires knowing the local
 */


/**
 * Tests to see if the bridge is at the current ip.
 * If the ip is not known, this returns false.
 * Token is not used.
 *
 * WARNING:
 *  This must be called off the main thread as it access
 *  the network.
 */
@Suppress("RedundantIf")
suspend fun doesBridgeRespondToIp(ctx: Context) : Boolean {

    // exit with false if no ip
    val ip = philipsHueBridgeIp ?: return false

    val response = OkHttpUtils.synchronousGetRequest("$ip$PHILIPS_HUE_BRIDGE_TEST_SUFFIX")
    Log.d(TAG, "doesBridgeRespondToIp -> $response")

    // check this response.  It should have a code=200
    val code = getCodeFromResponse(response)
    Log.d(TAG, "code = $code")

    if (response.isSuccessful) {
        return true
    }

    return false
}

/**
 * Finds the current "name" for the philips hue bridge.  The name is
 * actually a token necessary for any real communication with the
 * bridge.  This is the fastest way to get the token and  is fine
 * running on the main thread.
 *
 * If the bridge has not been initialized, then this name won't
 * exists (returns null).
 *
 * The token (name) is stored in a shared pref (encrypted eventually).
 * The first call to getBridgeToken() retrieves that token.
 * Successive calls use that same token.
 *
 * NOTE:    This does care if the bridge is currently active or
 *          not.  This simply returns any token that has been
 *          recorded for this app.
 *
 * NOTE:    Does not check that the token is correct.  This can
 *          happen if connected to a different bridge or the
 *          bridge has forgotten this token somehow.
 *
 * @return      The token (String) needed to access the bridge.
 *              Null if no token has been assigned.
 */
fun getBridgeToken(ctx: Context) : String? {

    if (philipsHueBridgeToken == null) {
        // attempt to get the token from our saved data
        philipsHueBridgeToken = getPrefsString(ctx, PHILIPS_HUE_BRIDGE_TOKEN_KEY)
    }
    return philipsHueBridgeToken
}

/**
 * Sets the given string to be the new philips hue bridge
 * token.  This also stores the token in long-term memory.
 */
fun setBridgeToken(ctx: Context, newToken: String) {
    philipsHueBridgeToken = newToken
    setPrefsString(ctx, PHILIPS_HUE_BRIDGE_TOKEN_KEY, newToken)
}


/**
 * Retrieves a string version of the ip.  If it has not been
 * loaded yet, grabs it from the prefs.  If it STILL is not found,
 * then null is returned.
 *
 * @return      The ip of the bridge in String form.  Null if not found.
 */
fun getBridgeIPStr(ctx: Context) : String? {
    if (philipsHueBridgeIp == null) {
        philipsHueBridgeIp = getPrefsString(ctx, PHILIPS_HUE_BRIDGE_IP_KEY)
    }
    return philipsHueBridgeIp
}

/**
 * Sets the ip of the bridge.  Overwrites any existing ip.
 *
 * @param   ctx     of course
 *
 * @param   newIp   String representation of the ip to access the bridge
 *                  locally.
 */
fun setBridgeIpStr(ctx: Context, newIp: String) {
    philipsHueBridgeIp = newIp
    setPrefsString(ctx, PHILIPS_HUE_BRIDGE_IP_KEY, newIp)
}

//-----------------------------------
//  private functions
//-----------------------------------



//-----------------------------------
//  classes & enums
//-----------------------------------

/**
 * The states of the Philips Hue bridge.
 */
enum class PhilipsHueBridgeStatus {

    /** the bridge has not been initialized yet */
    BRIDGE_UNINITIALIZED,
    /** currently attempting to initialize the bridge */
    BRIDGE_INITIALIZING,
    /** attempt to initialize the bridge has timed out */
    BRIDGE_INITIALIZATION_TIMEOUT,
    /** successfully initialized */
    BRIDGE_INITIALIZED,
    /** some error has occurred when dealing with the bridge */
    ERROR
}


//-----------------------------------
//  constants
//-----------------------------------

private const val TAG = "PhilipsHueBridgeUtils"

/** key to get the token for the philips hue bridge from prefs */
private const val PHILIPS_HUE_BRIDGE_TOKEN_KEY = "ph_bridge_token_key"

/** key to get ip from prefs */
private const val PHILIPS_HUE_BRIDGE_IP_KEY = "ph_bridge_ip_key"

/** append this to the bridge's ip to get the debug screen */
private const val PHILIPS_HUE_BRIDGE_TEST_SUFFIX = "/debug/clip.html"
