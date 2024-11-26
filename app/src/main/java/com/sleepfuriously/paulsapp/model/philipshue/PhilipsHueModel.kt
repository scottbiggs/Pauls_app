package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import androidx.collection.mutableIntSetOf
import com.google.gson.Gson
import com.sleepfuriously.paulsapp.model.OkHttpUtils
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPostString
import com.sleepfuriously.paulsapp.model.getPrefsSet
import com.sleepfuriously.paulsapp.model.getPrefsString
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.savePrefsSet
import com.sleepfuriously.paulsapp.model.savePrefsString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * This is the model for the Philips Hue suite of Internet of Things.
 * These things are primarily lights, but there are a few more items,
 * most importantly: BRIDGES.
 *
 * This class does the basics: CRUD (create, read, update, delete) on our Philips
 * Hue data as well as some utility functions, most of which are private.
 * This class also has some general utility functions, which are placed
 * after the CRUD functions.
 *
 * Bridges control everything in the Philips Hue world.  So a connection
 * to a bridge allows us to control whatever it connects to.  We can connect
 * directly to lights, but for now we will just connect to bridges and let
 * those bridges do the controlling of the lights.  This is pretty much
 * how Philips wants us to do it.
 *
 * I'm using version v2 of the Philips Hue API.  Docs can be found:
 *      https://developers.meethue.com/develop/hue-api-v2/
 *
 * ---------------------------------------------------
 *
 * CONVENTIONS
 *
 *  - The functions are divided into public and private functions, then along
 *    CRUD divisions.
 *
 *  - Quick functions that access in-memory data use get...(), is...(), and
 *    set...() in their function names.  Long-term data storage use load...()
 *    and save...().  Delete functions are always both short and long-term.
 *
 *  - Long-term functions are usually safe to run on the
 *    Main thread (unless you do several quickly).  For those there are command
 *    line parameters to make these operations synchronous, and of course you
 *    should to these off the Main thread.
 *
 * ---------------------------------------------------
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
 * To insure this, call [generateNewId] when making a new bridge.  That
 * function will insure that the id is brand-spanking new.
 *
 * WARNING:
 *  The construction of this class can take a bit of time.
 *  Consider initializing this class within a coroutine.
 */
class PhilipsHueModel(private val ctx: Context) {

    init {
        properInit()
    }


    //-------------------------------------
    //  create
    //-------------------------------------

    /**
     * This creates a new bridge and adds it the permanent bridge
     * data.
     *
     * It's assumed that whenever this happens, the bridge will be
     * currently active and that any additional data will be added
     * later.
     *
     * preconditions
     *      The newBridge MUST contain a token!!!
     *
     * @param   newBridge       A nearly fully-loaded bridge info class
     *                          (the id will be set BY THIS FUNCTION).
     *                          The data is NOT checked for accuracy.
     *
     * @return      The id for this new bridge.
     */
    suspend fun addBridge(newBridge: PhilipsHueNewBridge) : String {

        // update our local data
        val uniqueBridgeId = generateNewId()
        val bridgeToAdd = PhilipsHueBridgeInfo(
            id = uniqueBridgeId,
            ip = newBridge.ip,
            token = newBridge.token,
            lastUsed = System.currentTimeMillis(),
            active = newBridge.active,
            rooms = mutableSetOf(),
        )
        bridges.add(bridgeToAdd)

        // update long-term data
        saveBridge(bridgeToAdd)

        return uniqueBridgeId
    }


    //-------------------------------------
    //  read
    //-------------------------------------

    /**
     * Returns a Set of all the bridges currently loaded in this class.
     * Could be empty if no bridges are registered or we haven't loaded
     * bridges from the long-term storage yet.
     *
     * NOTE
     *  Changing any data of these bridges will not be propogated
     *  and saved.  The only way to save that data is to use the
     *  functions below like [saveBridgeIp], [saveBridgeToken], etc.
     */
    fun getAllBridges() : MutableSet<PhilipsHueBridgeInfo> {
        return bridges
    }

    /**
     * Returns a Set of all bridges that are currently active.
     *
     * preconditions
     *  - [bridges] is already setup and running.  Otherwise this just
     *    doesn't make sense.
     */
    fun getAllActiveBridges() : MutableSet<PhilipsHueBridgeInfo> {
        val activeBridges = mutableSetOf<PhilipsHueBridgeInfo>()

        bridges.forEach { bridge ->
            if (bridge.active) {
                activeBridges.add(bridge)
            }
        }

        return activeBridges
    }


