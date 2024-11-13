package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.OkHttpUtils
import com.sleepfuriously.paulsapp.model.getPrefsSet
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.savePrefsLong
import com.sleepfuriously.paulsapp.model.savePrefsString
import kotlinx.coroutines.runBlocking
import java.io.IOException

/***********************
 * Suite of functions and variables that involve the Philips Hue
 * Bridges.  Yes, the user may have bunches of bridges.
 *
 * DETAILS
 *
 * The Philips Hue bridge is the central device of the Philips Hue
 * lights.  It contains all the information about the light system
 * and is the primary point of contact for this app.
 *
 * The ip and token are generally stored in shared prefs (todo: use encrypted prefs),
 * but once the info is found, it is stored here for quick retrieval.
 * Saving this information is done asynchronously by default.  But for
 * special needs, it may be done synchronously (which of course needs
 * to be done off the Main thread).
 *
 *      Specifics on data storage
 *
 * Most of the data is stored in the default prefs file.  This includes
 * the info on how to access the different bridges.  The keys to access
 * the bridges are constructed thusly (based on [PhilipsHueBridgeInfo]):
 *
 * bridge ip = PHILIPS_HUE_BRIDGE_IP_KEY + bridge_id
 * token key = PHILIPS_HUE_BRIDGE_TOKEN_KEY + bridge_id
 *
 * The active boolean is set during initialization.
 *
 * Now, how do we find the id of the bridges?  That's a great question!
 * The ids are stored in a different preference file:
 *      PHILIPS_HUE_BRIDGE_ID_FILENAME
 *
 * This file is a Set of strings that can be converted to Ints that
 * will be the IDs of all the bridges that this app has seen (unless
 * deleted by the user).
 *
 * As you can see, it's very important that all the ids are unique.
 * To insure this, call [getNewId] when making a new bridge.  That
 * function will insure that the id is brand-spanking new.
 *
 * WARNING:
 *  The construction of this class can take a bit of time.
 *  Consider initiazling this class within a coroutine.
 */
class PhilipsHueBridgeUtils(private val ctx: Context) {

    //-----------------------------------
    //  private variables
    //-----------------------------------

    /**
     * Holds the about all the bridges that this app knows about.
     * Since each house can have multiple bridges, and this app can
     * be used across multiple houses, each bridge has an [PhilipsHueBridgeInfo.active]
     * value that tells if this bridge is currently in use.
     */
    private var philipsHueBridges = mutableSetOf<PhilipsHueBridgeInfo>()

    /** This will be true after initialization. todo: is this really needed? */
    private var initialized = false


    //-----------------------------------
    //  init
    //-----------------------------------

    init {
        properInit()
    }


    //-----------------------------------
    //  public functions
    //-----------------------------------

    /**
     * Returns the bridge with the given id.  If the id is bogus
     * this returns null.  Great way to check to see if a bridge
     * exists.
     *
     * fixme:  needs testing
     */
    fun getBridgeInfo(id: String) : PhilipsHueBridgeInfo? {
        return  philipsHueBridges.find { bridge ->
            bridge.id == id
        }
    }


    /**
     * Use this function to make an id for a newly discovered
     * bridge.
     *
     * @return      A number that's guaranteed to be unique from
     *              all the other bridges
     *
     * fixme:  needs testing
     */
    @Suppress("FoldInitializerAndIfToElvis")
    fun getNewId() : String {

        // strategy: go through the id (as numbers) until we don't
        // match an existing id.  Not very fast, O(n^2), sigh.

        // start by getting a list of all the ids
        val idList = mutableSetOf<String>()
        for (info in philipsHueBridges) {
            idList.add(info.id)
        }

        var id = 1
        while (true) {

            // try to get the bridge with the current id
            val bridge = getBridgeInfo(id.toString())
            if (bridge == null) {
                // can't find it? then that id isn't used! yay!
                return id.toString()
            }

            // try the next one
            id++
        }
    }


