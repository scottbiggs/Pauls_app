package com.sleepfuriously.paulsapp.model

import android.content.Context
import android.util.Log
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
 * @param   filename    The name of the prefs file.  Defaults to
 *                      PREFS_FILENAME.
 *
 * @return      The corresponding string.  Null if does not exist.
 *
 * WARNING:     While this is generally pretty fast, consider calling
 *              from outside the main thread.  As with any file access,
 *              this could take a while.
 */
fun getPrefsString(ctx: Context, key: String, filename: String = PREFS_FILENAME) : String? {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
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
fun savePrefsString(
    ctx: Context,
    key: String,
    value: String,
    synchronize : Boolean = false,
    filename: String = PREFS_FILENAME
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putString(key, value)
    }
}

/**
 * Retrieves a number from prefs with the given key.
 *
 * Returns [defaultValue] if not found.
 */
fun getPrefsInt(ctx: Context, key: String, defaultValue: Int, filename: String = PREFS_FILENAME) : Int {
    val num: Int
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    try {
        num = prefs.getInt(key, defaultValue)
    }
    catch (e: ClassCastException) {
        Log.e(TAG, "Hey, I found something besides an Int in getPrefsInt(key = $key)!")
        e.printStackTrace()
        return defaultValue
    }

    return num
}

/**
 * Sets an Int prefs
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
fun savePrefsInt(
    ctx: Context,
    key: String,
    num: Int,
    synchronize : Boolean = false,
    filename: String = PREFS_FILENAME
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putInt(key, num)
    }
}

/**
 * Same as [savePrefsInt] but for longs.
 */
fun savePrefsLong(
    ctx: Context,
    key: String,
    num: Long,
    synchronize : Boolean = false,
    filename: String = PREFS_FILENAME
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putLong(key, num)
    }
}


/**
 * Retrieves a number from prefs with the given key.
 *
 * Returns [defaultBool] if not found.
 */
fun getPrefsBool(
    ctx: Context,
    key: String,
    defaultBool: Boolean,
    filename: String = PREFS_FILENAME
) : Boolean {
    val bool: Boolean
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    try {
        bool = prefs.getBoolean(key, defaultBool)
    }
    catch (e: ClassCastException) {
        Log.e(TAG, "Hey, I found something besides a Boolean in getPrefsBool(key = $key)!")
        e.printStackTrace()
        return defaultBool
    }

    return bool
}

/**
 * Sets an Int prefs
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
fun savePrefsBoolean(
    ctx: Context,
    key: String,
    bool: Boolean,
    synchronize : Boolean = false,
    filename: String = PREFS_FILENAME
) {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putBoolean(key, bool)
    }
}


/**
 * Returns a Set of Strings from the given key. If no key is present
 * or the key is somehow in error, null is returned.
 *
 * Note that the returned set is NOT mutable.
 */
fun getPrefsSet(ctx: Context, key: String, filename: String = PREFS_FILENAME) : Set<String>? {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    try {
        val emptySet = setOf<String>()
        val set = prefs.getStringSet(key, emptySet)
        if (set.isNullOrEmpty()) {
            return null
        }
        return set
    }
    catch (e: ClassCastException) {
        Log.e(TAG, "getPrefsSet() key = $key yielded a pref that wasn't a set!")
        e.printStackTrace()
        return null
    }
}

/**
 * Saves the given set to the prefs with the given key.  Same caveats
 * as the other set() functions here.
 */
fun savePrefsSet(
    ctx: Context,
    key: String,
    daSet: Set<String>,
    synchronize : Boolean = false
) {
    val prefs = ctx.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putStringSet(key, daSet)
    }
}


/**
 * Gets all the keys from a preferences file in the form of a Set.
 */
fun getAllKeys(ctx: Context, filename : String = PREFS_FILENAME) : MutableSet<String> {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    return prefs.all.keys
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

