package com.sleepfuriously.paulsapp.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Viewmodel for the main portion of the opening screen.
 * This should only incorporate the broad aspects of the
 * main screen.  Each tab component should have its
 * own viewmodel.
 */
class MainViewmodel : ViewModel() {

    //---------------------------
    //  flows
    //---------------------------

    private val _activeTab = MutableStateFlow<Int>(0)
    /** This will hold the active tab index (the tab that is current) */
    val activeTab = _activeTab.asStateFlow()

    //---------------------------
    //  functions
    //---------------------------

    /**
     * Call this when you want the tab to change
     */
    fun changeTab(newTabIndex: Int) {
        _activeTab.update { newTabIndex }
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

private const val TAG = "MainViewmodel"
