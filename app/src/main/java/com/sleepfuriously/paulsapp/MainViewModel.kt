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


    /** Will be false during initialization of models and viewmodels */
    var initializationComplete by mutableStateOf(false)
        private set


    /**
     * todo:  this is for testing the initialization graphic
     */
    init {
        viewModelScope.launch {
            delay(5000)
            initializationComplete = true
        }
    }

}