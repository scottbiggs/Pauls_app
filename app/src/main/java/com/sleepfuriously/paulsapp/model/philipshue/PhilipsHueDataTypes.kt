package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Bridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Device
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone

/**
 * todo - this file should be renamed PhilipsHueInterpretedDataTypes
 *  remember to rename the [TAG] too
 */


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
    /** name of the bridge given by the user */
    var humanName : String,
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
//    /**
//     * This should cause the flow to update correctly.
//     */
//    override fun equals(other: Any?): Boolean {
//        val otherBridge = other as PhilipsHueBridgeInfo
//        if (v2Id != otherBridge.v2Id) {
//            Log.d(TAG, "bridge.equals() -> false -> v2Id")
//            return false
//        }
//        if (bridgeId != otherBridge.bridgeId) {
//            Log.d(TAG, "bridge.equals() -> false -> bridgeId")
//            return false
//        }
//        if (ipAddress != otherBridge.ipAddress) {
//            Log.d(TAG, "bridge.equals() -> false -> ipAddress")
//            return false
//        }
//        if (token != otherBridge.token) {
//            Log.d(TAG, "bridge.equals() -> false -> token")
//            return false
//        }
//        if (active != otherBridge.active) {
//            Log.d(TAG, "bridge.equals() -> false -> active")
//            return false
//        }
//        if (connected != otherBridge.connected) {
//            Log.d(TAG, "bridge.equals() -> false -> connected")
//            return false
//        }
//        if (rooms.size != otherBridge.rooms.size) {
//            Log.d(TAG, "bridge.equals() -> false -> room.size (this.size = ${rooms.size}, other.size = ${otherBridge.rooms.size}")
//            return false
//        }
//        rooms.forEachIndexed { i, room ->
//            if (otherBridge.rooms[i] != room) {
//                // failing here
//                Log.d(TAG, "bridge.equals() -> false -> otherBridge.rooms[$i] != ${room.name})")
//                return false
//            }
//        }
//        if (scenes.size != otherBridge.scenes.size) {
//            Log.d(TAG, "bridge.equals() -> false -> scenes.size (this.size = ${scenes.size}, other.size = ${otherBridge.scenes.size}")
//            return false
//        }
//        scenes.forEachIndexed { i, scene ->
//            if (otherBridge.scenes[i] != scene) {
//                Log.d(TAG, "bridge.equals() -> false -> otherBridge.scenes[$i] != ${scene.metadata.name}")
//                return false
//            }
//        }
//        zones.forEachIndexed { i, zone ->
//            if (otherBridge.zones[i] != zone) {
//                Log.d(TAG, "bridge.equals() -> false -> otherBridge.zones[$i] != $zone)")
//                return false
//            }
//        }
//
//        Log.d(TAG, "equals() -> TRUE")
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = v2Id.hashCode()
//        result = 31 * result + bridgeId.hashCode()
//        result = 31 * result + ipAddress.hashCode()
//        result = 31 * result + token.hashCode()
//        result = 31 * result + active.hashCode()
//        result = 31 * result + connected.hashCode()
//        result = 31 * result + rooms.hashCode()
//        result = 31 * result + scenes.hashCode()
//        result = 31 * result + zones.hashCode()
//        return result
//    }

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

/**
 * Describes the essential data of a room in the philips hue world.
 */
