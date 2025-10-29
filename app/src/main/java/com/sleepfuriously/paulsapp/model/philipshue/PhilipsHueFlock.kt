package com.sleepfuriously.paulsapp.model.philipshue

import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.compose.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.compose.philipshue.MIN_BRIGHTNESS

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
    /** Set of the lights */
    val lightSet: Set<PhilipsHueLightInfo>,
    /** Set of rooms used by this flock */
    val roomSet: Set<PhilipsHueRoomInfo>,
    /** Set of all the Zones used by this flock */
    val zoneSet: Set<PhilipsHueZoneInfo>
) {
    /** unique id used for this object. Not perfect, but should be good enough. */
    val id = "${hashCode()}_${System.currentTimeMillis()}"


    /**
     * Returns the IP of all bridges used for rooms.
     */
    fun getRoomBridgesIPs() : Set<String> {
        val bridgeIpSet = mutableSetOf<String>()
        roomSet.forEach { room ->
            bridgeIpSet.add(room.bridgeIpAddress)
        }
        return bridgeIpSet
    }

    /**
     * Returns the IP of all bridges used for zones.
     */
    fun getZoneBridgesIPs() : Set<String> {
        val bridgeIpSet = mutableSetOf<String>()
        zoneSet.forEach { zone ->
            bridgeIpSet.add(zone.bridgeIpAddress)
        }
        return bridgeIpSet
    }

    /**
     * Returns ALL the bridge v2Ids used by this Flock--both zones and rooms.
     */
    fun getBridges() : Set<String> {
        return getRoomBridgesIPs() + getZoneBridgesIPs()
    }

    /**
     * Finds all the rooms in this Flock that are part of the specified bridge.
     *
     * @param   bridgeIpAddress     The v2Id of a bridge that may control our rooms.
     *
     * @return  A List of all the rooms in this Flock controlled by the given
     *          bridge.
     */
    fun getRoomsForBridge(bridgeIpAddress: String) : Set<PhilipsHueRoomInfo> {
        return roomSet.filter { room ->
            room.bridgeIpAddress == bridgeIpAddress
        }.toSet()
    }

    /**
     * Similar to [getRoomsForBridge] but for zones.  Finds all the zones
     * that use the v2Id of the given bridge.
     */
    fun getZonesForBridge(bridgeIpAddress: String) : Set<PhilipsHueZoneInfo> {
        return zoneSet.filter { zone ->
            zone.bridgeIpAddress == bridgeIpAddress
        }.toSet()
    }

}

private const val TAG = "PhilipsHueFlock"