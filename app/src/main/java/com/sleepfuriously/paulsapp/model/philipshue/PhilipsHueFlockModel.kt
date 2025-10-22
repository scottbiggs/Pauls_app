package com.sleepfuriously.paulsapp.model.philipshue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        TODO()
    }

    /**
     * Adjusts the brightness of all the lights in the flock.
     *
     * @param   newBrightness       The new brightness for all the lights [0..100].
     */
    fun changeBrightness(newBrightness: Int) {
        TODO()
    }


    //-------------------------------------
    //  private functions
    //-------------------------------------

}

//private val defaultFlock = PhilipsHueFlock(
//    id = "",
//    name = "default--don't use me",
//    roomBridgeMap = mapOf(),
//    zoneBridgeMap = mapOf(),
//    onOffState = false,
//    brightness = 0,
//
//)