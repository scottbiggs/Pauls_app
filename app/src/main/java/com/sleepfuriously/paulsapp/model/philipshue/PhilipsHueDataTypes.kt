package com.sleepfuriously.paulsapp.model.philipshue

/**
 * Holds info about a philips hue bridge.
 */
data class PhilipsHueBridgeInfo(
    /** unique identifier for this bridge */
    val id : String,
    /** The ip of this bridge in the local network.  Null means that the ip hasn't been figured out yet. */
    var ip : String? = null,
    /** The token "name" used to access this bridge.  Null means that no token has been created yet. */
    var token : String? = null,
    /** When was this bridge last accessed? */
    var lastUsed: Long = 0L,
    /** When true, this bridge is in active use */
    var active : Boolean = false,
    /** All the rooms controlled by this bridge */
    val rooms: MutableSet<PhilipsHueRoomInfo> = mutableSetOf()
)

/**
 * Describes the essential data of a room in the philips hue world.
 */
data class PhilipsHueRoomInfo(
    val id: String,
    val lights: MutableSet<PhilipsHueLightInfo>
) {
    /**
     * Returns the average illumination of the lights that are currently ON.
     */
    fun getAverageIllumination() : Int {
        var illumination = 0
        var onLightCount = 0
        lights.forEach { light ->
            if (light.state.on) {
                illumination += light.state.bri
                onLightCount++
            }
        }
        if (onLightCount == 0) {
            return 0
        }
        illumination /= onLightCount
        return illumination
    }
}

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
    /** looks like there are 8 of these, with keys "1", "2"..."8" */
    var pointsymbol: MutableMap<String, String> = mutableMapOf(
        "1" to "none",
        "2" to "none",
        "3" to "none",
        "4" to "none",
        "5" to "none",
        "6" to "none",
        "7" to "none",
        "8" to "none"
    )
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

/**
 * Use this class to modify the current state of a light
 */
data class PhilipsHueLightOnOffData (
    /** Turn the light on or off. */
    val on: Boolean,
    /** Set light brightness to range of [0..254]. Not the same as turning it off! */
    val bri: Int = 254,
    /** Range [0..65535] */
    val hue: Int = 65535,
    /** Range [0..254] */
    val sat: Int = 254,
    /** Color as an array of xy-coords */
    val xy: Pair<Float, Float> = Pair(0f, 0f),
    /** Color temperature of white. 154 (cold) - 500 (warm) */
    val ct: Int = 154,
    /** "select" flashes once, "lselect" flashes repeatedly for 10 seconds */
    val alert: String = "none",
    /** time for transition in centiseconds */
    val transitiontime: Int = 0
)


//------------------------------
//  constants
//------------------------------

/** the maximum light that a light can put out */
const val MAX_BRIGHTNESS = 254

/** min light that a light can emit */
const val MIN_BRIGHTNESS = 0