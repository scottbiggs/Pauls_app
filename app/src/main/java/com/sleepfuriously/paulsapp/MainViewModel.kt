package com.sleepfuriously.paulsapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Viewmodel for the main portion of the opening screen.
 * This should only incorporate the broad aspects of the
 * main screen.  The parts of the screen should have their
 * own viewmodels.
 */
class MainViewModel : ViewModel() {


    /** communicates state of bridge initialization to the ui */
    var bridgeInit by mutableStateOf(PhilipsHueBridgeInit.INITIALIZING)
        private set

    /**
     * todo:  this is for testing the initialization graphic
     */
    init {
        viewModelScope.launch {
            delay(3000)
            bridgeInit = PhilipsHueBridgeInit.INITIALIZED
        }
    }

}

/**
 * The states of the Philips Hue bridge.
 */
enum class PhilipsHueBridgeInit {
    /** currently attempting to initialize the bridge */
    INITIALIZING,
    /** attempt to initialize the bridge has timed out */
    INITIALIZATION_TIMEOUT,
    /** successfully initialized */
    INITIALIZED,
    /** some error has occurred when dealing with the bridge */
    ERROR
}