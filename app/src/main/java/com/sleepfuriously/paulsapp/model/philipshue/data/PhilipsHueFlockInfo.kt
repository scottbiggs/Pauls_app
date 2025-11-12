package com.sleepfuriously.paulsapp.model.philipshue.data

import com.sleepfuriously.paulsapp.compose.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.generateV2Id
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight

/**
 * Basic data class for Flocks, a data class of my own invention.
 * Flocks are light groups that span bridges.
 *
 * Should only be instantiated by [com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueFlockModel].
 */
data class PhilipsHueFlockInfo(
    /** unique id used for this object. Use generateV2Id() for a pretty good one. */
    val id: String,
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
    /** All the Rooms used by this Flock */
    val roomSet: Set<PhilipsHueRoomInfo>,
    /** All the Zones used by this Flock */
    val zoneSet: Set<PhilipsHueZoneInfo>
) {
    /**
     * Returns ALL the bridge v2Ids used by this Flock--both zones and rooms.
     */
    fun getBridgeIds() : List<String> {
        return bridgeSet.map { it.bridgeId }
    }

    /**
     * Given the ID of a Room, this returns the Bridge that controls
     * this room.  Returns null if not found.
     */
    fun getBridgeForRoom(roomId: String) : PhilipsHueBridgeInfo? {
        // simply go through the rooms until we find a matching id
        roomSet.forEach { room ->
            if (room.v2Id == roomId) {
                // Id matched, so find the bridge with that Id
                return bridgeSet.find { it.ipAddress == room.bridgeIpAddress }
            }
        }
        // not found
        return null
    }

    /**
     * Finds the bridge that controls the specified Zone. Returns null if
     * not found.  Similar to [getBridgeForRoom]
     */
    fun getBridgeForZone(zoneId: String) : PhilipsHueBridgeInfo? {
        zoneSet.forEach { zone ->
            if (zone.v2Id == zoneId) {
                return bridgeSet.find { it.ipAddress == zone.bridgeIpAddress }
            }
        }
        return null
    }


}

//-------------------------------------------------------------

/**
 * Defines a light group for my system.  Related to [PHv2GroupedLight].
 */
data class PhilipsHueLightGroup(
    /** unique id used for this object. Use generateV2Id() for a pretty good one. */
    val id: String,
    /** The v2Id that the bridge uses to access this light group */
    val groupId: String,
    /** v2Id of the bridge that controls this group of lights */
    val bridgeId: String,
    /** all the v2Ids of the lights that are in this group */
    val lightIds: Set<String>
)


private const val TAG = "PhilipsHueFlockInfo"