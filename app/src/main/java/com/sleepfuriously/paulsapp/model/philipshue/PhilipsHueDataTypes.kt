package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ItemInArray
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene

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
    /** all the scenes for this bridge */
    var scenes: List<PHv2Scene> = mutableListOf()
) {
    /**
     * This should cause the flow to update correctly.
     */
    override fun equals(other: Any?): Boolean {
        val otherBridge = other as PhilipsHueBridgeInfo
        if (id != otherBridge.id) {
            return false
        }
        if (labelName != otherBridge.labelName) {
            return false
        }
        if (ip != otherBridge.ip) {
            return false
        }
        if (token != otherBridge.token) {
            return false
        }
        if (active != otherBridge.active) {
            return false
        }
        if (connected != otherBridge.connected) {
            return false
        }
        if (rooms.size != otherBridge.rooms.size) {
            return false
        }
        rooms.forEach { room ->
            if (otherBridge.rooms.contains(room) == false) {
                return false
            }
        }
        if (scenes.size != otherBridge.scenes.size) {
            return false
        }
        scenes.forEachIndexed { i, scene ->
            if (otherBridge.scenes[i] != scene) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + labelName.hashCode()
        result = 31 * result + ip.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + active.hashCode()
        result = 31 * result + connected.hashCode()
        result = 31 * result + rooms.hashCode()
        return result
    }

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
    val lights: MutableSet<PhilipsHueLightInfo>,
    /** References to group of lights in this room.  Even though it's an array, there should be just 1. */
    val groupedLightServices: List<PHv2ItemInArray>
) {
    override fun equals(other: Any?): Boolean {
        val otherRoom = other as PhilipsHueRoomInfo
        if (otherRoom.id != id) {
            return false
        }
        if (otherRoom.name != name) {
            return false
        }
        if (otherRoom.on != on) {
            return false
        }
        if (otherRoom.brightness != brightness) {
            return false
        }
        if (otherRoom.lights.size != lights.size) {
            return false
        }
        otherRoom.lights.forEach { otherLight ->
            if (lights.contains(otherLight) == false) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + on.hashCode()
        result = 31 * result + brightness
        result = 31 * result + lights.hashCode()
        result = 31 * result + groupedLightServices.hashCode()
        return result
    }

}

/**
 * Holds info about a single philips hue light
 */
data class PhilipsHueLightInfo(
    /** the id is also a number to be used to access it directly as a LIGHT */
    val lightId: String,
    /** id for this light DEVICE--remember, a light is a child of a device (so this could also be called the parent). */
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
            return false
        }
        if (t.deviceId != deviceId) {
            return false
        }
        if (t.name != name) {
            return false
        }
        if (t.state != state) {
            return false
        }
        if (t.type != type) {
            return false
        }
        if (t.modelid != modelid) {
            return false
        }
        if (t.swversion != swversion) {
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