    /**
     * Tests to see if a bridge is at the given ip.
     *
     * @param   ip          The ip that may point to a philips hue bridge
     *
     * WARNING:
     *  This must be called off the main thread as it access
     *  the network.
     */
    suspend fun doesBridgeRespondToIp(ip: String) : Boolean {

        // exit with false if no ip or wrong format
        if (ip.isEmpty() || (isValidBasicIp(ip) == false)) return false

        try {
            val fullIp = createFullAddress(
                prefix = PHILIPS_HUE_BRIDGE_URL_PREFIX,
                ip = ip,
                suffix = PHILIPS_HUE_BRIDGE_TEST_SUFFIX
            )
            Log.v(TAG, "doesBridgeRespondToIp() requesting response from $fullIp")

            val myResponse = OkHttpUtils.synchronousGetRequest(fullIp)

            if (myResponse.isSuccessful) {
                return true
            }
        }
        catch (e: IOException) {
            Log.e(TAG, "error when testing the bridge (IOException).  The ip ($ip) is probably bad.")
            e.printStackTrace()
            return false
        }
        catch (e: IllegalArgumentException) {
            Log.e(TAG, "error when testing the bridge (IllegalArgumentException).  The ip (\"$ip\") is probably bad.")
            e.printStackTrace()
            return false
        }

        return false
    }

    /**
     * Finds the current "name" for the given philips hue bridge.  The name
     * is actually a token necessary for any real communication with the
     * bridge.
     *
     * NOTE:    This does care if the bridge is currently active or
     *          not.  This simply returns any token that has been
     *          recorded for this app.
     *
     * NOTE:    Does not check that the token is correct.  This can
     *          happen if connected to a different bridge or the
     *          bridge has forgotten this token somehow.
     *
     * @param   id      The id of the bridge to get the token for.
     *
     * @return      The token (String) needed to access the bridge.
     *              Null if no token has been assigned or the id
     *              isn't found.
     */
    fun getBridgeToken(id: String) : String? {
        val bridge = getBridgeInfo(id) ?: return null
        return bridge.token
    }

    /**
     * Sets the given string to be the new philips hue bridge
     * token.  This also stores the token in long-term memory.
     *
     * If the bridge does not exist, then a new one is created.
     *
     * todo: test this!  Make sure that the bridge is added AND the token is saved for both short and long term
     */
    fun setBridgeToken(bridgeId: String, newToken: String) {

        var bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            // create a new bridge
            bridge = PhilipsHueBridgeInfo(bridgeId)
            philipsHueBridges.add(bridge)
        }
        bridge.token  = newToken

