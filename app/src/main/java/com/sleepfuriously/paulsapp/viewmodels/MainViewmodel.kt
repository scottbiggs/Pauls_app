package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.utils.isConnectivityWifiWorking
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

    private val _activeTab = MutableStateFlow(0)
    /** This will hold the active tab index (the tab that is current) */
    val activeTab = _activeTab.asStateFlow()

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
        viewModelScope.launch {
            // check wifi--that's about all there is for the main viewmodel to do
            _wifiWorking.value = isConnectivityWifiWorking(ctx)
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
    fun changeTab(newTabIndex: Int) {
        _activeTab.update { newTabIndex }
    }

}

//---------------------------
//   classes, enums, constants
//---------------------------

private const val TAG = "MainViewmodel"
