package com.sleepfuriously.paulsapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the startup splash screen.  Pretty simple.
 */
class SplashViewModel : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    /** will be true when everything is ready for the splash screen to go away */
    // todo: refactor this (put it within the regular viewmodel)!
    val isReady = _isReady.asStateFlow()

    /**
     * Just pauses a bit for the splash screen animation to look nice.
     */
    init {
        viewModelScope.launch {
            delay(2000L)
            _isReady.value = true
        }
    }

}