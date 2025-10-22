package com.sleepfuriously.paulsapp.model.philipshue.data

import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Bridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene

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
    val v2Id : String,
    /** Name as printed on the bridge itself. Called "bridge-id" in [PHv2Bridge] */
    var bridgeId : String,
    /** Name of the bridge as given by the user.  Found in the metadata of [PHv2Device]. */
    var humanName : String,
    /** The ip of this bridge in the local network.  Empty means that the ip hasn't been figured out yet. */
    val ipAddress : String = "",
    /** The token "name" used to access this bridge.  Empty means that no token has been created yet. */
    val token : String = "",
    /** When true, this bridge is in active use */
    var active : Boolean = false,
    /** Are we currently listening for events from this bridge? */
    var connected: Boolean,
    /** All the rooms controlled by this bridge */
    val rooms: List<PhilipsHueRoomInfo> = listOf(),
    /** all the scenes for this bridge */
    val scenes: List<PHv2Scene> = listOf(),
    /** all the zones for this bridge */
    val zones: List<PhilipsHueZoneInfo> = listOf()
) {

    companion object {
        /**
         * Used for creating a new bridge
         */
        operator fun invoke(newBridge: PhilipsHueNewBridge) : PhilipsHueBridgeInfo {
            return PhilipsHueBridgeInfo(
                v2Id = "",
                bridgeId = newBridge.labelName,
                humanName = newBridge.humanName,
                ipAddress = newBridge.ip,
                token = newBridge.token,
                active = newBridge.active,
                connected = false
            )
        }
    }

    /**
     * Searches through the current rooms and returns the one that matches
     * the id.  Returns NULL if it can't be found.
     */
    fun getRoomById(roomId: String) : PhilipsHueRoomInfo? {
        return rooms.find { it.v2Id == roomId }
    }

    /**
     * Finds one of the current scene by its id.  Returns NULL if not found.
     */
    fun getSceneById(sceneId: String) : PHv2Scene? {
        return scenes.find { it.id == sceneId }
    }

    /**
     * Finds the zone with the given id.  Returns NULL if not found.
     */
    fun getZoneById(zoneId: String) : PhilipsHueZoneInfo? {
        return zones.find { it.v2Id == zoneId }
    }

}
