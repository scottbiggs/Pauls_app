package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ZONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This class handles communication between a Flock and the rest of the app.
 *
 * Flock data flows are produced here.
 *
 * CONTRACT
 * This class also wants to observe server-sent events from all the bridges
 * working with this app.  Those events will also be translated and passed
 * along as flows of an updated Flock (ahem, are you listening,
 * [PhilipsHueRepository]?).  Those updates should call [updateOnOffFromBridge],
 * [updateBrightnessFromBridges], and [updateAllFromBridges].
 *
 * @param   coroutineScope      needed to launch coroutines
 *
 * @param   startLightsAndBridges   A set of [PhilipsHueLightSetAndBridge] for this flock.
 *                              Used to figure out all the lights and bridges.
 *
 * @param   humanName           Human readable name for this flock
 *
 * @param   currentSceneName    The scene that is currently displayed. Defaults to
 *                              empty string (none).
 *
 * @param   brightness          The brightness 0..100 for this flock currently.
 *
 * @param   onOff               Tells if this flock is on or off.
 */
class PhilipsHueFlockModel(
    coroutineScope: CoroutineScope,
    startLightsAndBridges: Set<PhilipsHueLightSetAndBridge>,
    humanName: String,
    currentSceneName: String = "",
    brightness: Int,
    onOff: Boolean,
) {

    //-------------------------------------
    //  flows
    //-------------------------------------

    private lateinit var _flock: MutableStateFlow<PhilipsHueFlock>
    /** Flow of the Flock. Observers will be notified of changes */
    lateinit var flock: StateFlow<PhilipsHueFlock>


    //-------------------------------------
    //  private data
    //-------------------------------------

    //-------------------------------------
    //  init
    //-------------------------------------

    init {

        //
        // Initialization consists of:
        //  1. Create a Set of all the bridges for this flock
        //  2. Create a Set of LightGroups, which are essentially
        //      all the lights and their bridges used by this flock.
        //      A. This means checking to see if the bridge already knows
        //          about this light group(s).
        //      B. If it does not know, tell the bridge about the light
        //          group.  Do this for each bridge.
        //  3. Create our new Flock from this info.
        //  4. Update the Flock so that the state flow observers can see.
        //

        coroutineScope.launch(Dispatchers.IO) {

            // find the bridges
            val bridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
            val lightSet = mutableSetOf<PhilipsHueLightInfo>()
            val lightGroupSet = mutableSetOf<PhilipsHueLightGroup>()

            startLightsAndBridges.forEach { lightsAndBridge ->
                // add to the set of bridges
                bridgeSet.add(lightsAndBridge.bridgeInfo)

                // add the lights
                lightSet.addAll(lightsAndBridge.lightSet)

                // While we're at it, do the light group too.  We have to first
                // check to see if this light group already exists.  If it doesn't,
                // create it.
                if (lightsAndBridge.v2GroupId.isEmpty()) {
                    // v2Id is unknown.  We're going to have to make a new group
                    val lightGroupId = sendCreateLightGroupToBridge(
                        lightSet = lightsAndBridge.lightSet,
                        bridgeIpAddress = lightsAndBridge.bridgeInfo.ipAddress,
                        bridgeToken = lightsAndBridge.bridgeInfo.token
                    )
                    if (lightGroupId == null) {
                        Log.e(TAG, "init()  Unable to create new Light Group for bridge ${lightsAndBridge.bridgeInfo.humanName}! Aborting.")
                        return@launch
                    }
                    val newLightGroup = PhilipsHueLightGroup(
                        groupId = lightGroupId,
                        bridgeId = lightsAndBridge.bridgeInfo.bridgeId,
                        lightIds = lightsAndBridge.lightSet.map { it.lightId }.toSet()
                    )
                    lightGroupSet.add(newLightGroup)
                }
                else {
                    // group should be known already
                    val newLightGroup = PhilipsHueLightGroup(
                        groupId = lightsAndBridge.v2GroupId,
                        bridgeId = lightsAndBridge.bridgeInfo.bridgeId,
                        lightIds = lightsAndBridge.lightSet.map { it.lightId }.toSet()
                    )
                    lightGroupSet.add(newLightGroup)
                }
            }

            val tmpFlock = PhilipsHueFlock(
                name = humanName,
                currentSceneName = currentSceneName,
                brightness = brightness,
                onOffState = onOff,
                bridgeSet = bridgeSet,
                lightSet = lightSet,
                lightGroups = lightGroupSet
            )

            _flock.update { refreshOnOffAndBrightness(tmpFlock) }

            flock = _flock.asStateFlow()
        }
    }

    //-------------------------------------
    //  functions
    //-------------------------------------

    /**
     * Flocks deal with groups of lights.  For efficiency we send messages to
     * bridges in groups, which have to first be defined by the bridges.
     * This function asks the bridges to create ONE group for one bridge.
     *
     * @return  A light group complete with v2Id that the bridge can recognize.
     *          Null if there was a problem.
     */
    suspend fun sendCreateLightGroupToBridge(
        lightSet: Set<PhilipsHueLightInfo>,
        bridgeIpAddress: String,
        bridgeToken: String
    ) : String? = withContext(Dispatchers.IO) {
        PhilipsHueBridgeApi.createLightGroup(
            bridgeIpStr = bridgeIpAddress,
            token = bridgeToken,
            lights = lightSet,
            name = flock.value.name,        // todo: is this right?
            archetype = RTYPE_ZONE,
        )
    }


    /**
     * Causes this Flock to turns on or off all the lights.
     * This is done by sending on/off signals to bridges for all the
     * relevant lights.
     *
     * Note:    This will send messages to the APIs of the bridges which will
     *          do the work and then send an sse back. THEN we will have
     *          results and the settings will be changed.
     *
     * @param   newOnOff    True -> turn all lights on (restoring previous settings).
     *                      False -> turn all lights off
     */
    suspend fun sendOnOffToBridges(newOnOff: Boolean) {

        // Note that the lights are controlled by light groups.  Each bridge
        // should have one light group, and each light group should have just
        // one bridge.  Go through the light groups and tell its bridge to
        // turn that light group off.
        for(lightGroup in flock.value.lightGroups) {
            // find the bridge for this light group
            val bridgeId = lightGroup.bridgeId
            val bridge = flock.value.bridgeSet.find { it.bridgeId == bridgeId }
            if (bridge == null) {
                Log.e(TAG, "Unable to find bridge in sendOnOffToBridges()!  Aborting!")
                return
            }

            PhilipsHueBridgeApi.sendGroupedLightsOnOffToBridge(
                newOnOff = newOnOff,
                lightGroup = lightGroup,
                bridgeIp = bridge.ipAddress,
                bridgeToken = bridge.token
            )
        }
    }

    /**
     * Adjusts the brightness of all the lights in the flock. Works similarly
     * to [sendOnOffToBridges].
     *
     * @param   newBrightness       The new brightness for all the lights [0..100].
     */
    suspend fun sendBrightnessToBridges(newBrightness: Int) {
        for (lightGroup in flock.value.lightGroups) {
            val bridgeId = lightGroup.bridgeId
            val bridge = flock.value.bridgeSet.find { it.bridgeId == bridgeId }
            if (bridge == null) {
                Log.e(TAG, "Unable to find bridge in sendBrightnessToBridges()!  Aborting!")
                return
            }

            PhilipsHueBridgeApi.sendGroupedLightBrightnessToBridge(
                newBrightness = newBrightness,
                lightGroup = lightGroup,
                bridgeIp = bridge.ipAddress,
                bridgeToken = bridge.token
            )
        }
    }


    /**
     * Receives the result of an sse that at least one light should have
     * its on/off status changed.  This function goes through the lights
     * and turns the relevant ones on/off.
     *
     * @param   changedLightList    List of lights with new on/off status.
     */
    fun updateOnOffFromBridge(changedLightList: Set<PhilipsHueLightInfo>) {

        val newLightSet = mutableSetOf<PhilipsHueLightInfo>()
        _flock.update { workFlock ->

            // Create a new light list.  Lights that do not appear in the changed
            // list remain unchanged.  Those that do appear need to have their
            // on/off state updated.
            workFlock.lightSet.forEach { localLight ->
                val matchingChangedLight = changedLightList.find { it.lightId == localLight.lightId }
                if (matchingChangedLight != null) {
                    // yup, found a match. Note the change
                    val newState = localLight.state.copy(on = matchingChangedLight.state.on)
                    val newLight = localLight.copy(state = newState)
                    newLightSet.add(newLight)
                }
                else {
                    // not found, so add the light as it is
                    newLightSet.add(localLight)
                }
            }

            // update and refresh
            workFlock.copy(lightSet = newLightSet)
            refreshOnOff(workFlock)
        }
    }

    /**
     * Similar to [updateBrightnessFromBridges], this handles changes about the
     * brightness of lights sent via sse.
     */
    fun updateBrightnessFromBridges(changedLightSet: Set<PhilipsHueLightInfo>) {
        val newLightSet = mutableSetOf<PhilipsHueLightInfo>()
        _flock.update { tmpFlock ->
            // Loop through local lights.  If one matches any of the changed
            // lights, make note the change and add that to our rebuilt Set.
            tmpFlock.lightSet.forEach { localLight ->
                val matchingChangedLight = changedLightSet.find { it.lightId == localLight.lightId }
                if (matchingChangedLight != null) {
                    // match!
                    val newState = localLight.state.copy(bri = matchingChangedLight.state.bri)
                    val newLocalLight = localLight.copy(state = newState)
                    newLightSet.add(newLocalLight)
                }
                else {
                    // no match found, so simply add our original
                    newLightSet.add(localLight)
                }
            }
            // finally update
            tmpFlock.copy(lightSet = newLightSet)
        }
    }

    /**
     * Call this whenever a change is noted to any bridge.  The Flock will
     * sift through this data and use whatever it needs to update
     * (ignoring parts that are not relevant).
     *
     * @param   lightSet    A list of lights that have reported changes.
     *
     * side effects
     *  - potentially everything, but could be nothing
     */
    fun updateAllFromBridges(lightSet: Set<PhilipsHueLightInfo>) {

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
            refreshOnOffAndBrightness(it)
        }
        Log.d(TAG, "updateSse() finished after updating")
    }


    //-------------------------------------
    //  private functions
    //-------------------------------------

