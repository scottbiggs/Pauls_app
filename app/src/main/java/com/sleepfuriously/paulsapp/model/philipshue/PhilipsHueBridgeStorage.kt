package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.deletePref
import com.sleepfuriously.paulsapp.model.getPrefsSet
import com.sleepfuriously.paulsapp.model.getPrefsString
import com.sleepfuriously.paulsapp.model.savePrefsSet
import com.sleepfuriously.paulsapp.model.savePrefsString

/**
 * Controls storing and retrieving data about the bridges that
 * persists when the app isn't running.  Essentially all that
 * info is stored in sharedprefs.
 */
object PhilipsHueBridgeStorage {

    //---------------------------
    //  read
    //---------------------------

    /**
     * Loads all the known bridge ids from the prefs.
     *
     * @return      A Set of bridge ids (bridge ids are Strings).
     *              The Set will be empty if none are found.
     *              It'll be null if there was a different problem
     *              (probably the prefs file wasn't created yet).
     */
    fun loadAllBridgeIds(ctx: Context) : Set<String>? {
        val prefs = getPrefsSet(
            ctx = ctx,
            filename = PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME,
            key = PHILIPS_HUE_BRIDGE_ALL_IDS_KEY
        )
        val numPrefs = prefs?.size ?: 0
        Log.d(TAG, "loadAllBridgeIds() found $numPrefs bridges")
        return prefs
    }


    /**
     * Finds the token associated with the given bridge that was saved in
     * the prefs.
     *
     * @param       bridgeId        Id of bridge
     *
     * @return      The token (username) that this app uses to communicate
     *              with the bridge.
     *              Returns empty string on error.
     */
    fun loadBridgeToken(bridgeId: String, ctx: Context) : String {

        val key = assembleTokenKey(bridgeId)
        val token = getPrefsString(ctx, key)

        Log.d(TAG, "loadBridgeTokenFromPrefs($bridgeId) => $token")
        return token ?: ""
    }

    /**
     * Finds the IP that is stored for the given bridge that was saved in
     * the prefs.
     *
     * @param   bridgeId        The id of bridge in question.
     *
     * @return      The IP string.  Returns empty string if bridge not found (or some
     *              other error).
     */
    fun loadBridgeIp(bridgeId: String, ctx: Context) : String {

        val key = assembleIpKey(bridgeId)
        val ip = getPrefsString(ctx, key)

        Log.d(TAG, "loadBridgeIpFromPrefs($bridgeId) => $ip")
        return ip ?: ""
    }

    //---------------------------
    //  write
    //---------------------------

    /**
     * Saves the id of the bridge in the list of bridge ids.  The id is needed
     * to retrieve other long-term storage aspects about the bridge.
     *
     * Note
     *      This should be called BEFORE saving the token or the IP for this
     *      bridge.  It only makes sense!
     *
     * @param       newBridgeId     Will return error if this is not unique.
     *
     * @param       synchronize     When true (default is false), this will
     *                              not return until the write is complete.
     *                              This MUST be run outside of the Main thread
     *                              when this happens!!!
     *
     * @return      True - successfully saved
     *              False - some error prevented saving
     */
    private fun saveBridgeId(
        newBridgeId: String,
        synchronize: Boolean = false,
        ctx: Context,
    ) : Boolean {

//        Log.d(TAG, "saveBridgeId() newBridgeId = $newBridgeId")

        // get the current id set
        val idSet = (loadAllBridgeIds(ctx) ?: setOf()) as MutableSet
//        Log.d(TAG, "saveBridgeId() loaded current set: idSet size = ${idSet.size}, idSet = $idSet")

        // add the new id
        idSet.add(newBridgeId)
//        Log.d(TAG, "saveBridgeId() added newBridge: idSet size = ${idSet.size}, idSet = $idSet")

        // now save this in our shared prefs
        savePrefsSet(
            ctx = ctx,
            filename = PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME,
            key = PHILIPS_HUE_BRIDGE_ALL_IDS_KEY,
            synchronize = synchronize,
            daSet = idSet
        )
        return true
    }


