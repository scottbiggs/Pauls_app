package com.sleepfuriously.paulsapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.model.isConnectivityWifiWorking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the startup splash screen.  While this is displaying,
 * many initializations take place.
 *
 * 1. Wifi
 *   a. is wifi turned on for the device?
 *   b. is wifi connected?
 *
 * 2. IoT - Philips Hue
 *   a. do we have a token?
 *   b. does token work?
 *
 * 3. todo - all other IoT devices
 */
class SplashViewModel : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    /** will be true when everything is ready for the splash screen to go away */
    // todo: refactor this (put it within the regular viewmodel)!
    val isReady = _isReady.asStateFlow()

    private val _bridgeState = MutableStateFlow(PhilipsHueBridgeInit.BRIDGE_UNINITIALIZED)
    /** Holds the state of the Philips Hue Bridge   */
    var bridgeState = _bridgeState.asStateFlow()


    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()


    /**
     * Just pauses a bit for the splash screen animation to look nice.
     */
    init {
        viewModelScope.launch {
            delay(2000L)
            _isReady.value = true
        }
    }


    /**
     * Checks to see if wifi is enabled and operating currently.
     * Tested - works!
     *
     * side effect
     *      [wifiWorking]   will be set to true or false accordingly.
     */
    fun isWifiWorking(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _wifiWorking.value = isConnectivityWifiWorking(ctx)
        }
    }

    /**
     * Checks the status of the various Internet of Things devices.
     * Sets the states of the
     *
     * side effects
     *
     */
    fun checkIoT() {

    }

}