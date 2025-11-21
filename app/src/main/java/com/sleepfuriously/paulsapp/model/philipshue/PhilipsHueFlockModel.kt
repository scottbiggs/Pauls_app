package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.RTYPE_ZONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
 * @param   coroutineScope      Needed to do some network stuff.
 *
 * @param   bridgeSet           Set of all bridges used by this flock.
 *
 * @param   roomSet             All the rooms controlled by this flock.
 *
 * @param   zoneSet             The Zones controlled by this flock.
 *
 * @param   humanName           Human readable name for this flock
 *
 * @param   currentSceneName    The scene that is currently displayed. Defaults to
 *                              empty string (none).
 *
 * @param   repository          A [PhilipsHueRepository] that controls the
 *                              rooms and zones this flock uses.
 */
class PhilipsHueFlockModel(
    private val coroutineScope: CoroutineScope,
    bridgeSet: Set<PhilipsHueBridgeInfo>,
    roomSet: Set<PhilipsHueRoomInfo>,
    zoneSet: Set<PhilipsHueZoneInfo>,
    humanName: String,
    id: String = generateV2Id(),
    currentSceneName: String = "",
    /** The repository that holds rooms and zones */
    private val repository: PhilipsHueRepository
) {

    //-------------------------------------
    //  flows
    //-------------------------------------

    // make a new flock from supplied data
    private val _flock = MutableStateFlow(
        PhilipsHueFlockInfo(
            name = humanName,
            currentSceneName = currentSceneName,
            brightness = getBrightness(roomSet, zoneSet),
            onOffState = getOnOff(roomSet, zoneSet),
            bridgeSet = bridgeSet,
            roomSet = roomSet,
            zoneSet = zoneSet,
            id = id
        )
    )
    /** Flow of the Flock. Observers will be notified of changes */
    val flock = _flock.asStateFlow()

    //-------------------------------------
    //  private data
    //-------------------------------------

    //-------------------------------------
    //  init
    //-------------------------------------

    init {
        // create the new flock from the current
    }

    //-------------------------------------
    //  functions
    //-------------------------------------

    /**
     * Causes this Flock to turns on or off all the lights.
     * This is done by sending on/off signals to all the rooms and
     * zone in the flock.
     *
     * Note:    This will send messages to the APIs of the bridges which will
     *          do the work and then send an sse back. THEN we will have
     *          results and the settings will be changed.
     *
     * @param   newOnOff    True -> turn all lights on (restoring previous settings).
     *                      False -> turn all lights off
     */
    fun sendOnOffToBridges(newOnOff: Boolean) {
        flock.value.roomSet.forEach { roomInfo ->
            repository.sendRoomOnOffToBridge(roomInfo, newOnOff)
        }
        flock.value.zoneSet.forEach { zoneInfo ->
            repository.sendZoneOnOffToBridge(zoneInfo, newOnOff)
        }
    }

    /**
     * Adjusts the brightness of all the lights in the flock. Works similarly
     * to [sendOnOffToBridges].
     *
     * @param   newBrightness       The new brightness for all the lights [0..100].
     */
    fun sendBrightnessToBridges(newBrightness: Int) {
        flock.value.roomSet.forEach { roomInfo ->
            repository.sendRoomBrightnessToBridge(roomInfo, newBrightness)
        }
        flock.value.zoneSet.forEach { zoneInfo ->
            repository.sendZoneBrightnessToBridge(zoneInfo, newBrightness)
        }
    }

    /**
     * Tell the flock to do its best to display the given scene.
     * Right now, we just go through the bridges and check to see if
     * any rooms or zones implement the scene.  If they do, turn those
     * on.
     *
     * @return  Returns FALSE on error.
     */
    suspend fun sendSceneToBridge(scene: PHv2Scene) : Boolean {
        when (scene.group.rtype) {
            RTYPE_ROOM -> {
                // Is there a room controlled by this scene? todo: i think matching by scene NAME would be better!
                val sceneRooms = flock.value.roomSet.filter { it.v2Id == scene.group.rid }
                if (sceneRooms.isEmpty()) {
                    Log.v(TAG, "sendSceneToBridge() - no matching rooms. Nothing to do.")
                    return false
                }

                // send the scene for each room
                sceneRooms.forEach { room ->
                    // find the bridge for this room.
                    val bridge = flock.value.getBridgeForRoom(room.v2Id)
                    if (bridge == null) {
                        Log.e(TAG, "sendSceneToBridge() - weird error! Can't find the bridge for room ${room.name}. aborting!")
                        return false
                    }

                    PhilipsHueApi.sendSceneToLightGroup(
                        bridgeIp = bridge.ipAddress,
                        bridgeToken = bridge.token,
                        sceneToDisplay = scene
                    )
                }
            }

            RTYPE_ZONE -> {
                // find the zone controlled by this scene todo: scene name???
                val zones = flock.value.zoneSet.filter { it.v2Id == scene.group.rid }
                if (zones.isEmpty()) {
                    Log.v(TAG, "sendSceneToBridge() - no matching zones. Nothing to do.")
                    return false
                }

                zones.forEach { zone ->
                    // find the bridge for this zone
                    val bridge = flock.value.getBridgeForZone(zone.v2Id)
                    if (bridge == null) {
                        Log.e(TAG, "sendSceneToBridge() - weird error! Can't find the bridge for zone ${zone.name}. aborting!")
                        return false
                    }
                    PhilipsHueApi.sendSceneToLightGroup(
                        bridgeIp = bridge.ipAddress,
                        bridgeToken = bridge.token,
                        sceneToDisplay = scene
                    )
                }
            }

            else -> {
                Log.e(TAG, "sendSceneToBridge() - Scene does apply to room or zone. Nothing to do")
                Log.e(TAG, "   scene.group.rtype = ${scene.group.rtype}")
                return false
            }
        }

        return true
    }

    /**
     * Receives the result of an sse that at least one room or zone has
     * changed its on/off status.  This function goes through the areas
     * and turns the relevant ones on/off.  Then we determine if the
     * whole flock should be considered on or off.
     *
     * @param   changedRoomSet      The rooms that have changed their on/off
     *                              status.  These rooms don't have to be
     *                              part of this Flock--we'll figure out the
     *                              relevant ones.
     *
     * @param   changedZoneSet      Zones that changed.  Similar to [changedRoomSet].
     *
     * side effects
     *  - potentially each room and zone, but could be nothing
     */
    fun updateOnOffFromBridge(
        changedRoomSet: Set<PhilipsHueRoomInfo>,
        changedZoneSet: Set<PhilipsHueZoneInfo>
    ) {

        val newRoomSet = mutableSetOf<PhilipsHueRoomInfo>()
        val newZoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        _flock.update { workFlock ->

            // Go through our rooms. If one matches the changed room set,
            // modify it.
            workFlock.roomSet.forEach { roomInfo ->
                val foundChangedRoom = changedRoomSet.find { it.v2Id == roomInfo.v2Id }
                newRoomSet.add(foundChangedRoom ?: roomInfo)
            }

            // similar for zones
            workFlock.zoneSet.forEach { zoneInfo ->
                val foundChangedZone = changedZoneSet.find { it.v2Id == zoneInfo.v2Id }
                newZoneSet.add(foundChangedZone ?: zoneInfo)
            }

            // update and refresh
            val retVal = refreshOnOff(workFlock.copy(roomSet = newRoomSet, zoneSet = newZoneSet))
            Log.d(TAG, "updateOnOffFromBridge()  retVal.id = ${retVal.id}")
            retVal
        }
    }

    /**
     * Similar to [updateOnOffFromBridge], this handles changes about the
     * brightness of lights sent via sse.
     */
    fun updateBrightnessFromBridges(
        changedRoomSet: Set<PhilipsHueRoomInfo>,
        changedZoneSet: Set<PhilipsHueZoneInfo>
    ) {
        val newRoomSet = mutableSetOf<PhilipsHueRoomInfo>()
        val newZoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        _flock.update { tmpFlock ->
            tmpFlock.roomSet.forEach { roomInfo ->
                val foundChangedRoom = changedRoomSet.find { it.v2Id == roomInfo.v2Id }
                newRoomSet.add(foundChangedRoom ?: roomInfo)
            }
            tmpFlock.zoneSet.forEach { zoneInfo ->
                val foundChangedZone = changedZoneSet.find { it.v2Id == zoneInfo.v2Id }
                newZoneSet.add(foundChangedZone ?: zoneInfo)
            }

            refreshBrightness(tmpFlock.copy(roomSet = newRoomSet, zoneSet = newZoneSet))
        }
    }

    /**
     * Finds the brightness (the average) of all the lights in the given
     * rooms and zones.
     *
     * @return  0 if there are no rooms or zones.
     */
    fun getBrightness(
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>
    ) : Int {

        if (roomSet.isEmpty() && zoneSet.isEmpty()) { return 0 }

        var brightness = 0.0
        var lightCounter = 0
        roomSet.forEach { room ->
            brightness += room.brightness
            lightCounter++
        }
        zoneSet.forEach { zone ->
            brightness += zone.brightness
            lightCounter++
        }

        return (brightness / lightCounter).toInt()
    }

    /**
     * Finds the on/off status with the given set of rooms and zones.
     *
     * @return  True if *any* light is on.  False if ALL the lights are off.
     */
    fun getOnOff(
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>
    ) : Boolean {
        roomSet.forEach { room ->
            if (room.on) { return true }
        }
        zoneSet.forEach { zone ->
            if (zone.on) { return true }
        }
        return false
    }


    /**
     * Call this whenever a change is noted to any room or zone.
     * The Flock will sift through this data and use whatever it needs to
     * update (ignoring parts that are not relevant).
     *
     * @param   changedRoomSet      The rooms that have changed their on/off
     *                              status.  These rooms don't have to be
     *                              part of this Flock--we'll figure out the
     *                              relevant ones.
     *
     * @param   changedZoneSet      Zones that changed.  Similar to [changedRoomSet].
     *
     * side effects
     *  - potentially everything, but could be nothing
     */
    fun updateAllFromBridges(
        changedRoomSet: Set<PhilipsHueRoomInfo>,
        changedZoneSet: Set<PhilipsHueZoneInfo>
    ) {
        val newRoomSet = mutableSetOf<PhilipsHueRoomInfo>()
        val newZoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        _flock.update { workFlock ->
            Log.d(TAG, "updateAllFromBridges() - workFlock id = ${workFlock.id}")
            workFlock.roomSet.forEach { roomInfo ->
                val foundChangedRoom = changedRoomSet.find { it.v2Id == roomInfo.v2Id }
                newRoomSet.add(foundChangedRoom ?: roomInfo)
            }
            workFlock.zoneSet.forEach { zoneInfo ->
                val foundChangedZone = changedZoneSet.find { it.v2Id == zoneInfo.v2Id }
                newZoneSet.add(foundChangedZone ?: zoneInfo)
            }

            val returnVal = refreshOnOffAndBrightness(workFlock.copy(roomSet = newRoomSet, zoneSet = newZoneSet))
            Log.d(TAG, "updateAllFromBridge()  returnVal id = ${returnVal.id}")
            returnVal
        }
    }

    /**
     * Whenever a flock has its scene set, call this function.  It will modify
     * the flock so that it correctly reflects the new scene.  You can also call
     * it to indicate that NO scene is being displayed (like the user has made
     * changes, etc.).
     *
     * This is done here instead of through an sse because server-sent events
     * don't really identify anything that was changed--just the lights (as far
     * as I know, which is probably wrong).
     *
     * Similar to [PhilipsHueBridgeModel.updateRoomCurrentScene].
     *
     * @param   scene       The details of the scene that is operating on the room.
     */
    fun updateFlockCurrentScene(scene: PHv2Scene) {
        Log.d(TAG, "updateFlockCurrentScene() begin")
        Log.d(TAG, "    flock.name = ${flock.value.name}")
        Log.d(TAG, "    flock.currentSceneName = ${flock.value.currentSceneName}")
        Log.d(TAG, "    scene = ${scene.metadata.name}")

        // update the flock with the new name.
        _flock.update {
            it.copy(currentSceneName = scene.metadata.name)
        }
    }

    //-------------------------------------
    //  private functions
    //-------------------------------------

    /**
     * Recalculates the brightness of the given flock based on all the lights'
     * current brightness.  The general brightness is simply the average.
     *
     * @return  A copy of the given flock with brightness refreshed.
     */
    fun refreshBrightness(flock: PhilipsHueFlockInfo) : PhilipsHueFlockInfo {
        return flock.copy(brightness = flock.calculateBrightness())
    }

    /**
     * The on/off state is actually pretty simple.  If there exists a light
     * that is on, then the Flock is on.
     *
     * @return  A flock just like the input but with the on/off state correct.
     */
    fun refreshOnOff(flock: PhilipsHueFlockInfo) : PhilipsHueFlockInfo {
        return flock.copy(onOffState = flock.calculateOnOff())
    }

    /**
     * For convenience, call this to refresh both the brightness and on/off
     * state of a [PhilipsHueFlockInfo].
     *
     * @return  A [PhilipsHueFlockInfo] with the newly refreshed everything.
     */
    fun refreshOnOffAndBrightness(flock: PhilipsHueFlockInfo)
            : PhilipsHueFlockInfo {

        val flockWithNewBrightness = refreshBrightness(flock)
        return refreshOnOff(flockWithNewBrightness)
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