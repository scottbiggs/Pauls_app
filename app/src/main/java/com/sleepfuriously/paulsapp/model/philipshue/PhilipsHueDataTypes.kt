package com.sleepfuriously.paulsapp.model.philipshue

/**
 * Information about a NEW bridge.  It's essentially the same
 * as [PhilipsHueBridgeInfo], but without a few things that
 * aren't needed while constructing a bridge.
 */
data class PhilipsHueNewBridge(
    /** The ip of this bridge in the local network. */
    var ip : String = "",
    /** the name of the bridge as printed on it */
    var labelName: String = "",
    /** The token "name" used to access this bridge. */
    var token : String = "",
    /** When true, this bridge is in active use */
    var active : Boolean = false,
    /** The json string returned by the bridge describing its capabilities */
    var body : String = ""
)

/**
 * Holds info about a philips hue bridge.
 *
 * Note that this has two constructors: a normal one and a second that takes
 * a [PhilipsHueNewBridge] as input.  The second uses a weird invoke operator
 * in the companion object.  It's not intuitive, but seems to work.
 *
 * NOTE
 *  When using the companion object, of course there will be no id as ids
 *  are not part of [PhilipsHueNewBridge] data.  You'll have to put the id
 *  in after.
 */
data class PhilipsHueBridgeInfo(
    /** unique identifier for this bridge which comes from the philips hue v2 id*/
    var id : String,
    /** Name as printed on the bridge itself */
    var labelName : String,
    /** The ip of this bridge in the local network.  Empty means that the ip hasn't been figured out yet. */
    var ip : String = "",
    /** The token "name" used to access this bridge.  Empty means that no token has been created yet. */
    var token : String = "",
    /** When true, this bridge is in active use */
    var active : Boolean = false,
    /** Are we currently listening for events from this bridge? */
    var connected: Boolean,
    /** All the rooms controlled by this bridge */
    var rooms: MutableSet<PhilipsHueRoomInfo> = mutableSetOf(),
    /** The body of the json info was returned by the bridge. defaults to "" */
    var body: String = ""
) {
    companion object {
        /**
         * Used the creating a new bridge
         */
        operator fun invoke(newBridge: PhilipsHueNewBridge) : PhilipsHueBridgeInfo {
            return PhilipsHueBridgeInfo(
                id = "",
                labelName = newBridge.labelName,
                ip = newBridge.ip,
                token = newBridge.token,
                active = newBridge.active,
                connected = false,
                body = newBridge.body
            )
        }
    }
}

/**
 * Describes the essential data of a room in the philips hue world.
 */
data class PhilipsHueRoomInfo(
    val id: String,
    val name: String,
    var on: Boolean = false,
    var brightness : Int = 0,
    val lights: MutableSet<PhilipsHueLightInfo>
)

/**
 * Holds info about a single philips hue light
 */
data class PhilipsHueLightInfo(
    /** the id is also a number to be used to access it directly */
    val id: String,
    val name: String = "",
    var state: PhilipsHueLightState = PhilipsHueLightState(),
    val type: String = "",
    val modelid: String = "",
    val swversion: String = "",
)

/**
 * Defines the state of a light.  Yup, pretty complicated (these lights
 * do a lot!).
 */
data class PhilipsHueLightState(
    /** Simply tells if the light is currently on. */
    var on: Boolean = false,
    /** Range of [0..254]. Not the same as [on]! */
    var bri: Int = 254,
    /** Range [0..65535] */
    var hue: Int = 65535,
    /** Range [0..254] */
    var sat: Int = 254,
    /** Color as an array of xy-coords */
    var xy: Pair<Float, Float> = Pair(0f,0f),
    /** Color temperature of white. 154 (cold) - 500 (warm) */
    var ct: Int = 154,
    /** "select" flashes once, "lselect" flashes repeatedly for 10 seconds */
    var alert: String = "none",
    var effect: String = "none",
    var colormode: String = "ct",
    var reachable: Boolean = true
)

//------------------------------
//  constants
//------------------------------

/** the maximum light that a light can put out */
const val MAX_BRIGHTNESS = 100

/** min light that a light can emit */
const val MIN_BRIGHTNESS = 0