package com.sleepfuriously.paulsapp.model

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * @param   filename    The name of the prefs file.
 *
 * @return      The corresponding string.  Null if does not exist.
 *
 * WARNING:     While this is generally pretty fast, consider calling
 *              from outside the main thread.  As with any file access,
 *              this could take a while.
 */
fun getPrefsString(
    ctx: Context,
    key: String,
    filename: String,
) : String? {
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
    filename: String
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putString(key, value)
    }
}

/**
 * Removes a pref accessed by the given key.
 * If the pref doesn't exist, then nothing is done.
 * Doesn't care what the type is.
 *
 * @param   synchronize     When true, the value will be removed immediately.
 *                          This takes a little bit of time, so I recommend
 *                          using this param when calling outside the main thread.
 *                          The only time to use it if there are lots of reads
 *                          and writes at nearly the same time (perhaps in different
 *                          threads).  Honestly it's generally not needed.
 *                          Default is false, which is very fast and doesn't need
 *                          any special treatment.
 */
fun deletePref(
    ctx: Context,
    key: String,
    synchronize : Boolean = false,
    filename: String
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        remove(key)
    }
}

/**
 * Returns all string values that correspond to the list of keys.
 * Use this to get lots of values at once--it's much more efficient
 * than successive calls to [getPrefsString].
 *
 * Note:
 *  Because of all the hits to the prefs, this needs to be called off
 *  the main thread.
 *
 *  @param  keys    A Set of keys to access all the values in a single
 *                  prefs file.
 *
 *  @return     - A Set of Pairs, where the First is the key and the
 *              Second is the corresponding value.
 *              - If a key does not have a corresponding value, then its
 *              value will be null.
 */
suspend fun getLotsOfPrefsStrings(
    ctx: Context,
    keys: Set<String>,
    filename: String
) : Set<Pair<String, String?>> = withContext(Dispatchers.IO) {

    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    val retVals = mutableSetOf<Pair<String, String?>>()

    keys.forEach { key ->
        val pair = Pair(
            key,
            prefs.getString(key, null)
        )
        retVals.add(pair)
    }

    return@withContext retVals
}


/**
 * The opposite of [getLotsOfPrefsStrings], this saves a whole
 * bunch of key-value pairs at the same time.  It's much more efficient
 * than repeatedly calling [savePrefsString] over and over.
 *
 * Of course this needs to be called off the main thread as it can take
 * a while.
 *
 * @param   setOfPairs      A Set of Pair (like the name implies).  The
 *                          First of the pair is the key, and the Second
 *                          is the value.  Note that null is not allowed
 *                          for either.
 */
@Suppress("unused")
suspend fun saveLotsOfPrefsStrings(
    ctx: Context,
    setOfPairs: Set<Pair<String, String>>,
    filename: String
) = withContext(Dispatchers.IO) {

    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(true) {
        setOfPairs.forEach { pair ->
            putString(pair.first, pair.second)
        }
    }
}

/**
 * Use this if you have a bunch of things (Strings and Sets<String>) that
 * you want to save.  Sharedprefs don't like being called lots of times
 * (especially writes!), so this should handle that case without causing
 * data loss.
 *
 * @param   daStringPairs   This is a Map of multips KEYs and VALUEs.
 *                          Each key will map to its value.  Each will be
 *                          added to the sharedprefs.
 *                          Empty Set should be sent if not applicable.
 *
 * @param   daSets      This is even more complicated.  Each item in [daSets]
 *                      is a Map where the first item is a KEY.  The VALUE
 *                      is itself a SET of Strings.
 *                      Send an empty Set if nothing is relevant.
 *
 * @param   filename    The name of the prefs file.
 *
 * @param   synchronize When TRUE, this function will wait and do things the
 *                      slow way.
 */
fun savePrefsStringsAndSets(
    ctx: Context,
    daStringPairs: Map<String, String>,
    daSets: Map<String, Set<String>>,
    filename: String,
    synchronize: Boolean
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(commit = synchronize) {

        // enter daStringPairs
        daStringPairs.forEach { stringPair ->
            putString(stringPair.key, stringPair.value)
        }

        // and then do the sets
        daSets.forEach { aSet ->
            putStringSet(aSet.key, aSet.value)
        }
    }
}


/**
 * Retrieves a number from prefs with the given key.
 *
 * Returns [defaultValue] if not found.
 */
fun getPrefsInt(
    ctx: Context,
    key: String,
    defaultValue: Int,
    filename: String
) : Int {
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
    filename: String
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        putInt(key, num)
    }
}


/**
 * Same as [getPrefsInt] but for longs.
 */
@Suppress("unused")
fun getPrefsLong(
    ctx: Context,
    key: String,
    defaultValue: Long,
    filename: String
) : Long {
    val num: Long
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    try {
        num = prefs.getLong(key, defaultValue)
    }
    catch (e: ClassCastException) {
        Log.e(TAG, "Hey, I found something besides an Long in getPrefsLong(key = $key)!")
        e.printStackTrace()
        return defaultValue
    }
    return num
}

/**
 * Same as [savePrefsInt] but for longs.
 */
@Suppress("unused")
fun savePrefsLong(
    ctx: Context,
    key: String,
    num: Long,
    synchronize : Boolean = false,
    filename: String
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
@Suppress("unused")
fun getPrefsBool(
    ctx: Context,
    key: String,
    defaultBool: Boolean,
    filename: String
) : Boolean {
    val bool: Boolean
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
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
@Suppress("unused")
fun savePrefsBoolean(
    ctx: Context,
    key: String,
    bool: Boolean,
    synchronize : Boolean = false,
    filename: String
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
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
fun getPrefsSet(
    ctx: Context,
    key: String,
    filename: String
) : Set<String>? {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
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
 *
 * @param   clear       Set to TRUE if you want to clear ***-EVERYTHING-***
 *                      before calling this.  Yes, that's everything in the
 *                      entire prefs file!!!
 */
fun savePrefsSet(
    ctx: Context,
    key: String,
    filename: String,
    daSet: Set<String>,
    clear: Boolean,
    synchronize : Boolean = false
) {
    val prefs = ctx.getSharedPreferences(filename, Context.MODE_PRIVATE)
    prefs.edit(synchronize) {
        if (clear) {
            clear()     // necessary (for reasons that are unclear to everyone!)
        }
        putStringSet(key, daSet)
    }
}


/**
 * Gets all the keys from a preferences file in the form of a Set.
 */
@Suppress("unused")
fun getAllKeys(ctx: Context, filename : String) : MutableSet<String> {
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
const val PREFS_BRIDGE_PASS_TOKEN_FILENAME = "paulsapp_prefs"

