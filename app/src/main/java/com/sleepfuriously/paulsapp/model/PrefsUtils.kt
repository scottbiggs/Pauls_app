package com.sleepfuriously.paulsapp.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/*********************
 * Utility functions that deal with shared preferences.
 *
 * This is the central calling point for any accesses to the
 * preferences (which is where a lot of data is stored).
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
 * Returns a String item from the prefs.
 *
 * @param   ctx     ye old context
 *
 * @param   key     The key for this string.
 *
 * @return      The corresponding string.  Null if does not exist.
 *
 * WARNING:     While this is generally pretty fast, consider calling
 *              from outside the main thread.  As with any file access,
 *              this could take a while.
 */
fun getPrefsString(ctx: Context, key: String) : String? {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    return prefs.getString(key, null)
}


/**
 * Creates a new String entry in the preferences with the given key and value.
 * If the key already exists, it will be overwritten.
 *
 * @param   synchronize     When true, the value will be written immediately.
 *                          This takes a little bit of time, so I recommend
 *                          using this param when calling outside the main thread.
 *                          The only time to use it if there are lots of reads
 *                          and writes at nearly the same time (perhaps in different
 *                          threads).  Honestly it's generally not needed.
 *                          Default is false, which is very fast and doesn't need
 *                          any special treatment.
 */
fun setPrefsString(ctx: Context, key: String, value: String, synchronize : Boolean = false) {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putString(key, value)
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



