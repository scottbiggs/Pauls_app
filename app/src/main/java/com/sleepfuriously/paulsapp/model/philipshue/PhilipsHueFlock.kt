package com.sleepfuriously.paulsapp.model.philipshue

import com.sleepfuriously.paulsapp.compose.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight

/**
 * Basic data class for Flocks, a data class of my own invention.
 * Flocks are light groups that span bridges.
 *
 * Should only be instantiated by [PhilipsHueFlockModel].
 */
data class PhilipsHueFlock(
    /** human-readable name.  Does not have to be unique */
    val name: String,
    /** The name of the currently displayed scene. Empty if not applicble (default) */
    val currentSceneName: String = "",
    /** brightness from 0 to [MAX_BRIGHTNESS] */
    val brightness: Int,
    /** on = true, off = false */
    val onOffState: Boolean,
    /** All the bridges used by this Flock */
    val bridgeSet: Set<PhilipsHueBridgeInfo>,
    /** All the lights used by this Flock */
    val lightSet: Set<PhilipsHueLightInfo>,
    /**
     * A group of lights that belong to the same bridge.  All the lights in
     * all the groups = the lights controlled by this Flock.  Each lightGroup
     * knows what bridge controls it.
     */
    val lightGroups: Set<PhilipsHueLightGroup>
) {
    /** unique id used for this object. Not perfect, but should be good enough. */
    val id = "${hashCode()}_${System.currentTimeMillis()}"


    /**
     * Returns ALL the bridge v2Ids used by this Flock--both zones and rooms.
     */
    fun getBridgeIds() : List<String> {
        return bridgeSet.map { it.bridgeId }
    }

}

//-------------------------------------------------------------

/**
 * Defines a light group for my system.  Related to [PHv2GroupedLight].
 */
data class PhilipsHueLightGroup(
    /** The v2Id that the bridge uses to access this light group */
    val groupId: String,
    /** v2Id of the bridge that controls this group of lights */
    val bridgeId: String,
    /** all the v2Ids of the lights that are in this group */
    val lightIds: Set<String>,
) {
    /** unique id used for this object. Not perfect, but should be good enough. */
    val id = "${hashCode()}lightGroup${System.currentTimeMillis()}"
}


private const val TAG = "PhilipsHueFlock"