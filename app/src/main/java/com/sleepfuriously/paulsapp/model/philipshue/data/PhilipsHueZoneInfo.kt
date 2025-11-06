package com.sleepfuriously.paulsapp.model.philipshue.data

import com.sleepfuriously.paulsapp.compose.philipshue.MIN_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone

/**
 * Similar to [PhilipsHueRoomInfo], but a zone can span several rooms.
 * Furthermore, lights can belong to more than one zone, making zones
 * much more flexible than rooms.
 *
 * This is derived from [PHv2Zone].
 */
data class PhilipsHueZoneInfo(
    val v2Id: String,
    val name: String,
    /** Human-readable name for the current scene. Use empty string if not applicable. */
    val currentSceneName: String,
    var on: Boolean = false,
    var brightness : Int,
    val lights: List<PhilipsHueLightInfo>,
    /** References to group of lights in this room.  Even though it's an array, there should be just 1. */
    val groupedLightServices: List<PHv2ItemInArray>,
    /** IP of the bridge that controls this Zone (used to differentiate between bridges) */
    val bridgeIpAddress: String
)
