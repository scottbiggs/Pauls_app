package com.sleepfuriously.paulsapp.model.philipshue.json

/**
 * Use this class to initialize a Philips Hue bridge and
 * possibly connect it to the app.
 */
class PhilipsHueBridgeInit {


    /**
     * To obtain the token for accessing a bridge, we have to tell that
     * bridge a name for this app (username).  This function makes
     * sure that it's done in a consistent manner.
     *
     * The username is a constant + the ip of that particular bridge.
     * This makes sure that we use different user names for each
     * bridge (just in case there's some sort of confusion).
     *
     * Not sure if the user name needs to be remembered or not.  I guess
     * I'll find out soon enough!  (The token: yeah, that needs to be
     * remembered!)
     */
    private fun constructUserName(bridgeIp: String) : String {
        return PHILIPS_HUE_BRIDGE_DEVICE_NAME_PREFIX + bridgeIp
    }



}


//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueBridgeInit"


/** append this to the bridge's ip to get the debug screen */
const val PHILIPS_HUE_BRIDGE_TEST_SUFFIX = "/debug/clip.html"

/**
 * When registering this app with a bridge, we need to tell it what kind of device
 * this is.  Here's the string.
 */
private const val PHILIPS_HUE_BRIDGE_DEVICE_TYPE = "sleepfuriously android client"

/**
 * Device ids (usernames) are used to get the token from bridges.  The username will
 * be the following string + the ip of this bridge.  You can construct this
 * by calling [PhilipsHueBridgeInit.constructUserName].
 */
private const val PHILIPS_HUE_BRIDGE_DEVICE_NAME_PREFIX = "sleepfuriously_p"

