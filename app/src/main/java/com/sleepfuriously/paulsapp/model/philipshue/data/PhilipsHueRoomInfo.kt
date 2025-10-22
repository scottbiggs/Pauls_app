package com.sleepfuriously.paulsapp.model.philipshue.data

import com.sleepfuriously.paulsapp.compose.philipshue.MIN_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray

/**
 * Describes the essential data of a room in the philips hue world.
 */
data class PhilipsHueRoomInfo(
    val v2Id: String,
    val name: String,
    /** Human-readable name for the current scene. Use empty string if not applicable. */
    val currentSceneName: String,
    var on: Boolean = false,
    var brightness : Int = MIN_BRIGHTNESS,
    val lights: List<PhilipsHueLightInfo>,
    /** References to group of lights in this room.  Even though it's an array, there should be just 1. */
    val groupedLightServices: List<PHv2ItemInArray>,
    /** IP of the bridge that controls this room (used to differentiate between bridges) */
    val bridgeIpAddress: String
)