//    /**
//     * Re-calculates the current brightness based on all the lights, rooms
//     * and zones.  Please do this whenever there is any kind of change to
//     * a [PhilipsHueFlock].
//     *
//     * Kind of complicated and may need tweaking.
//     *
//     * side effects
//     *  - [flock] flow will be updated according to the new brightness.
//     */
//    @Deprecated("use refreshSpecificBrightness")
//    fun refreshBrightness() {
//        Log.d(TAG, "refreshBrightness() begin")
//        var brightnessSum = 0
//        var lightCount = 0
//
//        // for now, it's just the average of all the lights
//        flock.value.lightSet.forEach { light ->
//            lightCount++
//            brightnessSum += light.state.bri
//        }
//
//        val newBrightness = brightnessSum / lightCount
//
//        // only change if different
//        if (newBrightness != flock.value.brightness) {
//            Log.d(TAG, "refreshBrightness() updating to $newBrightness")
//            _flock.update { it.copy(brightness = newBrightness) }
//        }
//    }

    /**
     * Recalculates the brightness of the given flock based on all the lights'
     * current brightness.  The general brightness is simply the average.
     *
     * @return  A copy of the given flock with brightness refreshed.
     */
    fun refreshBrightness(flock: PhilipsHueFlock) : PhilipsHueFlock {
        var brightnessSum = 0
        var lightCount = 0

        // for now, it's just the average of all the lights
        flock.lightSet.forEach { light ->
            lightCount++
            brightnessSum += light.state.bri
        }

        val newBrightness = brightnessSum / lightCount
        return flock.copy(brightness = newBrightness)
    }

