package com.sleepfuriously.paulsapp.model.philipshue.data

import android.util.Log

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
    val state: PhilipsHueLightState = PhilipsHueLightState(),
    val type: String = "",
    val modelid: String = "",
    val swversion: String = "",
    /** IP of the bridge that controls this light (used to differentiate between bridges) */
    val bridgeIpAddress: String
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


private const val TAG = "PhilipsHueLightInfo"