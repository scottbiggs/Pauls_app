package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * This class handles communication between a Flock and the rest of the app.
 *
 * Flock data flows are produced here.
 *
 * This class also wants to observe server-sent events from all the bridges
 * working with this app.  Those events will also be translated and passed
 * along as flows of an updated Flock (ahem, are you listening,
 * [PhilipsHueRepository]?).
 */
class PhilipsHueFlockModel(

    /** A Flock to start with.  Defaults to null (which will cause a bare-bones Flock to be created). */
    startFlock: PhilipsHueFlock
) {

    //-------------------------------------
    //  flows
    //-------------------------------------

    private val _flock = MutableStateFlow(startFlock)
    /** Flow of the Flock. Observers will be notified of changes */
    val flock = _flock.asStateFlow()


    //-------------------------------------
    //  private data
    //-------------------------------------

    //-------------------------------------
    //  init
    //-------------------------------------

    init {
        refreshOnOffAndBrightness()
    }

    //-------------------------------------
    //  functions
    //-------------------------------------

    /**
     * Turns on or off all the lights in the flock.
     *
     * Note:    This will send messages to the APIs of the bridges which will
     *          do the work and then send an sse back. THEN we will have
     *          results and the settings will be changed.
     *
     * @param   newOnOff    True -> turn all lights on (restoring previous settings).
     *                      False -> turn all lights off
     */
    fun changeOnOff(newOnOff: Boolean) {
        // create a new light set where all the lights are now off
        // yet the old values are saved
        _flock.update {
            val newLightSet = mutableSetOf<PhilipsHueLightInfo>()
            it.lightSet.forEach { light ->
                val newLightState = light.state.copy(on = newOnOff)
                val newLight = light.copy(state = newLightState)
                newLightSet.add(newLight)
            }
            // by setting the on/off state here, there's no need for
            // calling refreshOnOff().
            it.copy(lightSet = newLightSet, onOffState = newOnOff)
        }
    }

    /**
     * Adjusts the brightness of all the lights in the flock. Works similarly
     * to [changeOnOff].
     *
     * @param   newBrightness       The new brightness for all the lights [0..100].
     */
    fun changeBrightness(newBrightness: Int) {
        // create a new light set, but this time change only the brightness
        // of each light.
        _flock.update {
            val newLightSet = mutableSetOf<PhilipsHueLightInfo>()
            it.lightSet.forEach { light ->
                val newLightState = light.state.copy(bri = newBrightness)
                val newLight = light.copy(state = newLightState)
                newLightSet.add(newLight)
            }
            it.copy(lightSet = newLightSet, brightness = newBrightness)
        }
    }


    /**
     * Call this whenever a change is noted to any bridge.  The Flock will
     * sift through this data and use whatever it needs to update
     * (ignoring parts that are not relevant).
     *
     * @param   lightSet    A list of bridges that have reported changes.
     *
     * side effects
     *  - potentially everything, but could be nothing
     */
//    fun updateBridgesChanged(bridgeList: List<PhilipsHueBridgeInfo>) {
    fun updateBridgesChanged(lightSet: Set<PhilipsHueLightInfo>) {

        Log.d(TAG, "updateBridgesChanged() begin")

        // making a brand new light set
        val newLightSet = mutableSetOf<PhilipsHueLightInfo>()

        // go through our lights one by one to see if any has changed
        flock.value.lightSet.forEach { ourLight ->
            val matchingInputLight = lightSet.find { ourLight.lightId == it.lightId }
            if (matchingInputLight != null) {
                // got a matching light, so make a new light with the changed values
                val newLight = ourLight.copy(state = matchingInputLight.state)
                newLightSet.add(newLight)
            }
            else {
                // the light wasn't one of the ones that changed, so add it
                // as it is
                newLightSet.add(ourLight)
            }
        }
        _flock.update {
            it.copy(lightSet = newLightSet)
        }

        refreshOnOffAndBrightness()
        Log.d(TAG, "updateSse() finished after updating")
    }


    //-------------------------------------
    //  private functions
    //-------------------------------------

    /**
     * Re-calculates the current brightness based on all the lights, rooms
     * and zones.  Please do this whenever there is any kind of change to
     * a [PhilipsHueFlock].
     *
     * Kind of complicated and may need tweaking.
     *
     * side effects
     *  - [flock] flow will be updated according to the new brightness.
     */
    fun refreshBrightness() {
        Log.d(TAG, "refreshBrightness() begin")
        var brightnessSum = 0
        var lightCount = 0

        // for now, it's just the average of all the lights
        flock.value.lightSet.forEach { light ->
            lightCount++
            brightnessSum += light.state.bri
        }

        val newBrightness = brightnessSum / lightCount

        // only change if different
        if (newBrightness != flock.value.brightness) {
            Log.d(TAG, "refreshBrightness() updating to $newBrightness")
            _flock.update { it.copy(brightness = newBrightness) }
        }
    }

    /**
     * The on/off state is actually pretty simple.  If there exists a light
     * that is on, then the Flock is on.
     *
     * side effects
     *  - flow updated to reflect on/off condition
     */
    fun refreshOnOff() {
        Log.d(TAG, "refreshOnOff() begin")
        var anyLightOn = false

        for (light in flock.value.lightSet) {
            if (light.state.on) {
                anyLightOn = true
                break
            }
        }

        // check to see if we actually need to bother with changing the flow
        if (anyLightOn != flock.value.onOffState) {
            Log.d(TAG, "refreshOnOff() updating to $anyLightOn")
            _flock.update { it.copy(onOffState = anyLightOn) }
        }
    }

    /**
     * For efficiency use this if both brightness and on/off have changed.
     * See [refreshBrightness] and [refreshOnOff].
     */
    fun refreshOnOffAndBrightness() {
        Log.d(TAG, "refreshOnOffAndBrightness() begin")
        var anyLightOn = false
        for (light in flock.value.lightSet) {
            if (light.state.on) {
                anyLightOn = true
                break
            }
        }

        var brightnessSum = 0
        var lightCount = 0
        // for now, it's just the average of all the lights
        flock.value.lightSet.forEach { light ->
            lightCount++
            brightnessSum += light.state.bri
        }

        val newBrightness = brightnessSum / lightCount

        if ((anyLightOn != flock.value.onOffState) || (newBrightness != flock.value.brightness)) {
            _flock.update { it.copy(onOffState = anyLightOn, brightness = newBrightness) }
        }
    }


}

private const val TAG = "PhilipsHueFlockModel"