package com.sleepfuriously.paulsapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Viewmodel for the main portion of the opening screen.
 * This should only incorporate the broad aspects of the
 * main screen.  The parts of the screen should have their
 * own viewmodels.
 */
class MainViewmodel : ViewModel() {

    /** Tells the UI which state to display */
    var displayStates by mutableStateOf(MainActivityDisplayStates.UNKNOWN)
        private set


    /**
     * todo:  this is for testing the initialization graphic
     */
//    fun initialize(ctx: Context) {
//        viewModelScope.launch {
//            withContext(Dispatchers.IO) {
//
//                val epref = getESharedPref(ctx) as EncryptedSharedPreferences
//
//                val ph_name = epref.getString(PHILIPS_HUE_NAME_KEY, PH_NAME_NOT_FOUND)
//                if (ph_name.equals(PH_NAME_NOT_FOUND)) {
//                    displayStates = MainActivityDisplayStates.APP_STARTUP
//                    bridgeInit = PhilipsHueBridgeInit.BRIDGE_UNINITIALIZED
//                    Log.d(TAG, "Philips Hue Bridge not initialized")
//
//                    // fixme:  for testing
//                    delay(8000)
//                    displayStates = MainActivityDisplayStates.RUNNING
//                    Log.d(TAG, "displayState = $displayStates")
//                }
//                else {
//                    displayStates = MainActivityDisplayStates.APP_STARTUP
//                    Log.d(TAG, "displayState = $displayStates")
//
//                    // fixme:  for testing
//                    delay(3000)
//                    bridgeInit = PhilipsHueBridgeInit.BRIDGE_INITIALIZED
//
//                    displayStates = MainActivityDisplayStates.RUNNING
//                    Log.d(TAG, "displayState = $displayStates")
//                }
//            }
//        }
//    }


//    /**
//     * Gets a reference to an encrypted shared prefs file.  I presume that
//     * if it doesn't exist, a new one will be created.  Should otherwise
//     * work just like a regular prefs file.
//     *
//     * from:  https://stackoverflow.com/a/64115229/624814
//     */
//    private fun getESharedPref(ctx: Context) : SharedPreferences {
//        val masterKey = MasterKey.Builder(ctx)
//            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
//            .build()
//
//        return EncryptedSharedPreferences.create(
//            ctx,
//            SHARED_PREFS_FILENAME,
//            masterKey,
//            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//        )
//    }


    /**
     * The function that starts the guided tour that is the manual
     * Philips Hue bridge setup.
     *
     * side effects:
     */
    fun startManualBridgeInit() {

    }

}

/**
 * Display states
 */
enum class MainActivityDisplayStates {
    /** The app is going through its startup sequence.  Show splash screen. */
    APP_STARTUP,
    /** Initialization process has begun.  Show init stuff.  todo: May need subivisions */
    INITIALIZING,
    /** The normal running state of the app.  All data is displayed and accessible. */
    RUNNING,
    /** When an error occurs, this indicates the error condition. todo: more error states needed */
    ERROR,
    /** When the app complete starts we're in an unknown state. */
    UNKNOWN
}

private const val TAG = "MainViewmodel"