data class PhilipsHueRoomInfo(
    val v2Id: String,
    val name: String,
    var on: Boolean = false,
    var brightness : Int = MIN_BRIGHTNESS,
    val lights: List<PhilipsHueLightInfo>,
    /** References to group of lights in this room.  Even though it's an array, there should be just 1. */
    val groupedLightServices: List<PHv2ItemInArray>
) {
//    /**
//     * Overriden equals = for [PhilipsHueRoomInfo].
//     */
//    override fun equals(other: Any?): Boolean {
//        val otherRoom = other as PhilipsHueRoomInfo
//        if (otherRoom.id != id) {
//            Log.d(TAG, "roomInfo.equals() -> false: id($id) != otherRoom.id(${otherRoom.id})")
//            return false
//        }
//        if (otherRoom.name != name) {
//            Log.d(TAG, "roomInfo.equals() -> false: name")
//            return false
//        }
//        if (otherRoom.on != on) {
//            Log.d(TAG, "roomInfo.equals() -> false: on")
//            return false
//        }
//        if (otherRoom.brightness != brightness) {
//            Log.d(TAG, "roomInfo.equals() -> false: brightness")
//            return false
//        }
//        if (otherRoom.lights.size != lights.size) {
//            Log.d(TAG, "roomInfo.equals() -> false: lights.size")
//            return false
//        }
//        otherRoom.lights.forEachIndexed { i, otherLight ->
//            if (lights[i] != otherLight) {
//                Log.d(TAG, "roomInfo.equals() -> false: lights[$i] != $otherLight)")
//                return false
//            }
//        }
//        otherRoom.groupedLightServices.forEachIndexed {i, lightGroup ->
//            if (groupedLightServices[i] != lightGroup) {
//                Log.d(TAG, "roomInfo.equals() -> false: a lightGroup doesn't match!")
//                return false
//            }
//        }
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        var result = id.hashCode()
//        result = 31 * result + name.hashCode()
//        result = 31 * result + on.hashCode()
//        result = 31 * result + brightness
//        result = 31 * result + lights.hashCode()
//        result = 31 * result + groupedLightServices.hashCode()
//        return result
//    }
}

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
    var on: Boolean = false,
    var brightness : Int = MIN_BRIGHTNESS,
    val lights: List<PhilipsHueLightInfo>,
    /** References to group of lights in this room.  Even though it's an array, there should be just 1. */
    val groupedLightServices: List<PHv2ItemInArray>
)

/**
 * Holds info about a single philips hue light
 */
data class PhilipsHueLightInfo(
    /** the id is also a number to be used to access it directly as a LIGHT */
    val lightId: String,
    /** id for this light DEVICE--remember, a light is a child of a device (so this could also be called the parent). Aka owner rid */
    val deviceId: String,
    /** human-readable name for this light (find in the metadata of PHv2Device), the parent of the light. */
    val name: String = "",
    var state: PhilipsHueLightState = PhilipsHueLightState(),
    val type: String = "",
    val modelid: String = "",
    val swversion: String = "",
) {
    override fun equals(other: Any?): Boolean {
        val t = other as PhilipsHueLightInfo
        if (t.lightId != lightId) {
            Log.d(TAG, "lightInfo.equals() -> false: lightId")
            return false
        }
        if (t.deviceId != deviceId) {
            Log.d(TAG, "lightInfo.equals() -> false: deviceId")
            return false
        }
        if (t.name != name) {
            Log.d(TAG, "lightInfo.equals() -> false: name")
            return false
        }
        if (t.state != state) {
            Log.d(TAG, "lightInfo.equals() -> false: state")
            return false
        }
        if (t.type != type) {
            Log.d(TAG, "lightInfo.equals() -> false: type")
            return false
        }
        if (t.modelid != modelid) {
            Log.d(TAG, "lightInfo.equals() -> false: modelid")
            return false
        }
        if (t.swversion != swversion) {
            Log.d(TAG, "lightInfo.equals() -> false: swversion")
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = lightId.hashCode()
        result = 31 * result + deviceId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + modelid.hashCode()
        result = 31 * result + swversion.hashCode()
        return result
    }
}

/**
 * Defines the state of a light.  Yup, pretty complicated (these lights
 * do a lot!).
 */
data class PhilipsHueLightState(
    /** Simply tells if the light is currently on. */
    var on: Boolean = false,
    /** Range [0..100] */
    var bri: Int = 100,
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

private const val TAG = "PhilipsHueDataTypes"

/** the maximum light that a light can put out */
const val MAX_BRIGHTNESS = 100

/** min light that a light can emit */
const val MIN_BRIGHTNESS = 0