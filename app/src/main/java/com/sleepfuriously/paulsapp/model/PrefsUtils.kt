package com.sleepfuriously.paulsapp.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/*********************
 * Utility functions that deal with shared preferences.
 *
 * todo:  make encrypted
 *
 ********************/

//----------------------
//  variables
//----------------------

//----------------------
//  public functions
//----------------------

/**
 * Returns the token for the philips hue bridge.  If no token exists
 * then null is returned.
 *
 * WARNING:     This should be called from outside the main thread as
 *              with any file access, this could take a while.
 */
fun getPhilipsHueBridgeToken(ctx: Context) : String? {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    return prefs.getString(PHILIPS_HUE_BRIDGE_TOKEN_KEY, null)
}


/**
 * Sets the philips hue bridge token to the given string.
 *
 * Note
 *      This uses asynchronous saving.  This means it's possible for
 *      race conditions to happen if this is called often.
 *
 * @param   synchronous     When false (default) this does asynchronous
 *                          writes.  If you're doing lots of these at the
 *                          same time, set synchronous to true, which will
 *                          prevent any possibility of writes clashing with
 *                          each other.  In those cases, I also recommend
 *                          calling this function outside of the Main thread
 *                          as it might take a little while to run.
 */
fun setPhilipsHueBridgeToken(ctx: Context, newToken: String, synchronous : Boolean = false) {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    prefs.edit(synchronous) {
        putString(PHILIPS_HUE_BRIDGE_TOKEN_KEY, newToken)
    }
}


//----------------------
//  private functions
//----------------------

//----------------------
//  constants
//----------------------

private const val TAG = "PrefsUtil"

/** name of the preference file */
private const val PREFS_FILENAME = "paulsapp_prefs"


/** key to get the token for the philips hue bridge */
private const val PHILIPS_HUE_BRIDGE_TOKEN_KEY = "ph_bridge_key"