//    /**
//     * The on/off state is actually pretty simple.  If there exists a light
//     * that is on, then the Flock is on.
//     *
//     * side effects
//     *  - flow updated to reflect on/off condition
//     */
//    fun refreshOnOff() {
//        Log.d(TAG, "refreshOnOff() begin")
//        var anyLightOn = false
//
//        for (light in flock.value.lightSet) {
//            if (light.state.on) {
//                anyLightOn = true
//                break
//            }
//        }
//
//        // check to see if we actually need to bother with changing the flow
//        if (anyLightOn != flock.value.onOffState) {
//            Log.d(TAG, "refreshOnOff() updating to $anyLightOn")
//            _flock.update { it.copy(onOffState = anyLightOn) }
//        }
//    }

    /**
     * The on/off state is actually pretty simple.  If there exists a light
     * that is on, then the Flock is on.
     *
     * @return  A flock just like the input but with the on/off state correct.
     */
    fun refreshOnOff(flock: PhilipsHueFlock) : PhilipsHueFlock {

        var anyLightOn = false
        for (light in flock.lightSet) {
            if (light.state.on) {
                anyLightOn = true
                break
            }
        }
        return flock.copy(onOffState = anyLightOn)
    }

//    /**
//     * For efficiency use this if both brightness and on/off have changed.
//     * See [refreshBrightness] and [refreshOnOff].
//     */
//    fun refreshOnOffAndBrightness() {
//        Log.d(TAG, "refreshOnOffAndBrightness() begin")
//        var anyLightOn = false
//        for (light in flock.value.lightSet) {
//            if (light.state.on) {
//                anyLightOn = true
//                break
//            }
//        }
//
//        var brightnessSum = 0
//        var lightCount = 0
//        // for now, it's just the average of all the lights
//        flock.value.lightSet.forEach { light ->
//            lightCount++
//            brightnessSum += light.state.bri
//        }
//
//        val newBrightness = brightnessSum / lightCount
//
//        if ((anyLightOn != flock.value.onOffState) || (newBrightness != flock.value.brightness)) {
//            _flock.update { it.copy(onOffState = anyLightOn, brightness = newBrightness) }
//        }
//    }

    /**
     * For convenience, call this to refresh both the brightness and on/off
     * state of a [PhilipsHueFlock].
     */
    fun refreshOnOffAndBrightness(flock: PhilipsHueFlock)
            : PhilipsHueFlock {

        val flockWithNewBrightness = refreshBrightness(flock)
        return refreshOnOff(flockWithNewBrightness)
    }


    /**
     * Searches through our data of lights and bridges to see if we have a
     * match for the given v2Id for a light.
     *
     * @return  A Pair of light info and bridge info that matches.
     *          Null if not found.
     */
    private fun getMatchingLightBridgePair(lightId: String) : Pair<PhilipsHueLightInfo, PhilipsHueBridgeInfo>? {
        for(lightGroup in flock.value.lightGroups) {
            val foundLightId = lightGroup.lightIds.find { id -> id == lightId }
            if (foundLightId != null) {
                // found it!
                val lightInfo = getLightFromId(foundLightId)
                if (lightInfo == null) {
                    Log.e(TAG, "getMatchingLight() BAD ERROR: could not get lightInfo!!! Aborting!")
                    return null
                }
                val bridgeInfo = getBridgeFromId(lightGroup.bridgeId)
                if (bridgeInfo == null) {
                    Log.e(TAG, "getMatchingLight() BAD ERROR: could not find the bridgeInfo!!! Aborting!")
                    return null
                }
                return Pair(
                    lightInfo,
                    bridgeInfo
                )
            }
        }
        return null
    }

    /**
     * Finds the [PhilipsHueLightInfo] in the flock with the given Id.
     * Returns NULL if not found.
     */
    private fun getLightFromId(lightId: String) : PhilipsHueLightInfo? {
        return flock.value.lightSet.find { it.lightId == lightId }
    }

    /**
     * Finds the [PhilipsHueBridgeInfo] in this flock with the given Id.
     * Returns NULL if not found.
     */
    private fun getBridgeFromId(bridgeId: String) : PhilipsHueBridgeInfo? {
        return flock.value.bridgeSet.find { it.bridgeId == bridgeId }
    }

}


/**
 * A convenient way to associate some lights with a bridge.
 *
 * @param   v2GroupId   If known, please supply the v2Id for this group.
 *                      Empty String indicates that this is a new group.
 *
 * @param   lightSet    All the lights for this light group.
 *
 * @param   bridgeInfo  The bridge in charge of this group of lights.
 */
data class PhilipsHueLightSetAndBridge(
    val v2GroupId: String = "",
    val lightSet: Set<PhilipsHueLightInfo>,
    val bridgeInfo: PhilipsHueBridgeInfo
)


private const val TAG = "PhilipsHueFlockModel"