        // save this
        savePrefsString(ctx, assembleTokenKey(bridgeId), newToken)
    }


    /**
     * Determines if the bridge understands the token (also called "name").
     *
     * This is a network call and should not be called on the main thread.
     *
     * @param       bridgeId    Id for the bridge to test.
     *
     * @param       token       Token to test.  Philips Hue also calls this
     *                          the "name."  It is specific to a mobile device
     *                          (and probably the app).
     *
     * @return      True if the bridge responds to the current token.
     *              False for all other cases.
     */
    suspend fun testBridgeToken(bridgeId: String, token: String) : Boolean {

        val ip = getBridgeIPStr(bridgeId) ?: return false

        try {
            val fullIp = createFullAddress(
                prefix = PHILIPS_HUE_BRIDGE_URL_PREFIX,
                ip = ip,
                suffix = PHILIPS_HUE_BRIDGE_TEST_SUFFIX
            )
            Log.v(TAG, "doesBridgeRespondToIp() requesting response from $fullIp")

            // todo
            // todo: actually access the bridge!!!
            // todo

        }
        // fixme: catch the appropriate exceptions (instead of this catch-all)
        catch(e: Exception) {
            Log.d(TAG, "error when testing bridge token")
            return false
        }

        return true
    }


    /**
     * Retrieves a string version of the ip from the current bridge info.
     *
     * @param       id      Identifier for the bridge in question.
     *
     * @return      The ip of the bridge in String form.  Null if not found.
     */
    fun getBridgeIPStr(id: String) : String? {
        val bridge = getBridgeInfo(id) ?: return null

        return bridge.ip
    }

    /**
     * Sets the ip of the bridge.  Overwrites any existing ip.
     * Does nothing if the bridge can't be found.
     *
     * preconditions:
     *      The bridge is already created.
     *
     * @param   bridgeId    id of the bridge that we're working with
     *
     * @param   newIp   String representation of the ip to access the bridge
     *                  locally.
     */
    fun saveBridgeIpStr(bridgeId: String, newIp: String) {
        val bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "can't find bridge $bridgeId in saveBridgeIpStr()!")
            return
        }

        // strip any prefix from this ip and save the info
        val justIp = newIp.substringAfter(PHILIPS_HUE_BRIDGE_URL_PREFIX)
        bridge.ip = justIp
        val ipKey = assembleIpKey(bridgeId)
        savePrefsString(ctx, ipKey, newIp)
    }


    /**
     * Gets the timestamp of the last time the bridge was used.
     *
     * @return      The time in millis since 1970 when this bridge
     *              was last accessed.
     *              Null if the bridge can't be found or the data was
     *              weird.
     */
    fun getBridgeLastUsed(bridgeId: String) : Long? {
        val bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "can't find bridge $bridgeId in getBridgeLastUsed()!")
            return null
        }
        return bridge.lastUsed
    }

    fun saveBridgeLastUsed(bridgeId: String, timeStamp: Long) {
        val bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "can't find bridge $bridgeId in saveBridgeLastUsed()!")
            return
        }

        bridge.lastUsed = timeStamp
        savePrefsLong(ctx, assembleLastUsedKey(bridgeId), timeStamp)
    }


    /**
     * Is this bridge currently in use?  This function will tell ya.
     * But it'll return null if it can't find the bridge at all.
     */
    fun getBridgeActive(bridgeId: String) : Boolean? {
        val bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "cannot find bridge $bridgeId in getBridgeActive()!")
            return null
        }
        return bridge.active
    }

    /**
     * Sets the active bit in the given bridge.  Does nothing if
     * the bridge can't be found.
     *
     * NOTE
     *  This does not save the data permanently.  Each time this app
     *  is run (or comes into view or is polled) the active state of the bridges
     *  is re-assessed.
     */
    fun saveBridgeActive(bridgeId: String, active: Boolean) {
        val bridge = getBridgeInfo(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "cannot find bridge $bridgeId in saveBridgeActive()!")
            return
        }
        bridge.active = active
    }


    /**
     * Returns a Set of all the philips hue bridges that are
     * known to this app.  Does not checking whatsoever.
     */
    fun getAllBridges() : Set<PhilipsHueBridgeInfo> {
        return philipsHueBridges
    }


    /**
     * Returns a Set of all bridges that are currently active.
     */
    fun getAllActiveBridges(bridges: Set<PhilipsHueBridgeInfo> = philipsHueBridges) : Set<PhilipsHueBridgeInfo> {
        val activeBridges = mutableSetOf<PhilipsHueBridgeInfo>()

        bridges.forEach { bridge ->
            if (bridge.active) {
                activeBridges.add(bridge)
            }
        }

        return activeBridges
    }


    //-----------------------------------
    //  private functions
    //-----------------------------------

    /**
     * This exists because kotlin sucks at inits.  Please don't
     * call this function except during initialization.
     *
     * This loads any data that is stored in shared prefs.  Actual
     * tests are not done.
     */
    private fun properInit() {

        // idiot test to make sure this isn't run more than once.
        if (initialized) {
            return
        }

        runBlocking {
            // Load the bridge ids.
            val bridgeIds = getAllBridgeIdsFromPrefs()
            if (bridgeIds.isNullOrEmpty()) {
                // initialization complete
                initialized = true      // todo: is this really needed?
                return@runBlocking
            }

            // use these ids to load all the data
            for (id in bridgeIds) {
                val ip = getBridgeIPStr(id)
                val token = getBridgeToken(id)
                val lastUsed = getBridgeLastUsed(id) ?: 0L
                philipsHueBridges.add(PhilipsHueBridgeInfo(id, ip, token, lastUsed))
            }

            // done initializing
            initialized = true
        }
    }

    /**
     * This function is designed to be called periodically to make sure
     * all the bridges active data is up-to-date.
     *
     * Not to be confused with [pollBridgeData] which gathers data from
     * a specific bridge.
     */
    private suspend fun pollBridges() {
        // todo
    }

    private suspend fun pollBridgeData() {
        // todo
    }

    /**
     * Returns the key needed to access the pref for a bridge's
     * ip.
     *
     * @param   bridgeId        The id of the bridge in question.
     */
    private fun assembleIpKey(bridgeId: String) : String {
        return PHILIPS_HUE_BRIDGE_URL_PREFIX + bridgeId
    }

    private fun assembleTokenKey(bridgeId: String) : String {
        return PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX + bridgeId
    }

    private fun assembleLastUsedKey(bridgeId: String) : String {
        return PHILIPS_HUE_BRIDGE_LAST_USED_KEY_PREFIX + bridgeId
    }


    /**
     * Loads all the known bridge ids from the prefs.
     *
     * @return      A Set of bridge ids (bridge ids are Strings).
     *              The Set will be empty if none are found.
     *              It'll be null if there was a different problem
     *              (probably the prefs file wasn't created yet).
     */
    private suspend fun getAllBridgeIdsFromPrefs() : Set<String>? {
        val idSet = getPrefsSet(ctx, PHILIPS_HUE_BRIDGE_ALL_IDS_KEY)
        return idSet
    }


    /**
     * Constructs the complete address from the constituent parts.
     *
     * @param   ip      The main part of the address.  This is generally thought
     *                  to be the domain name + ".com" or whatever domain suffix
     *                  is appropriate.  In our use, this also can contain the
     *                  prefix, which is usually "http://" or "https://" as well.
     *                  Also in our cases, the domain is rarely used--we use the
     *                  translated ip number, like "196.192.86.1".  So the most
     *                  common case is:  "http://196.192.86.1"
     *
     * @param   prefix  If the prefix is not contained in the ip, then this holds
     *                  it.  Generally this will be "https://" or "http://".
     *                  Defaults to "".
     *
     * @param   suffix  Anything that goes AFTER the ip.  This is the directory
     *                  or parameters or whatever.  Remember that this should start
     *                  with a backslash ('/').  Defaults to "".
     */
    private fun createFullAddress(
        ip: String,
        prefix: String = PHILIPS_HUE_BRIDGE_URL_PREFIX,
        suffix: String = "",
    ) : String {

        val fullAddress = "${prefix}$ip${suffix}"
        Log.d(TAG,"fullAddress created. -> $fullAddress")
        return fullAddress
    }

}


