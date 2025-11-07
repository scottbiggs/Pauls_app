package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
 * @param   brightness          The brightness 0..100 for this flock currently.
 *
 * @param   onOff               Tells if this flock is on or off.
 *
 * @param   repository          A [PhilipsHueRepository] that controls the
 *                              rooms and zones this flock uses.
 */
class PhilipsHueFlockModel(
    bridgeSet: Set<PhilipsHueBridgeInfo>,
    roomSet: Set<PhilipsHueRoomInfo>,
    zoneSet: Set<PhilipsHueZoneInfo>,
    humanName: String,
    currentSceneName: String = "",
    brightness: Int,
    onOff: Boolean,
    /** The repository that holds rooms and zones */
    private val repository: PhilipsHueRepository
) {

    //-------------------------------------
    //  flows
    //-------------------------------------

    private val _flock = MutableStateFlow<PhilipsHueFlock>(
        PhilipsHueFlock(
            name = humanName,
            currentSceneName = currentSceneName,
            brightness = brightness,
            onOffState = onOff,
            bridgeSet = bridgeSet,
            roomSet = roomSet,
            zoneSet = zoneSet
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
            refreshOnOff(workFlock.copy(roomSet = newRoomSet, zoneSet = newZoneSet))
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
            workFlock.roomSet.forEach { roomInfo ->
                val foundChangedRoom = changedRoomSet.find { it.v2Id == roomInfo.v2Id }
                newRoomSet.add(foundChangedRoom ?: roomInfo)
            }
            workFlock.zoneSet.forEach { zoneInfo ->
                val foundChangedZone = changedZoneSet.find { it.v2Id == zoneInfo.v2Id }
                newZoneSet.add(foundChangedZone ?: zoneInfo)
            }

            refreshOnOffAndBrightness(workFlock.copy(roomSet = newRoomSet, zoneSet = newZoneSet))
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
    fun refreshBrightness(flock: PhilipsHueFlock) : PhilipsHueFlock {
        var brightnessSum = 0
        var groupCount = 0

        // for now, it's just the average of all the rooms and zones
        flock.roomSet.forEach { roomInfo ->
            groupCount++
            brightnessSum += roomInfo.brightness
        }

        flock.zoneSet.forEach { zoneInfo ->
            groupCount++
            brightnessSum += zoneInfo.brightness
        }

        val newBrightness = brightnessSum / groupCount
        return flock.copy(brightness = newBrightness)
    }

    /**
     * The on/off state is actually pretty simple.  If there exists a light
     * that is on, then the Flock is on.
     *
     * @return  A flock just like the input but with the on/off state correct.
     */
    fun refreshOnOff(flock: PhilipsHueFlock) : PhilipsHueFlock {

        flock.roomSet.forEach { room ->
            if (room.on) {
                return flock.copy(onOffState = true)
            }
        }

        flock.zoneSet.forEach { zone ->
            if (zone.on) {
                return flock.copy(onOffState = true)
            }
        }
        return flock.copy(onOffState = false)
    }

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