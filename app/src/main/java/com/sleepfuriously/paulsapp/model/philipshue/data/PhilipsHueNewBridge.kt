package com.sleepfuriously.paulsapp.model.philipshue.data

/**
 * Information about a NEW bridge.  It's essentially the same
 * as [PhilipsHueBridgeInfo], but without a few things that
 * aren't needed while constructing a bridge.
 *
 * todo: deprecate and remove this
 */
data class PhilipsHueNewBridge(
    /** The ip of this bridge in the local network. */
    var ip : String = "",
    /** the name of the bridge as printed on it */
    var labelName: String = "",
    /** name of the bridge given by the user */
    var humanName : String,
    /** The token "name" used to access this bridge. */
    var token : String = "",
    /** When true, this bridge is in active use */
    var active : Boolean = false,
    /** The json string returned by the bridge describing its capabilities */
    var body : String = ""
)