    /**
     * Returns a Set of all the bridge ids from the currently loaded bridges.
     * You can use these to access those bridge as most of these functions
     * need a bridge id.
     *
     * Note
     *  This should be used only after bridge data has been loaded from
     *  long-term storage.
     *
     * @return      Set (not mutable) of Strings that represent the ids of
     *              the various bridges.  Could be empty of no bridges are
     *              registered.
     */
    fun getAllBridgeIds() : Set<String> {
        val ids = mutableSetOf<String>()
        bridges.forEach() {
            ids.add(it.id)
        }
        return ids
    }

    /**
     * Returns the bridge with the given id from the current
     * Set of bridges.  If the id is bogus this returns null.
     * Great way to check to see if a bridge exists.
     *
     * @return  The [PhilipsHueBridgeInfo] of the bridge with the
     *          given id.
     *          Null if id is not found.
     */
    fun getBridge(bridgeId: String) : PhilipsHueBridgeInfo? {
        return  bridges.find { bridge ->
            bridge.id == bridgeId
        }
    }


    //-------------------------------------
    //  update
    //-------------------------------------

    /**
     * Saves the data of all the bridges into shared prefs.  Any existing prefs
     * will be overwritten.
     *
     * Because this hits the shared prefs multiple times, to avoid any race
     * conditions, this should be run off the main thread.
     *
     * side effects
     *      - shared prefs files will be modified
     */
    suspend fun saveAllBridgesData() {

        //------------
        //  Explanation:
        // Because I'm using sharedprefs, the bridge data needs to be saved in a
        // simple way.  There are two methods.
        //
        //  - A Set of ids is stored.  It is kept in its own file.
        //
        //  - Each aspect of the data is stored in its own key-value pair.
        //  That means that we have to generate a unique key for the values.
        //  The values that need saving are:
        //
        //      IPs for the bridges
        //
        //      tokens for the bridges
        //
        //  The key for each of these follows this formula:
        //
        //      key = item_prefix + bridge_id
        //
        //  For example
        //      The token key for bridge (with id = 42) would be:
        //      key = PHILIPS_HUE_BRIDGE_TOKEN_KEY_PREFIX + 42
        //

        // build the Set of keys

        TODO()
    }


    //-------------------------------------
    //  delete
    //-------------------------------------

    /**
     * Removes a bridge, effectively deleting it from this app.
     * If the bridge is currently active, we'll try to tell the bridge
     * to let go this token (username) to free up bridge memory.
     *
     * This bridge will be removed from both the short-term and long-term
     * storage.  Yup, it's gone forever.
     *
     * @return      True - successfully removed bridge.
     *              False - bridge wasn't found.
     */
    fun deleteBridge(bridgeId: String) : Boolean {
        TODO()
    }

    /**
     * This is the nuclear option.  Removes all the bridges for all time.
     * This is essentially a reset of the app.
     *
     * @return      True - all bridges were removed (even if there were none).
     *              False - error condition (although I don't know what).
     */
    fun deleteAllBridges() : Boolean {

        var error = false
        bridges.forEach() { bridge ->
            if (deleteBridge(bridge.id) == false) {
                Log.d(TAG, "removeAllBridges() unable to remove ${bridge.id}!")
                error = true
            }
        }
        return error
    }


    //-------------------------------------
    //  utils
    //-------------------------------------

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
        if (ip.isEmpty() || (isValidBasicIp(ip) == false)){
            Log.d(TAG, "doesBridgeRespondToIp($ip) - ip is empty or not valid! Aborting!")
            return false
        }