//-----------------------------------
//  constants
//-----------------------------------

private const val TAG = "PhilipsHueBridgeUtils"

/** this prefix should appear before the numbers of the bridge's ip address */
private const val PHILIPS_HUE_BRIDGE_URL_PREFIX = "http://"

/** append this to the bridge's ip to get the debug screen */
private const val PHILIPS_HUE_BRIDGE_TEST_SUFFIX = "/debug/clip.html"


//----------------
//  prefs
//

/** the name of the file that holds all the philips hue bridge ids */
private const val PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME = "ph_bridges_prefs"

/** key for getting all the ids for all the bridges */
private const val PHILIPS_HUE_BRIDGE_ALL_IDS_KEY = "ph_bridge_all_ids"

/**
 * Prefix of key to get the token for the philips hue bridge from prefs.
 * The actual key is PHILIPS_HUE_BRIDGE_TOKEN_KEY + bridge_id
 */
private const val PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX = "ph_bridge_token_key"

/** prefix of key to get ip from prefs (see [PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX]) */
private const val PHILIPS_HUE_BRIDGE_IP_KEY_PREFIX = "ph_bridge_ip_key"

/** prefix for accessing the last-used date for a philips hue bridge (see [PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX]) */
private const val PHILIPS_HUE_BRIDGE_LAST_USED_KEY_PREFIX = "ph_bridge_last_used_key"

