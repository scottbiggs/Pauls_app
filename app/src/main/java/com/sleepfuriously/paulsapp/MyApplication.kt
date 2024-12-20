package com.sleepfuriously.paulsapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

/**
 * The main Main of the app.  Big important globals, significant intializations,
 * and things that should never go away are put here.
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "Starting Paul's App")

        // get application context so it can be accessed in other places
        appContext = applicationContext

        // always in night mode, yay!
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        Log.v(TAG, "onConfigurationChanged()")
    }

    companion object {
        /** way to access the application context from anywhere */
        lateinit var appContext: Context
    }

}

private const val TAG = "MyApplication"