        try {
            val fullIp = createFullAddress(
//                prefix = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
                prefix = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
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
     * This asks the bridge for a token (name).  This is part of the
     * Philips Hue security system.  All inquiries will need this token
     * to succeed.
     *
     * @param   bridgeIp        The ip of the bridge we want to register this
     *                          app to.
     *
     * @return  A Pair.  The first will either be a String or empty.  If it's
     *   a String, then it holds token and the second part of the Pair
     *   can be ignored.
     *   If the first is empty (""), then the second contains information about
     *   what sort of error it is (see [GetBridgeTokenErrorEnum]).
     *
     * preconditions
     *      The user has hit the button on this bridge just a few seconds before
     *      this request happens.  If not, then the bridge will send an error
     *      (like "button not hit").
     */
    suspend fun registerAppToBridge(
        bridgeIp: String
    ) : Pair<String, GetBridgeTokenErrorEnum> {

        val returnPair = Pair<String?, GetBridgeTokenErrorEnum>(null, GetBridgeTokenErrorEnum.NO_ERROR)


        // fixme: is the username even needed?
        val username = constructUserName(bridgeIp)
        val deviceType = constructDeviceType(bridgeIp)
        Log.i(TAG, "registerAppToBridge() - username = $username, deviceType = $deviceType")

        val fullAddress = createFullAddress(ip = bridgeIp, suffix = "/api/")
        Log.d(TAG, "registerAppToBridge() - fullAddress = $fullAddress")

        val response = synchronousPostString(
            fullAddress,
            // fixme:  this is NOT RIGHT (just using for testing)
            bodyStr = """
{
  "devicetype": "$deviceType"
}
                """
        )

        if (response.isSuccessful) {
            // get token
            val token = getTokenFromBodyStr(response.body)
            Log.i(TAG, "requestTokenFromBridge($bridgeIp) -> $token")

            if (token != null) {
                return Pair(token, GetBridgeTokenErrorEnum.NO_ERROR)
            }
            else {
                Log.e(TAG, "unable to parse body in requestTokenFromBridge($bridgeIp)")
                Log.e(TAG, "   response => \n$response")
                return Pair("", GetBridgeTokenErrorEnum.CANNOT_PARSE_RESPONSE_BODY)
            }
        }
        else {
            Log.e(TAG, "error: unsuccessful attempt to get token from bridge at ip $bridgeIp")
            Log.e(TAG, "   response => \n$response")
            return Pair("", GetBridgeTokenErrorEnum.UNSUCCESSFUL_RESPONSE)
        }
    }


    /**
     * Determines if the bridge understands the token (also called "name").
     *
     * This is a network call and should not be called on the main thread.
     *
     * @param       bridgeIp   ip of the bridge to test.
     *
     * @param       token       Token to test.  Philips Hue also calls this
     *                          the "name."  It is specific to a mobile device
     *                          (and probably the app).
     *
     * @return      True if the bridge responds to the current token.
     *              False for all other cases.
     */
    suspend fun testBridgeToken(bridgeIp: String, token: String) : Boolean {

        try {
            val fullIp = createFullAddress(
//                prefix = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
                prefix = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
                ip = bridgeIp,
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


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // private data
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /** holds all the bridges that this app know about */
    private lateinit var bridges : MutableSet<PhilipsHueBridgeInfo>

    /** This will be true after initialization. Needed to prevent accidentally double initialization. */
    private var initialized = false


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //  private functions
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    //~~~~~~~~~~~
    //  create
    //~~~~~~~~~~~

    /**
     * Use this function to make an id for a newly discovered
     * bridge.
     *
     * @return      An id string that's guaranteed to be unique from
     *              all the other bridges
     *
     * fixme:  needs testing
     */
    private fun generateNewId() : String {

        // strategy: go through the id (as numbers) until we don't
        // match an existing id.  Not very fast, O(n^2), sigh.

        // create a Set of bridgeIds (in number form)
        val bridgeIds = mutableIntSetOf()
        bridges.forEach() { bridge ->
            bridgeIds.add(bridge.id.toInt())
        }

        // start counting until we come up with a unique number
        var id = 1
        while (true) {
            if (bridgeIds.contains(id) == false) {
                return id.toString()
            }
            // try the next one (keep trying...we'll eventually succeed!)
            id++
        }
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
//        prefix: String = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
        prefix: String = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
        suffix: String = "",
    ) : String {

        val fullAddress = "${prefix}$ip${suffix}"
        Log.d(TAG,"fullAddress created. -> $fullAddress")
        return fullAddress
    }

    /**
     * To obtain the token for accessing a bridge, we have to tell that
     * bridge a name for this app (username).  This function makes
     * sure that it's done in a consistent manner.
     *
     * The username is a constant + the ip of that particular bridge.
     * This makes sure that we use different user names for each
     * bridge (just in case there's some sort of confusion).
     *
     * Not sure if the user name needs to be remembered or not.  I guess
     * I'll find out soon enough!  (The token: yeah, that needs to be
     * remembered!)
     */
    private fun constructUserName(bridgeIp: String) : String {
        return PHILIPS_HUE_BRIDGE_DEVICE_NAME_PREFIX + bridgeIp
    }

    /**
     * When registering this app to a philips hue bridge, it's curious
     * about the type of device talking to it.  This returns a string
     * for that purpose.
     */
    private fun constructDeviceType(bridgeIp: String) : String {
        return PHILIPS_HUE_BRIDGE_DEVICE_TYPE
    }


    //~~~~~~~~~~~
    //  read
    //~~~~~~~~~~~

    /**
     * This exists because kotlin sucks at inits.  Please don't
     * call this function except during initialization.
     *
     * This loads any data that is stored in shared prefs.
     *
     * side effects
     *  - [bridges] will contain data as in the shared prefs
     */
    private fun properInit() {

        // idiot test to make sure this isn't run more than once.
        if (initialized) {
            Log.e(TAG, "Error: tried to initialize more than once!!!")
            return
        }

        bridges = mutableSetOf()

        // Load the bridges
        runBlocking(Dispatchers.IO) {
            // start with the ids
            val bridgeIds = loadAllBridgeIdsFromPrefs()
            if (bridgeIds.isNullOrEmpty()) {
                // initialization complete as there's nothing to load
                initialized = true
                return@runBlocking
            }

            // use these ids to load the rest of the data
            bridgeIds.forEach() { id ->

                // get the IP for this bridge
                val ip = loadBridgeIpFromPrefs(id)

                // get the token for this bridge
                val token = loadBridgeTokenFromPrefs(id)

                val bridge = PhilipsHueBridgeInfo(
                    id = id,
                    ip = ip,
                    token = token,
                )

                bridges.add(bridge)
            }

            // todo: This will tell us which bridges are active
//            pollBridgesActive()

            // done initializing
            initialized = true
        }
    }

    /**
     * Loads all the known bridge ids from the prefs.
     *
     * @return      A Set of bridge ids (bridge ids are Strings).
     *              The Set will be empty if none are found.
     *              It'll be null if there was a different problem
     *              (probably the prefs file wasn't created yet).
     */
    private suspend fun loadAllBridgeIdsFromPrefs() : Set<String>? {
        val idSet = withContext(Dispatchers.IO) {
            getPrefsSet(
                ctx = ctx,
                filename = PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME,
                key = PHILIPS_HUE_BRIDGE_ALL_IDS_KEY)
        }
        return idSet
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
    private suspend fun loadBridgeTokenFromPrefs(bridgeId: String) : String {

        val key = assembleTokenKey(bridgeId)
        val token = withContext(Dispatchers.IO) {
            getPrefsString(ctx, key)
        }

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
    private suspend fun loadBridgeIpFromPrefs(bridgeId: String) : String {

        val key = assembleIpKey(bridgeId)
        val ip = withContext(Dispatchers.IO) {
            getPrefsString(ctx, key)
        }

        Log.d(TAG, "loadBridgeIpFromPrefs($bridgeId) => $ip")
        return ip ?: ""
    }


    /**
     * This takes a string, converts it to Gson, then looks around to
     * find the token (name) part.  This token is then returned.
     *
     * @param       bodyStr     The body as returned from a POST or GET
     *                          response.
     *
     * @return      The token within the contents of the string.
     *              Null if not found.
     */
    private fun getTokenFromBodyStr(bodyStr: String) : String? {

        Log.d(TAG, "getTokenFromBodyStr() started. bodyStr = $bodyStr")

        // convert to our kotlin data class
        val bridgeResponseData = Gson().fromJson(bodyStr, PhilipsHueBridgeRegistrationResponse::class.java)
            ?: return null      // just return null if bridgeResponse is null

        // there will be just one item
        val bridgeResponseItem = bridgeResponseData[0]

        // is there an error?
        if (bridgeResponseItem.error != null) {
            Log.e(TAG, "Getting token from bridge error:\n   type = ${bridgeResponseItem.error.type}\n   desc = ${bridgeResponseItem.error.description}")
            return null
        }

        // can't find a success, must be unrecoverable error
        if (bridgeResponseItem.success == null) {
            Log.e(TAG, "Getting token from bridge error:   Can't find success data")
            return null
        }

        Log.v(TAG, "Getting token from bridge:   token = ${bridgeResponseItem.success.username}")
        return bridgeResponseItem.success.username
    }


    //~~~~~~~~~~~
    //  update
    //~~~~~~~~~~~

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
        synchronize: Boolean = false
    ) : Boolean {

        // make a Set of ids including the new one.
        val idSet = mutableSetOf<String>()
        bridges.forEach() { bridge ->
            idSet.add(bridge.id)
        }

        // add the new id
        idSet.add(newBridgeId)

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
     * side effects:
     *      - The ip of the bridge is changed locally as well as
     *      saved in the prefs.
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
    fun saveBridgeIp(
        bridgeId: String,
        newIp: String,
        synchronize: Boolean = false
    ) : Boolean {

        // does this bridge exist?
        val bridge = getBridge(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "can't find bridge $bridgeId in saveBridgeIpStr()!")
            return false
        }

        // strip any prefix from this ip and save the info
//        val justIp = newIp.substringAfter(PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX)
        val justIp = newIp.substringAfter(PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX)
        bridge.ip = justIp                      // local

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
        synchronize: Boolean = false
    ) : Boolean {

        // does this bridge exist?
        val bridge = getBridge(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "can't find bridge $bridgeId in saveBridgeToken()!")
            return false
        }

        bridge.token = newToken     // local save

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
    private suspend fun saveBridge(bridge: PhilipsHueBridgeInfo) : Boolean {

        val returnVal = withContext(Dispatchers.IO) {

            if (saveBridgeId(bridge.id) == false) {
                Log.e(TAG, "Problem saving id in saveBridge($bridge) - aborting!")
                return@withContext false
            }

            if (bridge.ip == null) {
                Log.e(TAG, "Error in saveBridge($bridge) - ip is null - aborting!")
                return@withContext false
            }

            if (saveBridgeIp(bridge.id, bridge.ip!!, true) == false) {
                Log.e(TAG, "Can't save the IP of the bridge in saveBridge($bridge) - aborting!")
                return@withContext false
            }

            if (bridge.token == null) {
                Log.e(TAG, "Error in saveBridge($bridge) - token is null - aborting!")
                return@withContext false
            }

            if (saveBridgeToken(bridge.id, bridge.token!!, true) == false) {
                Log.e(TAG, "Can't save the token of the bridge in saveBridge($bridge) - aborting!")
                return@withContext false
            }

            true
        }
        return returnVal
    }

    //~~~~~~~~~~~
    //  delete
    //~~~~~~~~~~~

    //~~~~~~~~~~~
    //  util
    //~~~~~~~~~~~

    /**
     * This function is designed to be called periodically to make sure
     * all the bridges active data is up-to-date.
     *
     * Not to be confused with [pollBridgeDataChanged] which gathers data from
     * a specific bridge.
     *
     * side effects
     *  - The bridges in [bridges] may have their Active value changed.
     *
     * @return      False only if an error occurred.
     */
    private suspend fun pollBridgesActive() : Boolean {
        TODO()
    }

    /**
     * Call this periodically on active bridges.  It'll reveal the details of
     * the lights and such that are controlled by this bridge.
     *
     * @param   bridgeId        The bridge with this id will be polled to see if
     *                          it is still active and if any of the room data
     *                          has changed.  If so, that data will be updated.
     *
     * @return  - Returns true if the bridge has changed in any way.
     */
    private suspend fun pollBridgeDataChanged(bridgeId: String) : Boolean {
        TODO()
    }


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
//  classes
//-------------------------------------

/**
 * These are the possibilities of the types of errors that
 * can occur while trying to get a new token (username) from
 * the bridge.
 */
enum class GetBridgeTokenErrorEnum {
    NO_ERROR,
    /** ip is not proper format */
    BAD_IP,
    /** reponse was not successful--probably a bad url or no bridge */
    UNSUCCESSFUL_RESPONSE,
    /** for whatever reason there WAS a successful response, but a token wasn't found */
    TOKEN_NOT_FOUND,
    /** successful response, but the body would not parse properly--perhaps ip went to wrong device? */
    CANNOT_PARSE_RESPONSE_BODY,
    /** user did not hit the button on the bridge before we tried to register with it */
    BUTTON_NOT_HIT
}


//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueModel"

/** this prefix should appear before the numbers of the bridge's ip address when testing */
private const val PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX = "https://"
private const val PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX = "http://"

/** append this to the bridge's ip to get the debug screen */
private const val PHILIPS_HUE_BRIDGE_TEST_SUFFIX = "/debug/clip.html"

/**
 * When registering this app with a bridge, we need to tell it what kind of device
 * this is.  Here's the string.
 */
private const val PHILIPS_HUE_BRIDGE_DEVICE_TYPE = "sleepfuriously android client"

/**
 * Device ids (usernames) are used to get the token from bridges.  The username will
 * be the following string + the ip of this bridge.  You can construct this
 * by calling [PhilipsHueModel.constructUserName].
 */
private const val PHILIPS_HUE_BRIDGE_DEVICE_NAME_PREFIX = "sleepfuriously_p"


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



