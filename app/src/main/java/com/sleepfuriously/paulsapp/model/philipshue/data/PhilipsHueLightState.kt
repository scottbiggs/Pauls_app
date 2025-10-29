package com.sleepfuriously.paulsapp.model.philipshue.data

/**
 * Defines the state of a light.  Yup, pretty complicated (these lights
 * do a lot!).
 */
data class PhilipsHueLightState(
    /** Simply tells if the light is currently on. */
    val on: Boolean = false,
    /** Range [0..100] */
    val bri: Int = 100,
    /** Range [0..65535] */
    val hue: Int = 65535,
    /** Range [0..254] */
    val sat: Int = 254,
    /** Color as an array of xy-coords */
    val xy: Pair<Float, Float> = Pair(0f,0f),
    /** Color temperature of white. 154 (cold) - 500 (warm) */
    val ct: Int = 154,
    /** "select" flashes once, "lselect" flashes repeatedly for 10 seconds */
    val alert: String = "none",
    val effect: String = "none",
    val colormode: String = "ct",
    val reachable: Boolean = true
)

