package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.PREFS_BRIDGE_PASS_TOKEN_FILENAME
import com.sleepfuriously.paulsapp.model.deletePref
import com.sleepfuriously.paulsapp.model.getPrefsSet
import com.sleepfuriously.paulsapp.model.getPrefsString
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.WorkingFlock
import com.sleepfuriously.paulsapp.model.savePrefsSet
import com.sleepfuriously.paulsapp.model.savePrefsString
import com.sleepfuriously.paulsapp.model.savePrefsStringsAndSets

/**
 * Controls storing and retrieving data about the Philips Hue system
 * that persists when the app isn't running.  Essentially all info is
 * stored in sharedprefs.
 */
object PhilipsHueStorage {

    //---------------------------
    //  read
    //---------------------------

    //--------------------
    //  Bridges
    //

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
        val token = getPrefsString(ctx, key, PREFS_BRIDGE_PASS_TOKEN_FILENAME)

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
        val ip = getPrefsString(ctx, key, PREFS_BRIDGE_PASS_TOKEN_FILENAME)

        Log.d(TAG, "loadBridgeIpFromPrefs($bridgeId) => $ip")
        return ip ?: ""
    }

    //--------------------
    //  Flocks
    //

    /**
     * Loads the various [WorkingFlock] from long-term storage.
     *
     * Here's how zones are stored:
     *
     *  Flocks have their own file, [PHILIPS_HUE_FLOCK_PREFS_FILENAME].
     */
    fun loadFlocks(ctx: Context) : Set<WorkingFlock> {
        TODO("If necessary")
    }

    /**
     * Returns all the ids for all the flocks.  These keys are used to find
     * all the other data.
     *
     * @return      A Set of all the Flock ids.  Returns empty set if none are
     *              found.
     */
    private fun loadFlockIds(ctx: Context) : Set<String> {
        val idSet = getPrefsSet(
            ctx = ctx,
            key = PHILIPS_HUE_FLOCK_IDS_KEY,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )

        if (idSet == null) {
            Log.e(TAG, "loadFlockIds() - unable to get flock ids!  Probably none to get yet.")
            return emptySet()
        }

        return idSet
    }

    /**
     * Loads the name of the given flock.
     *
     * The key for the name is comes from [assembleFlockNameKey].
     *
     * @param   flockId     The unique id for this flock.  It's used to create
     *                      the key for its name.
     */
    private fun loadFlockName(ctx: Context, flockId: String) : String? {

        // unique key for this name
        val key = assembleFlockNameKey(flockId)
        val name = getPrefsString(
            ctx = ctx,
            key = key,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )
        if (name == null) {
            Log.e(TAG, "loadFlockName() - unable to find the name of flock $flockId")
        }
        return name
    }

    /**
     * Loads all the room ids for the given flock.
     *
     * @return  A Set of the ids for the rooms.  The Set will be empty if none
     *          are found.
     */
    private fun loadFlockRoomIds(ctx: Context, flockId: String) : Set<String> {
        val key = assembleFlockRoomsKey(flockId)
        val roomIds = getPrefsSet(
            ctx = ctx,
            key = key,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )

        if (roomIds == null) {
            Log.e(TAG, "loadFlockRoomIds() - unable to find any rooms (not even no rooms)")
            return emptySet()
        }
        return roomIds
    }

    /**
     * Similar to [loadFlockRoomIds] but for zones.
     */
    private fun loadFlockZoneIds(ctx: Context, flockId: String) : Set<String> {
        val key = assembleFlockZonesKey(flockId)
        val zoneIds = getPrefsSet(
            ctx = ctx,
            key = key,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )
        if (zoneIds == null) {
            Log.e(TAG, "loadFlockRoomIds() - unable to find any zones (not even no zones)")
            return emptySet()
        }
        return zoneIds
    }

    //---------------------------
    //  write
    //---------------------------

    //--------------------
    //  Bridges
    //

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

        // get the current id set
        val idSet = (loadAllBridgeIds(ctx) ?: setOf())

        // create a new id set
        val newIdSet = mutableSetOf<String>()

        // add the new id
        newIdSet.add(newBridgeId)

        // and add the old bridges
        idSet.forEach { id ->
            newIdSet.add(id)
        }

        // now save this in our shared prefs
        savePrefsSet(
            ctx = ctx,
            filename = PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME,
            key = PHILIPS_HUE_BRIDGE_ALL_IDS_KEY,
            synchronize = synchronize,
            daSet = newIdSet
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
        savePrefsString(ctx, ipKey, newIp, synchronize, PREFS_BRIDGE_PASS_TOKEN_FILENAME)

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
        savePrefsString(ctx, tokenKey, newToken, synchronize, PREFS_BRIDGE_PASS_TOKEN_FILENAME)

        return true
    }

    /**
     * Saves the data in the given bridge into long-term storage.
     * This is pretty much the bridge id, the IP, and its token.
     *
     * Because this does multiple saves in a row, you need to call
     * this off the main thread to avoid any possible race condition.
     *
     * @param       bridgeId    The bridge to save (complete).
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
        bridgeId: String,
        bridgeipAddress: String,
        newToken: String,
        synchronize: Boolean = false,
        ctx: Context
    ) : Boolean {
//        Log.d(TAG, "saveBridge(): ${bridge.id}")

        if (saveBridgeId(bridgeId, synchronize, ctx) == false) {
            Log.e(TAG, "Problem saving id in saveBridge(id = $bridgeId) - aborting!")
            return false
        }

        if (saveBridgeIp(bridgeId, bridgeipAddress, synchronize, ctx) == false) {
            Log.e(TAG, "Can't save the IP of the bridge in saveBridge(id = $bridgeId) - aborting!")
            return false
        }

        if (saveBridgeToken(bridgeId, newToken, synchronize, ctx) == false) {
            Log.e(TAG, "Can't save the token of the bridge in saveBridge(id = $bridgeId) - aborting!")
            return false
        }

        return true
    }

    //--------------------
    //  Flocks
    //

    /**
     * Does all the work of saving this flocks' info to long-term storage.
     *
     * @return      True if everything works.  False on any kind of error.
     */
    fun saveFlock(ctx: Context, flock: PhilipsHueFlockInfo) {

        // Prep for calling savePrefsStringsAndSets()
        val stringPairs = mutableMapOf<String, String>()
        val setPairs = mutableMapOf<String, Set<String>>()

        // figure out the flock's name
        val flockNameKey = assembleFlockNameKey(flock.id)
        stringPairs[flockNameKey] = flock.name

        // add the rooms
        val roomsKey = assembleFlockRoomsKey(flock.id)
        val roomsValueSet = flock.roomSet.map { it.v2Id }.toSet()
        setPairs[roomsKey] = roomsValueSet

        // add the zones
        val zonesKey = assembleFlockZonesKey(flock.id)
        val zonesValueSet = flock.zoneSet.map { it.v2Id }.toSet()
        setPairs[zonesKey] = zonesValueSet

        // Finally the actual id of the flock itself.  This is tricky
        // as we need to modify the existing flock set to include this
        // new id

        // get the current flock id set
        val flockIdSet = loadFlockIds(ctx)

        // create new id set
        val newFlockIdSet = flockIdSet + flock.id

        // add it
        setPairs[PHILIPS_HUE_FLOCK_IDS_KEY] = newFlockIdSet

        // And NOW we are ready to actually save to prefs
        savePrefsStringsAndSets(
            ctx = ctx,
            daStringPairs = stringPairs,
            daSets = setPairs,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME,
            synchronize = false
        )
    }

    //---------------------------
    //  delete
    //---------------------------

    //--------------------
    //  Bridges
    //

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
        deletePref(ctx, tokenKey, synchronize, PREFS_BRIDGE_PASS_TOKEN_FILENAME)
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
        deletePref(ctx, ipKey, synchronize, PREFS_BRIDGE_PASS_TOKEN_FILENAME)
    }


    //--------------------
    //  Flocks
    //

    /**
     * Call this to delete the given flock from long-term storage.
     *
     * Deleting a Flock entails:
     *  - deleting the flock's name
     *  - deleting the flock's room IDs
     *  - deleting the flock's zone IDs
     *  - finally deleting the Flock IDs
     */
    suspend fun removeFlock(ctx: Context, flock: PhilipsHueFlockInfo) {
        removeFlockName(ctx, flock)
        removeFlockRoomIDs(ctx, flock)
        removeFlockZoneIDs(ctx, flock)
        removeFlockId(ctx, flock)
    }

    /**
     * Removes the ID for the given flock from long-term storage.  The
     * other data referenced by this ID will no longer be accessible, so
     * remember to delete those too!
     */
    private suspend fun removeFlockId(ctx: Context, flock: PhilipsHueFlockInfo) {
        //
        // Removing just one element from a Set is tricky.
        //  1. copy the Set of IDs
        //  2. remove the ID we want to delete
        //  3. delete the original Set of IDs
        //  4. save the new set.
        //
        val flockIdSet = loadFlockIds(ctx)
        val newFlockIdSet = flockIdSet - flock.id
        deletePref(
            ctx = ctx,
            key = PHILIPS_HUE_FLOCK_IDS_KEY,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME,
            synchronize = true
        )
        savePrefsSet(
            ctx = ctx,
            key = PHILIPS_HUE_FLOCK_IDS_KEY,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME,
            newFlockIdSet,
            synchronize = true
        )
    }

    /**
     * Removes the given flock name from long-term storage.  Helper function
     * for [removeFlock].
     */
    private suspend fun removeFlockName(ctx: Context, flock: PhilipsHueFlockInfo) {
        val deleteNameKey = assembleFlockNameKey(flock.id)
        deletePref(
            ctx = ctx,
            key = deleteNameKey,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )
    }

    /**
     * Removes all the room IDs associated with the given flock from
     * long-term storage.
     */
    private suspend fun removeFlockRoomIDs(ctx: Context, flock: PhilipsHueFlockInfo) {
        val deleteRoomsKey = assembleFlockRoomsKey(flock.id)
        deletePref(
            ctx = ctx,
            key = deleteRoomsKey,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )
    }

    /**
     * Removes all the zone IDs for the given flock from long-term storage.
     */
    private suspend fun removeFlockZoneIDs(ctx: Context, flock: PhilipsHueFlockInfo) {
        val deleteZonesKey = assembleFlockZonesKey(flock.id)
        deletePref(
            ctx = ctx,
            key = deleteZonesKey,
            filename = PHILIPS_HUE_FLOCK_PREFS_FILENAME
        )
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

    /**
     * Creates the key needed to access a flock's name in the prefs.
     * You still need to use the correct filename later.
     */
    fun assembleFlockNameKey(flockId: String) : String {
        return PHILIPS_HUE_FLOCK_NAME_KEY_PREFIX + flockId
    }

    fun assembleFlockRoomsKey(flockId: String) : String {
        return PHILIPS_HUE_FLOCK_ROOM_ID_KEY_PREFIX + flockId
    }

    fun assembleFlockZonesKey(flockId: String) : String {
        return PHILIPS_HUE_FLOCK_ZONE_ID_KEY_PREFIX + flockId
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

//---
//  flocks

/** filename to access the Philips Hue prefs that hold Flock info */
const val PHILIPS_HUE_FLOCK_PREFS_FILENAME = "ph_flock_prefs"

/** key to access all the ids for all the flocks */
const val PHILIPS_HUE_FLOCK_IDS_KEY = "flock_ids_key"

/** Merged with the flock id to make the key to access that flock's name */
private const val PHILIPS_HUE_FLOCK_NAME_KEY_PREFIX = "flock_name_prefix-"

/** Merged with the flock id to make the full key to access a Set of room ids */
private const val PHILIPS_HUE_FLOCK_ROOM_ID_KEY_PREFIX = "flock_room_prefix-"

/** The zone id key is made by concatenating the flock id with this */
private const val PHILIPS_HUE_FLOCK_ZONE_ID_KEY_PREFIX = "flock_zone_prefix-"