package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.utils.isConnectivityWifiWorking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    @Deprecated("use ActiveSystem enum")
    private val _activeTab = MutableStateFlow(0)
    /**
     * This will hold the active tab index (the tab that is current).
     * Also the section of the program that is being displayed (in case
     * tabs are no longer used).
     */
    @Deprecated("use ActiveSystem enum")
    val activeTab = _activeTab.asStateFlow()

    private val _activeSystem = MutableStateFlow<ActiveSystem>(ActiveSystem.MainScreen)
    /** Specifies which of the different systems is currently displayed */
    val activeSystem = _activeSystem.asStateFlow()


    private val _intializing = MutableStateFlow(true)
    /** Will be TRUE during initialization or a reset (starts true). */
    val intializing = _intializing.asStateFlow()

    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()

    //---------------------------
    //  init
    //---------------------------

    fun initialize(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // check wifi--that's about all there is for the main viewmodel to do
            _wifiWorking.update { isConnectivityWifiWorking(ctx) }
            Log.d(TAG, "done initializing")
            _intializing.update { false }      // done initializaing
        }
    }

    //---------------------------
    //  functions
    //---------------------------

    /**
     * Call this when you want the tab to change
     */
    @Deprecated("use changeSystem(ActiveSystem)")
    fun changeTab(newTabIndex: Int) {
        _activeTab.update { newTabIndex }
    }

    /**
     * Call to change the current displayed system.
     */
    fun changeSystem(newSystem: ActiveSystem) {
        _activeSystem.update { newSystem }
    }

}

//---------------------------
//   classes, enums, constants
//---------------------------

/**
 * Specifies which UI state we are currently displaying.
 */
enum class ActiveSystem {
    MainScreen,
    PhilipsHue,
    Pool,
    Sprinkler,
    Error
}

/**
 * All the info to specify where to draw an icon.
 * Note that these are in percentages!  1 is furthest, while 0 is
 * the least.  So 0,0 will be top left, and 1,1 is bottom right.
 */
data class SystemIconPosition(
    val x: Float,
    val y: Float
)

private const val TAG = "MainViewmodel"