    /**
     * Saves the ip of a bridge in long-term storage (the prefs).
     * Overwrites any existing ip.
     * Does nothing if the bridge can't be found.
     *
     * preconditions:
     *      The bridge is already created.
     *
     * @param   bridgeId    id of the bridge that we're working with
     *
     * @param   newIp   String representation of the ip to access the bridge
     *                  locally.
     *
     * @param   synchronize     When true (default is false), this will
     *                          not return until the write is complete.
     *                          This MUST be run outside of the Main thread
     *                          when this happens!!!
     *
     * @return      True - successfully saved
     *              False - could not find the bridgeId
     */
    private fun saveBridgeIp(
        bridgeId: String,
        newIp: String,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {

        val ipKey = assembleIpKey(bridgeId)
        savePrefsString(ctx, ipKey, newIp, synchronize)      // long-term

        return true
    }

    /**
     * Saves the token into the specified bridge both locally and in long-term
     * prefs storage.  If the bridge already exists then its token is overridden.
     *
     * preconditions
     *      - The bridge must already exist
     *
     * side effects
     *      - The local data for the bridge is changed to reflect the new token
     *      as well as long-term storage.
     *
     * @param       bridgeId        Id of the bridge we want to change the token.
     *
     * @param       newToken        The token to replace the existing token with.
     *
     * @param       synchronize     When true (default is false), this will
     *                              not return until the write is complete.
     *                              This MUST be run outside of the Main thread
     *                              when this happens!!!
     *
     * @return      True - successfully saved
     *              False - could not find the bridgeId
     */
    private fun saveBridgeToken(
        bridgeId: String,
        newToken: String,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {

        val tokenKey = assembleTokenKey(bridgeId)
        savePrefsString(ctx, tokenKey, newToken, synchronize)      // long-term

        return true
    }

    /**
     * Saves the data in the given bridge into long-term storage.
     * This is pretty much the bridge id, the IP, and its token.
     *
     * Because this does multiple saves in a row, you need to call
     * this off the main thread to avoid any possible race condition.
     *
     * @param       bridge      The bridge to save (complete).
     *                          Warning: this id MUST BE UNIQUE if
     *                          we are saving a NEW bridge!!!
     *                          This does not check the uniqueness
     *                          as I don't know if we're making a new
     *                          bridge or updating an existing one.
     *
     * @return      True - successfully saved
     *              False - some problem prevented saving
     */
    fun saveBridge(
        bridge: PhilipsHueBridgeInfo,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {
//        Log.d(TAG, "saveBridge(): ${bridge.id}")

        if (saveBridgeId(bridge.id, synchronize, ctx) == false) {
            Log.e(TAG, "Problem saving id in saveBridge($bridge) - aborting!")
            return false
        }

        if (saveBridgeIp(bridge.id, bridge.ip, synchronize, ctx) == false) {
            Log.e(TAG, "Can't save the IP of the bridge in saveBridge($bridge) - aborting!")
            return false
        }

        if (saveBridgeToken(bridge.id, bridge.token, synchronize, ctx) == false) {
            Log.e(TAG, "Can't save the token of the bridge in saveBridge($bridge) - aborting!")
            return false
        }

        return true
    }

    //---------------------------
    //  delete
    //---------------------------

    /**
     * Removes the bridge id from long-term storage (prefs).  If it's not
     * there, nothing is done.
     *
     * Note:    This does NOT remove the bridge data from our short-term
     *  internal cache.  The caller is responsible for that.
     *
     * @param       bridgeId        Id of the bridge we want to remove.
     *
     * @param       synchronize     When true (default is false), this will
     *                              not return until the write is complete.
     *                              This MUST be run outside of the Main thread
     *                              when this happens!!!
     *
     * @return      True - successfully removed
     *              False - could not find the bridgeId
     */
    fun removeBridgeId(
        bridgeId: String,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {

        // Get the set of ids.  We'll have to change it
        // and save that changed set.
        var idSet = loadAllBridgeIds(ctx)
        if (idSet == null) {
            Log.v(TAG, "trying to remove bridge (id = $bridgeId), but I can't find any bridges! Skipping.")
            return false
        }

        // remove this bridge
        idSet = idSet - bridgeId

        // now save this in our shared prefs, overwriting
        // any previous set of IDs.
        savePrefsSet(
            ctx = ctx,
            filename = PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME,
            key = PHILIPS_HUE_BRIDGE_ALL_IDS_KEY,
            synchronize = synchronize,
            daSet = idSet
        )
        return true
    }

    /**
     * Removes the given token from our permanent storage.
     * If the token does not already exist, then nothing is done.
     *
     * Note:    This does NOT remove the bridge token from our short-term
     *  internal cache.  The caller is responsible for that.
     *
     * @param       bridgeId        Id of the bridge we want to change the token.
     *
     * @param       synchronize     When true (default is false), this will
     *                              not return until the write is complete.
     *                              This MUST be run outside of the Main thread
     *                              when this happens!!!
     *
     * @return      True - successfully removed
     *              False - could not find the bridgeId
     */
    fun removeBridgeTokenPrefs(
        bridgeId: String,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {

        val tokenKey = assembleTokenKey(bridgeId)
        deletePref(ctx, tokenKey, synchronize)
        return true
    }

    /**
     * Removes the given IP from our permanent storage.
     * If the bridge does not exist, then nothing is done.
     *
     * @param       bridgeId        Id of the bridge we want to change the token.
     *
     * @param       synchronize     When true (default is false), this will
     *                              not return until the write is complete.
     *                              This MUST be run outside of the Main thread
     *                              when this happens!!!
     */
    fun removeBridgeIP(
        bridgeId: String,
        synchronize: Boolean = false,
        ctx: Context
    ) {

        val ipKey = assembleIpKey(bridgeId)
        deletePref(ctx, ipKey, synchronize)
    }



    //---------------------------
    //  helpers
    //---------------------------

    /**
     * Returns the key needed to access the pref for a bridge's
     * ip.
     *
     * @param   bridgeId        The id of the bridge in question.
     */
    private fun assembleIpKey(bridgeId: String) : String {
        return PHILIPS_HUE_BRIDGE_IP_KEY_PREFIX + bridgeId
    }

    private fun assembleTokenKey(bridgeId: String) : String {
        return PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX + bridgeId
    }



}

//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueBridgeStorage"

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
