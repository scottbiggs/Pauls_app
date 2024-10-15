package com.sleepfuriously.paulsapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Viewmodel for the main portion of the opening screen.
 * This should only incorporate the broad aspects of the
 * main screen.  The parts of the screen should have their
 * own viewmodels.
 */
class MainViewModel(ctx: Context) : ViewModel() {

    /** Tells the UI which state to display */
    var displayStates by mutableStateOf(MainActivityDisplayStates.UNKNOWN)
        private set

    /** communicates state of bridge initialization to the ui */
    var bridgeInit by mutableStateOf(PhilipsHueBridgeInit.BRIDGE_INITIALIZING)
        private set


    /**
     * todo:  this is for testing the initialization graphic
     */
    init {
        viewModelScope.launch {

            val epref = getESharedPref(ctx) as EncryptedSharedPreferences

            val ph_name = epref.getString(PHILIPS_HUE_NAME_KEY, PH_NAME_NOT_FOUND)
            if (ph_name.equals(PH_NAME_NOT_FOUND)) {
                bridgeInit = PhilipsHueBridgeInit.BRIDGE_UNINITIALIZED
                Log.v(TAG, "Philips Hue Bridge not initialized")
                return@launch
            }


            delay(3000)
            bridgeInit = PhilipsHueBridgeInit.BRIDGE_INITIALIZED
        }
    }


    /**
     * Gets a reference to an encrypted shared prefs file.  I presume that
     * if it doesn't exist, a new one will be created.  Should otherwise
     * work just like a regular prefs file.
     *
     * from:  https://stackoverflow.com/a/64115229/624814
     */
    private fun getESharedPref(ctx: Context) : SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            ctx,
            SHARED_PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


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

/**
 * The states of the Philips Hue bridge.
 */
enum class PhilipsHueBridgeInit {

    /** the bridge has not been initialized yet */
    BRIDGE_UNINITIALIZED,
    /** currently attempting to initialize the bridge */
    BRIDGE_INITIALIZING,
    /** attempt to initialize the bridge has timed out */
    BRIDGE_INITIALIZATION_TIMEOUT,
    /** successfully initialized */
    BRIDGE_INITIALIZED,
    /** some error has occurred when dealing with the bridge */
    ERROR
}

private const val TAG = "MainViewModel"

/** Name of the encrypted shared prefs file */
private const val SHARED_PREFS_FILENAME = "prefs"

/**
 * Key for preferences that holds the secret name to access the
 * philips hue bridge for this app.
 */
private const val PHILIPS_HUE_NAME_KEY = "ph_name_key"

/**
 * If the philips hue bridge access name is this, that means that the
 * name hasn't been created yet.  Time to initialize the PH bridge!
 */
private const val PH_NAME_NOT_FOUND = "not_found"