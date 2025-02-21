package com.sleepfuriously.paulsapp.model.philipshue

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure
import android.util.Log
import androidx.collection.mutableIntSetOf
import com.google.gson.Gson
import com.sleepfuriously.paulsapp.model.philipshue.json.*
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Light
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceLightsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceRoomsAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Room
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.getTime
import com.sleepfuriously.paulsapp.model.OkHttpUtils.getAllTrustingSseClient
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousGet
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPost
import com.sleepfuriously.paulsapp.model.deletePref
import com.sleepfuriously.paulsapp.model.getPrefsSet
import com.sleepfuriously.paulsapp.model.getPrefsString
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.json.PHBridgePostTokenResponse
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2GroupedLight
import com.sleepfuriously.paulsapp.model.savePrefsSet
import com.sleepfuriously.paulsapp.model.savePrefsString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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
 *  - Quick functions that access in-memory data use get...(), is...(), and
 *    set...() in their function names.  Long-term data storage use load...()
 *    and save...().  Delete functions are always both short and long-term.
 *
 *  - Functions that access a bridge always are appended with ...[From|To]Api().
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
class PhilipsHueModel(
    private val ctx: Context = MyApplication.appContext,
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------------
    //  flow data
    //-------------------------------------

    private val _bridgeFlowSet = MutableStateFlow<Set<PhilipsHueBridgeInfo>>(setOf())
    /** Flow for the bridges. Collectors will be notified of changes to bridges from this. */
    val bridgeFlowSet = _bridgeFlowSet.asStateFlow()


    //-------------------------------------
    //  class data
    //-------------------------------------

    /**
     * Reference to Event Source--I use it to close the connections.
     * Each item in the list is a Pair:
     *      bridgeId : EventSource
     */
//    private val eventSourceList = mutableListOf<Pair<String, EventSource>>()
    /** reference to Event Source--I use it to close the connection */
    private lateinit var myEventSource : EventSource


    private val defaultListener = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            Log.d(TAG, "EventSourceListener: Connection Opened")
            Log.i(TAG, "   time = ${getTime()}")

            // todo - translate this info and put it in a flow so the
            //  can display it
            if (response.isSuccessful) {
                val eventJsonString = response.body?.string()
                Log.d(TAG, "sse - onOpen: json = $eventJsonString")

                val bridge = getBridgeFromEventSource(eventSource)
                if (bridge == null) {
                    Log.e(TAG, "Unable to get bridge in listener.onOpen()!  Nothing to update!")
                    return
                }

                // trying a new way to update a Set. Hope it works.
                _bridgeFlowSet.update {
                    val currentBridgeFlowSet = bridgeFlowSet.value.toMutableSet()
                    currentBridgeFlowSet.remove(bridge)
                    bridge.connected = true         // set connect status
                    currentBridgeFlowSet.add(bridge)
                    currentBridgeFlowSet
                }

            }
            else {
                Log.e(TAG, "problem with response in defaultListener.onOpen()!")
                Log.e(TAG, "   code = ${response.code}")
                Log.e(TAG, "   message = ${response.message}")
                Log.e(TAG, "   request = ${response.request}")
                Log.e(TAG, "   time = ${getTime()}")
            }
            response.body?.close()
        }

        override fun onClosed(eventSource: EventSource) {
            super.onClosed(eventSource)
            Log.d(TAG, "sse - onClosed() for eventSource ${eventSource.toString()}")
            Log.i(TAG, "   time = ${getTime()}")

            val bridge = getBridgeFromEventSource(eventSource)
            if (bridge == null) {
                Log.e(TAG, "Unable to get bridge in listener.onClosed()!")
                return
            }

            _bridgeFlowSet.update {
                val currentBridgeFlowSet = bridgeFlowSet.value.toMutableSet()
                currentBridgeFlowSet.remove(bridge)
                bridge.connected = false         // set connect status
                currentBridgeFlowSet.add(bridge)
                currentBridgeFlowSet
            }
        }

        /**
         * This is the event that the server sends us!!!!  YAY!
         */
        override fun onEvent(
            eventSource: EventSource,
            id: String?,
            type: String?,
            data: String
        ) {
            super.onEvent(eventSource, id, type, data)
            Log.d(TAG, "EventSourceListener: On Event Received!")
            Log.d(TAG, "   eventSource = ${eventSource.toString()}")
            Log.d(TAG, "   id = $id")
            Log.d(TAG, "   type = $type")
            Log.d(TAG, "   data = $data")
            Log.i(TAG, "   time = ${getTime()}")

            val bridge = getBridgeFromEventSource(eventSource)
            if (bridge == null) {
                Log.e(TAG, "Unable to find bridge in onEvent()!")
                return
            }

            try {
                val eventJsonArray = JSONArray(data)

                coroutineScope.launch {
                    // process each event
                    for (i in 0 until eventJsonArray.length()) {
                        val eventJsonObj = eventJsonArray.getJSONObject(i)
                        val v2Event = PHv2ResourceServerSentEvent(eventJsonObj)
                        interpretEvent(bridge, v2Event)
                    }
                }
            }
            catch (e: JSONException) {
                Log.e(TAG, "Unable to parse event into json object: $data")
                e.printStackTrace()
                return
            }
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            super.onFailure(eventSource, t, response)

            Log.e(TAG, "EventSourceListener: On Failure():")
            Log.e(TAG, "   response.isSuccessful = ${response?.isSuccessful}")
            Log.e(TAG, "   response.code = ${response?.code}")
            Log.e(TAG, "   response.request = ${response?.request.toString()}")
            Log.e(TAG, "   response.body = ${response?.peekBody(10000)?.string()}")
            Log.e(TAG, "   time = ${getTime()}")

            val bridge = getBridgeFromEventSource(eventSource)
            if (bridge == null) {
                Log.e(TAG, "Unable to get bridge in listener.onFailure()!")
            }
            else {
                _bridgeFlowSet.update {
                    val currentBridgeFlowSet = bridgeFlowSet.value.toMutableSet()
                    currentBridgeFlowSet.remove(bridge)
                    bridge.connected = false         // set connect status
                    currentBridgeFlowSet.add(bridge)
                    currentBridgeFlowSet
                }
            }
            response?.body?.close()
        }
    }


    //-------------------------------------
    //  initializations
    //-------------------------------------

    init {
        properInit()
    }

    /**
     * This exists because kotlin sucks at inits.  Please don't
     * call this function except during initialization.
     *
     * This loads any data that is stored in shared prefs.
     *
     * side effects
     *  - [bridgeFlowSet] will contain data as in the shared prefs
     */
    private fun properInit() {

        // idiot test to make sure this isn't run more than once.
        if (initialized) {
            Log.e(TAG, "Error: tried to initialize more than once!!!")
            return
        }
        initialized = true

        //----------------
        //  1. Load bridge info from long-term storage
        //
        //  2. For each active bridge, get rooms for that bridge
        //     (includes control data for the room)
        //
        //  3. Setup callbacks (events) for the bridges so we can be
        //     updated when anything changes.
        //

        // Needs to run off the main thread
        runBlocking(Dispatchers.IO) {

            // 1. Load bridge ids from prefs
            //
            val bridgeIdsFromPrefs = loadAllBridgeIdsFromPrefs()
            if (bridgeIdsFromPrefs.isNullOrEmpty()) {
                return@runBlocking
            }

            /**  temp holder */
            val workingBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()

            // use these ids to load the ips & tokens from prefs
            bridgeIdsFromPrefs.forEach() { id ->

                // get the IP for this bridge
                val ip = loadBridgeIpFromPrefs(id)

                // get the token for this bridge
                val token = loadBridgeTokenFromPrefs(id)

                val isActive = isBridgeActive(ip, token)
                var name = ""
                if (isActive) {
                    val jsonString = getBridgeDataStrFromApi(ip, token)
                    if (jsonString.isEmpty()) {
                        Log.e(TAG, "Unable to get bridge data (ip = $ip) in properInit()!")
                    }
                    else {
                        val v2Bridge = PHv2ResourceBridge(jsonString)
                        if (v2Bridge.hasData() == false) {
                            Log.e(TAG, "Bridge data empty (ip = $ip) in properInit()!")
                            Log.e(TAG, "   error = ${v2Bridge.getError()}")
                        }
                        else {
                            // finally we can get the name!
                            name = v2Bridge.getName()
                        }
                    }
                }

                val bridge = PhilipsHueBridgeInfo(
                    id = id,
                    labelName = name,
                    ip = ip,
                    token = token,
                    active = isActive,
                    connected = false
                )

                // fixme: this is too early!  The bridgeFlowSet should be done once all the
                //  bridges are completely figured out!
                workingBridgeSet += bridge
            }

            // 2. Load data for each bridge (but only if it's active)
            //    To make sure that the flow works correctly we construct
            //    a brand-new set with all the data in it.
            val newBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
            workingBridgeSet.forEach { bridge ->
                if (isBridgeActive(bridge)) {
                    bridge.active = true

                    // 2a. Find rooms and add them to the bridge data
                    val roomsFromApi = getRoomsFromBridgeApi(bridge)
                    bridge.rooms = convertV2RoomAll(roomsFromApi, bridge)
                }
                else {
                    bridge.active = false
                }

                newBridgeSet.add(bridge)
            }

            // 3. Connect each active bridge so we can receive server-sent events
            newBridgeSet.forEach { bridge ->
                if (bridge.active) {
                    connectToBridge(bridge)
                }
            }

            // update the flow for all the bridges
            _bridgeFlowSet.update { newBridgeSet }  // stateflow reflects changes
        }
    }


    //-------------------------------------
    //  create
    //-------------------------------------

    /**
     * This creates a new bridge and adds it the permanent bridge
     * data.
     *
     * preconditions
     *      The newBridge must be active and contain a token!!!
     *
     * @param   newBridge       A nearly fully-loaded bridge info class
     *                          (the id will be set BY THIS FUNCTION).
     *                          The data is NOT checked for accuracy.
     */
    suspend fun addBridge(
        newBridge: PhilipsHueNewBridge
    ) = withContext(Dispatchers.IO) {

        // grab the id for this bridge
        val responseStr = getBridgeDataStrFromApi(
            bridgeIp = newBridge.ip,
            token = newBridge.token
        )
        val v2BridgeResponse = PHv2ResourceBridge(responseStr)
        if (v2BridgeResponse.hasData() == false) {
            Log.e(TAG, "Unable to get info about bridge in addBridge--aborting!")
            Log.e(TAG, "   error msg: ${v2BridgeResponse.getError()}")
            return@withContext
        }
        val id = v2BridgeResponse.getId()

        val bridgeToAdd = PhilipsHueBridgeInfo(
            id = id,
            labelName = newBridge.labelName,
            ip = newBridge.ip,
            token = newBridge.token,
            active = newBridge.active,
            rooms = mutableSetOf(),
            connected = false
        )
        _bridgeFlowSet.value += bridgeToAdd

        // update long-term data
        saveBridge(bridgeToAdd)
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
    fun getAllBridges() : Set<PhilipsHueBridgeInfo> {
        return _bridgeFlowSet.value
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
        _bridgeFlowSet.value.forEach {
            ids.add(it.id)
        }
        return ids
    }

    /**
     * Returns the bridge with the given id from the current
     * Set of bridges.  If the id is bogus this returns null.
     * Great way to check to see if a bridge exists.
     *
     * preconditions
     *      bridgeFlowSet   Needs to be setup and working with all correct data.
     *
     * @return  The [PhilipsHueBridgeInfo] of the bridge with the
     *          given id.
     *          Null if id is not found.
     */
    fun getBridge(bridgeId: String) : PhilipsHueBridgeInfo? {
        val bridge = bridgeFlowSet.value.find { bridge ->
            bridge.id == bridgeId
        }
        Log.d(TAG, "getBridge($bridgeId) " + if (bridge == null) "failed" else "successful")
        return bridge
    }


    /**
     * Gets all the information about a particular bridge.  This is straight
     * from the bridge itself.  It can easily be parsed with [PHv2ResourceBridge].
     *
     * @return      The JSON string that was returned in the body of the api
     *              call to the bridge.
     *              Returns empty string if the bridge doesn't exist, token
     *              is not recognized, or some other error.
     */
    suspend fun getBridgeDataStrFromApi(bridgeIp: String, token: String) : String {

        val fullAddress = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_BRIDGE
        )

        val response = synchronousGet(
            url = fullAddress,
            header = Pair(HEADER_TOKEN_KEY, token),
            trustAll = true     // fixme: when we start higher security
        )

        if (response.isSuccessful) {
            return response.body
        }
        return EMPTY_STRING
    }

    /**
     * Gets a Set of all the lights that the bridge knows about.
     * Returns an empty set on error or no lights.
     *
     * fixme: this needs updating to v2!!!
     */
    @Deprecated("needs updating to v2")
    suspend fun getLightsFromBridgeApi(bridgeId: String) : Set<PhilipsHueLightInfo> {
        val lights = mutableSetOf<PhilipsHueLightInfo>()

        val bridge = getBridge(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "trying to find the lights with bad id in getLights($bridgeId). aborting!")
            return lights
        }

        val fullAddress = createFullAddress(
            ip = bridge.ip,
            suffix = SUFFIX_API + bridge.token + SUFFIX_GET_LIGHTS
        )

        val response = synchronousPost(
            url = fullAddress,
            bodyStr = generateGetTokenBody(),
            trustAll = true
        )

        TODO()
    }

    /**
     * Finds all the rooms with the associated bridge.
     *
     * @return      Set of all rooms found with this bridge
     */
    private suspend fun getRoomsFromBridgeApi(bridge: PhilipsHueBridgeInfo) : PHv2ResourceRoomsAll {

        val fullAddress = createFullAddress(
            prefix = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
            ip = bridge.ip,
            suffix = SUFFIX_GET_ROOMS
        )

        // get info from bridge
        val response = synchronousGet(
            url = fullAddress,
            header = Pair(HEADER_TOKEN_KEY, bridge.token),
            trustAll = true
        )

        Log.d(TAG, "getting response for url: $fullAddress")
        val roomsData = PHv2ResourceRoomsAll(response.body)
        Log.d(TAG, "got rooms: $roomsData")

        return roomsData
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
     * Since this does network communication, this needs to be called
     * off the main thread.
     *
     * @return      True - successfully removed bridge.
     *              False - bridge wasn't found.
     */
    fun deleteBridge(bridgeId: String) : Boolean {

        val bridgeToDelete = getBridge(bridgeId) ?: return false

        // 1) todo Tell the bridge to remove this app's username (token)

        // 2) Remove bridge data from our permanent storage
        if (removeBridgeTokenPrefs(bridgeId) == false) {
            Log.e(TAG, "Problem removing token for bridgeId = $bridgeId")
            return false
        }

        if (removeBridgeIP(bridgeId) == false) {
            Log.e(TAG, "Problem removing IP from bridgeId = $bridgeId")
            return false
        }

        if (removeBridgeId(bridgeId) == false) {
            Log.e(TAG, "Problem removing id from bridgeId = $bridgeId")
            return false
        }

        // 3) Remove bridge data from our temp storage.
        //  As this is a complicated data structure, we need to use a
        //  (kind of) complicated remove.
        var tmpBridges = setOf<PhilipsHueBridgeInfo>()
        var removed = false
        _bridgeFlowSet.value.forEach { bridge ->
            if (bridge.id != bridgeId) {
                tmpBridges += bridge
            }
            else {
                removed = true
            }
        }
        if (removed == false) {
            Log.e(TAG,"Unable to remove bridge $bridgeToDelete at the final stage of deleteBridge(bridgeId = $bridgeId)!")
            return false
        }

        _bridgeFlowSet.value = tmpBridges
        return true
    }

    /**
     * This is the nuclear option.  Removes all the bridges for all time.
     * This is essentially a reset of the app.
     *
     * @return      True - all bridges were removed (even if there were none).
     *              False - error condition (although I don't know what).
     */
    suspend fun deleteAllBridges() : Boolean {

        var error = false
        _bridgeFlowSet.value.forEach { bridge ->
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
     * Working completely by side effect, this analyses the result of a server-
     * sent event and changes the contents of the bridge data appropriately.
     *
     * @param   eventBridge     The bridge that the event relates to
     *
     * @param   event           Data structure describing the event.
     */
    private suspend fun interpretEvent(eventBridge: PhilipsHueBridgeInfo, event: PHv2ResourceServerSentEvent) {

        Log.d(TAG, "interpretEvent()  eventBridge = ${eventBridge.id}")
        Log.d(TAG, "interpretEvent()  event.type = ${event.type}")
        Log.d(TAG, "interpretEvent()  event.eventId = ${event.eventId}")
        Log.d(TAG, "interpretEvent()  event.data = ${event.data}")

        // if this is an error, we have nothing to do
        if (event.type == "error") {
            Log.w(TAG, "interpretEvent() - can't do anything with an error, skipping!")
            return
        }

        // Boo, this no longer works!!! The viewmodel only registers the first change.  sigh!!!
        val newBridgeList = mutableListOf<PhilipsHueBridgeInfo>()
        val newBridgeSetHash = System.identityHashCode(newBridgeList)
        Log.d(TAG, "-->creating newBridgeSet hash = $newBridgeSetHash")

        // rebuild the bridge list
        bridgeFlowSet.value.forEach { bridge ->
            // Is this the bridge in question?
            if (bridge.id == eventBridge.id) {
                // Yes, this is the bridge.  What kind of event was it?
                when (event.type) {
                    "update" -> {
                        // go through each datum for this event
                        event.data.forEach { eventDatum ->
                            // what kind of device changed?
                            when (eventDatum.type) {
                                "light" -> {
                                    val light = findLightFromId(eventDatum.owner!!.rid, bridge)
                                    if (light == null) {
                                        Log.e(TAG, "unable to find changed light in interpretEvent()!")
                                    }
                                    else {
                                        // yes, we have a light!
                                        if (eventDatum.on != null) {
                                            light.state.on = eventDatum.on.on
                                        }
                                        if (eventDatum.dimming != null) {
                                            light.state.bri = eventDatum.dimming.brightness
                                        }

                                        // now fix the room.
                                        val room = findRoomFromLight(light, bridge)
                                        if (room != null) {
                                            // There's just one light group per room. Get its info and
                                            // reset the room's data.
                                            val lightGroup = getGroupedLightFromApi(room.groupedLightServices[0].rid, bridge)
                                            room.brightness = lightGroup.data[0].dimming.brightness
                                            room.on = lightGroup.data[0].on.on
                                        }
                                    }
                                }

                                "grouped_light" -> {
                                    Log.e(TAG, "Unhandled grouped_light!!!")
                                }

                                "room" -> {
                                    Log.e(TAG, "Unhandled room event. Crashing now.")
                                    TODO()
                                }

                                "update" -> {
                                    Log.e(TAG, "Unhandled update event. Crashing now.")
                                    TODO()
                                }

                                else -> {
                                    Log.e(TAG, "Unhandled event type: ${event.type}. Crashing now.")
                                    TODO()
                                }
                            }

                        }
                    }
                    "add" -> {
                        TODO()
                    }
                    "delete" -> {
                        TODO()
                    }
                    else -> {
                        Log.e(TAG, "Unknown event type!!! Aborting!")
                        return
                    }
                }
//                bridge.labelName = "slippery sam"
            }

            newBridgeList.add(bridge)
        }
        Log.d(TAG, "-->Setting bridgeFlowSet to newBridgeSet (hash = $newBridgeSetHash). Viewmodel should be updating!")
        _bridgeFlowSet.update { newBridgeList.toSet() }



    }

    /**
     * Another helper.  This finds the room that a light resides in.
     * Returns null if not found.
     */
    private fun findRoomFromLight(
        light: PhilipsHueLightInfo,
        bridge: PhilipsHueBridgeInfo
    ) : PhilipsHueRoomInfo? {
        bridge.rooms.forEach { room ->
            room.lights.forEach { maybeThisLight ->
                if (maybeThisLight.lightId == light.lightId) {
                    Log.d(TAG, "found the room that holds light (id = ${light.lightId}")
                    return room
                }
            }
        }
        Log.d(TAG, "Could not find the room that holds light (id = ${light.lightId}. Sorry.")
        return null
    }


    /**
     * Goes through all the lights in this bridge and returns the one that
     * matches the given id.  It also checks device ids FOR lights (as they
     * are sometimes used interchangeably).
     *
     * Returns null if not found.
     */
    private fun findLightFromId(id: String, bridge: PhilipsHueBridgeInfo) : PhilipsHueLightInfo? {

        // go through all the rooms
        bridge.rooms.forEach { room ->
            room.lights.forEach { light ->
                if (light.lightId == id) {
                    Log.d(TAG, "Found the light in findLightFromId(id = $id), yay!")
                    return light
                }
                if (light.deviceId == id) {
                    Log.d(TAG, "Found the light from its deviceId in findLightFromId(id = $id), whew!")
                    return light
                }
            }
        }
        Log.d(TAG, "did not find light (id = $id) in findLightFromId().  Sigh.")
        return null
    }


    /**
     * Grabs the bridge that uses this given eventSource.  If none can be found, this
     * returns null.
     */
    private fun getBridgeFromEventSource(eventSource: EventSource) : PhilipsHueBridgeInfo? {
        // The id of the bridge is conveniently stashed in the tag.
        val bridgeId = eventSource.request().tag() as String
        return getBridge(bridgeId)
    }

    /**
     * Checks to see if the given bridge is active.
     */
    @SuppressWarnings("WeakerAccess")
    suspend fun isBridgeActive(bridge: PhilipsHueBridgeInfo) : Boolean = withContext(Dispatchers.IO) {

        if (doesBridgeRespondToIp(bridge) && doesBridgeAcceptToken(bridge)) {
            return@withContext true
        }
        return@withContext false
    }

    /**
     * Alternate version of [isBridgeActive].  This version only needs
     * the ip and token (not the whole bridge).
     */
    suspend fun isBridgeActive(
        bridgeIp: String,
        bridgeToken: String
    ) : Boolean = withContext(Dispatchers.IO) {
        if ((doesBridgeRespondToIp(ip = bridgeIp)) &&
            (doesBridgeAcceptToken(bridgeIp, bridgeToken))) {
            return@withContext true
        }

        return@withContext false
    }


    /**
     * Tests to see if a bridge is at the given ip.
     *
     * This just tests to see if the basic debug screen appears.
     * The url is  http://<ip>/debug/clip.html
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

            val myResponse = synchronousGet(fullIp)

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
     * Alternate version that takes a bridge instead of an IP.
     */
    @SuppressWarnings("WeakerAccess")
    suspend fun doesBridgeRespondToIp(bridge: PhilipsHueBridgeInfo) : Boolean {
        return doesBridgeRespondToIp(bridge.ip)
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
     *
     * NOTE
     *      This works for both version 1 and version 2 of the API.
     */
    suspend fun registerAppToBridge(
        bridgeIp: String
    ) : Pair<String, GetBridgeTokenErrorEnum> {

        val fullAddress = createFullAddress(ip = bridgeIp, suffix = "/api/")
        Log.d(TAG, "registerAppToBridge() - fullAddress = $fullAddress")

        val response = synchronousPost(
            url = fullAddress,
            bodyStr = generateGetTokenBody(),
            trustAll = true     // fixme when we have full security going
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


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // private data
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
     */
    @Deprecated("no longer needed: use v2 id instead")
    private fun generateNewId() : String {

        // strategy: go through the id (as numbers) until we don't
        // match an existing id.  Not very fast, O(n^2), sigh.

        // create a Set of bridgeIds (in number form)
        val bridgeIds = mutableIntSetOf()
        _bridgeFlowSet.value.forEach { bridge ->
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
     * @param   token   The token to insert in the command.  This authenticates the
     *                  caller to the bridge.  Defaults to empty string.
     *
     * @param   prefix  If the prefix is not contained in the ip, then this holds
     *                  it.  Generally this will be "https://" or "http://".
     *
     * @param   suffix  Anything that goes AFTER the ip.  This is the directory
     *                  or parameters or whatever.  Remember that this should start
     *                  with a backslash ('/').
     */
    private fun createFullAddress(
        ip: String,
        token: String = "",
        prefix: String = PHILIPS_HUE_BRIDGE_URL_SECURE_PREFIX,
//        prefix: String = PHILIPS_HUE_BRIDGE_URL_OPEN_PREFIX,
        suffix: String
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

    /**
     * Use this to generate the body for when trying to get a new
     * token (username) from the bridge.
     *
     * @return  The json we're constructing will look something like this:
     *      {"devicetype":"appName#instanceName", "generateclientkey":true}
     *
     * @param   appName         The name of the app (defaults to Pauls App)
     *
     * @param   instanceName    The name of the instance of this app.
     *                          Since mulitple copies of this app may be
     *                          accessing the same bridge, this needs to be
     *                          a unique number.  Default should generate a
     *                          very good one.
     */
    @SuppressLint("HardwareIds")
    private fun generateGetTokenBody(
        appName: String? = null,
        instanceName: String? = null
    ) : String {
        val name = appName ?: ctx.getString(R.string.app_name)

        // This tries to use the ANDROID_ID.  If it's null, then I just
        // throw in a JellyBean (should only happen on jelly bean devices, hehe).
        val instance = instanceName
            ?: Secure.getString(ctx.contentResolver, Secure.ANDROID_ID)
            ?: "jellybean"
        return """{"devicetype": "$name#$instance", "generateclientkey":true}"""
    }

    //~~~~~~~~~~~
    //  read
    //~~~~~~~~~~~

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
        val bridgeResponseData = Gson().fromJson(bodyStr, PHBridgePostTokenResponse::class.java)
            ?: return null      // just return null if bridgeResponse is null

        if (bridgeResponseData[0].success != null) {
            // looks like there was a success
            val success = bridgeResponseData[0].success!!
            val token = success.username
            val clientKey = success.clientkey
            Log.d(TAG, "getTokenFromBodyStr() successful. token = $token, clientKey = $clientKey")

            if (token.isEmpty() || clientKey.isEmpty()) {
                Log.e(TAG, "getTokenFromBodyStr()--thought we were successful, but token or clientKey is empty!")
                return null
            }
            return token
        }
        else {
            Log.e(TAG, "Error in getTokenFromBodyStr()! Unable to parse bodyStr = $bodyStr")
            return null
        }
    }

    /**
     * Finds out if a bridge currently is active and responds to its token.
     *
     * @param       bridge      The bridge in question.  Does not check to see
     *                          if everything is right.
     *
     * @param       token       The token (username) to test.
     *
     * @return      True if the bridge is working and responds positively.
     *              False otherwise.
     */
    suspend fun doesBridgeAcceptToken(bridgeIp: String, token: String) :
            Boolean = withContext(Dispatchers.IO) {

        // try getting the bridge's general info to see if the token works
        val fullAddress = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_BRIDGE,
        )

        // Just want a response, nothing fancy
        val response = synchronousGet(
            fullAddress,
            header = Pair(HEADER_TOKEN_KEY, token),
            trustAll = true
        )
        return@withContext response.isSuccessful

    }

    /**
     * Alternate version of [doesBridgeAcceptToken]
     */
    suspend fun doesBridgeAcceptToken(bridge: PhilipsHueBridgeInfo) :
            Boolean = withContext(Dispatchers.IO) {
        return@withContext doesBridgeAcceptToken(bridge.ip, bridge.token)
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
        _bridgeFlowSet.value.forEach { bridge ->
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
    private suspend fun saveBridge(bridge: PhilipsHueBridgeInfo) :
            Boolean = withContext(Dispatchers.IO) {

        if (saveBridgeId(bridge.id) == false) {
            Log.e(TAG, "Problem saving id in saveBridge($bridge) - aborting!")
            return@withContext false
        }

        if (saveBridgeIp(bridge.id, bridge.ip, true) == false) {
            Log.e(TAG, "Can't save the IP of the bridge in saveBridge($bridge) - aborting!")
            return@withContext false
        }

        if (saveBridgeToken(bridge.id, bridge.token, true) == false) {
            Log.e(TAG, "Can't save the token of the bridge in saveBridge($bridge) - aborting!")
            return@withContext false
        }

        return@withContext true
    }

    //~~~~~~~~~~~
    //  delete
    //~~~~~~~~~~~

    /**
     * Removes the given token from our permanent storage.
     * If the token does not already exist, then nothing is done.
     *
     * preconditions
     *      - The bridge must already exist
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
    private fun removeBridgeTokenPrefs(
        bridgeId: String,
        synchronize: Boolean = false
    ) : Boolean {

        if (getBridge(bridgeId) == null) {
            Log.e(TAG, "unable to find bridge in removeBridgeToken(bridgeId = $bridgeId)")
            return false
        }

        val tokenKey = assembleTokenKey(bridgeId)
        deletePref(ctx, tokenKey, synchronize)
        return true
    }

    /**
     * Removes the given IP from our permanent storage.
     * If the bridge does not exist, then nothing is done.
     *
     * Note:    This does NOT remove the bridge IP from our short-term
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
    private fun removeBridgeIP(
        bridgeId: String,
        synchronize: Boolean = false
    ) : Boolean {

        if (getBridge(bridgeId) == null) {
            Log.e(TAG, "unable to find bridge in removeBridgeIP(bridgeId = $bridgeId)")
            return false
        }

        val ipKey = assembleIpKey(bridgeId)
        deletePref(ctx, ipKey, synchronize)
        return true
    }

    /**
     * Removes the bridge id from long-term storage (prefs).
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
    private fun removeBridgeId(
        bridgeId: String,
        synchronize: Boolean = false
    ) : Boolean {

        if (getBridge(bridgeId) == null) {
            Log.e(TAG, "unable to find bridge in removeBridgeId(bridgeId = $bridgeId)")
            return false
        }

        // Get the set of ids.  We'll have to change it
        // and save that changed set.
        val idSet = mutableSetOf<String>()
        _bridgeFlowSet.value.forEach { bridge ->
            if (bridge.id != bridgeId) {
                // don't record THIS id!
                idSet.add(bridge.id)
            }
        }

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

    //~~~~~~~~~~~
    //  util
    //~~~~~~~~~~~

    /**
     * Connects the given bridge to this app, enabling this app to
     * receive updates on changes to the Philips Hue world.
     */
    fun connectToBridge(bridge: PhilipsHueBridgeInfo) {

        if (bridge.connected) {
            Log.e(TAG, "Trying to connect to a bridge that's already connected! bridge.id = ${bridge.id}")
            return
        }

        val request = Request.Builder()
            .url("https://${bridge.ip}/eventstream/clip/v2")
            .header("Accept", "text/event-stream")
            .addHeader("hue-application-key", bridge.token)
            .tag(bridge.id)     // identifies this request (within the EventSource)
            .build()

        // not found. add it using the default listener
        myEventSource = EventSources.createFactory(getAllTrustingSseClient())
            .newEventSource(
                request = request,
                listener = defaultListener
            )

        // note: the connection is not complete yet; we just made an attempt at connecting.
        // we'll know that the connection is successful in the onOpen() call in whatever
        // is listening to server-sent events.
    }

    /**
     * Stop receiving updates about the Philps Hue IoT for this bridge.
     * If the bridge is not found, nothing is done.
     */
    fun disconnectFromBridge(bridge: PhilipsHueBridgeInfo) {
        Log.d(TAG, "disconnect() called on bridge ${bridge.ip}")

        // find the right eventsource
//        for (i in 0 until eventSourceList.size) {
//            val eventSource = eventSourceList[i]
//            if (eventSource.first == bridge.id) {
//                eventSource.second.cancel()
//                return
//            }
//        }
//        Log.e(TAG, "Unable to find bridge in disconnectFromBridge(bridgeId = ${bridge.id}")

        myEventSource.cancel()

    }

    /**
     * Converts data structures: from [PHv2ResourceRoomsAll] to a Set
     * of [PhilipsHueRoomInfo].  This involves getting light information
     * and some other stuff that has to hit the bridge, so it needs
     * to be off the main thread.
     */
    private suspend fun convertRoomsData(
        roomsFromApi: PHv2ResourceRoomsAll,
        bridge: PhilipsHueBridgeInfo
    ) : Set<PhilipsHueRoomInfo> = withContext(Dispatchers.IO) {

        val newRooms = mutableSetOf<PhilipsHueRoomInfo>()
        roomsFromApi.data.forEach { apiRoom ->

//            val id = apiRoom.id
//            adsfadf
//
//            val room = PhilipsHueRoomInfo(
//                id = ,
//                on = ,
//                brightness = ,
//                lights = ,
//            )
        }

        return@withContext newRooms
    }

    /**
     * Polls a single bridge, updating info if necessary.
     *
     * side effects
     *  Bridge data will be modified if anything has changed.
     *
     * @return  True if anything has changed (that this detects!).
     *
     * Assumptions:
     *      The id and IP for the bridge does not changed.  If so, then
     *      the user needs to delete this entry for the bridge and
     *      enter it in as if it's a new bridge
     */
    private suspend fun pollBridge(bridge: PhilipsHueBridgeInfo) : Boolean {

        TODO()
//
//        var changed = false
//
//        // check to see if it's active first (may need to change active status)
//        if (doesBridgeRespondToIp(bridge)) {
//            if (bridge.active == true) {
//                changed = true
//            }
//            bridge.active = true
//        }
//        else {
//            if (bridge.active == true) {
//                changed = true
//            }
//            bridge.active = false
//            return changed
//        }
//
//        // test the token
//        if (doesBridgeAcceptToken(bridge) == false) {
//            // problem with the token.  Either there wasn't one
//            // or the bridge was reset.  I'm calling this a change
//            // regardless
//            bridge.token = ""
//            changed = true
//            return changed
//        }
//
//
//        // fixme:  this isn't that useful.
//        // Lastly, the catch-all: the body.  A lot of stuff goes on here,
//        // and if anything changes along here, we'll notice it.
//
////        val bridgeJsonDataStr = getBridgeDataStrFromApi(bridge)
////        if (bridgeJsonDataStr.isEmpty()) {
////            Log.e(TAG, "getBridgeDataFromApi(id = ${bridge.id} yielded an empty result!!!")
////            if (bridge.body.isNotEmpty()) {
////                changed = true
////                bridge.body = ""
////            }
////            // nothing more to do
////            return changed
////        }
//
//        // fixme: the bridgeInfo isn't that useful
////        val bridgeInfo = PHv2ResourceBridge(bridgeJsonDataStr)
////
////        if (bridge.body != bridgeJsonDataStr) {
////            changed = true
////            bridge.body = bridgeJsonDataStr
////        }
//
//
//        // get the list of rooms that the bridge knows about
//        val roomsBridgeInfo = getRoomsFromBridgeApi(bridge.id)
//        if (bridge.rooms.size != roomsBridgeInfo.size()) {
//            changed = true
//        }
//
//        // convert to a Set of rooms
//        val newRoomSet = convertV2RoomAll(phV2Rooms = roomsBridgeInfo, bridge = bridge)
//
//        // See if there are any differences in this set and our current set of rooms
//
////        compareRooms(roomsBridgeInfo)
//
//        return changed
    }

    /**
     * Converts a [PHv2ResourceRoomsAll] into a set of [PhilipsHueRoomInfo].
     *
     * @param   phV2Rooms       The data structure returned from the bridge about
     *                          its rooms.
     *
     * @param   bridge          Info about the bridge
     *
     * @return  A Set of information about the rooms translated from the input.
     */
    private suspend fun convertV2RoomAll(
        phV2Rooms: PHv2ResourceRoomsAll,
        bridge: PhilipsHueBridgeInfo
    ) : MutableSet<PhilipsHueRoomInfo> = withContext(Dispatchers.IO) {

        val newRoomSet = mutableSetOf<PhilipsHueRoomInfo>()

        // check for the case that there is no rooms
        if (phV2Rooms.data.isEmpty()) {
            return@withContext newRoomSet
        }

        phV2Rooms.data.forEach { v2Room ->
            // check correct type error (should never happen)
            if (v2Room.type != ROOM) {
                Log.e(TAG, "Error!  Room (id = ${v2Room.id} has wrong type! Type = ${v2Room.type}, but should be 'room'!")
                return@forEach      // this is the equivalent of continue (will skip this iteration)
            }

            val v2RoomIndividual = getRoomIndividualFromApi(v2Room.id, bridge)
            if (v2RoomIndividual.errors.isNotEmpty()) {
                Log.e(TAG, "convertV2RoomAll(): error getting individual room!")
            }
            else {
                val room = convertPHv2RoomIndividual(v2RoomIndividual, bridge)
                if (room != null) {
                    newRoomSet.add(room)
                }
            }
        }

        return@withContext newRoomSet
    }

    /**
     * Takes a [PHv2RoomIndividual] and converts it to a
     * [PhilipsHueRoomInfo].
     *
     * If [v2RoomIndividual] is in error state (no data), null is returned.
     */
    private suspend fun convertPHv2RoomIndividual(
        v2RoomIndividual: PHv2RoomIndividual,
        bridge: PhilipsHueBridgeInfo
    ) : PhilipsHueRoomInfo? = withContext(Dispatchers.IO) {

        // first check to make sure original data is valid.
        if (v2RoomIndividual.data.isEmpty()) {
            return@withContext null
        }

        return@withContext convertPHv2Room(v2RoomIndividual.data[0], bridge)
    }

    /**
     * Returns the first grouped light found in the given room.
     * As a room, it shouldn't have more than one group.
     *
     * @param   v2Room      This is a [PHv2Room].  That is, it's
     *                      taken from the data section of the
     *                      class that's actually returned from the
     *                      API.
     *
     * @return  The [PHv2GroupedLight] that's within.  Null will
     *          be returned if no grouped lights are found.
     */
    private suspend fun getGroupedLightFromRoom(
        v2Room: PHv2Room,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2GroupedLight? = withContext(Dispatchers.IO) {
        v2Room.children.forEach { child ->
            if (child.rtype == RTYPE_GROUP_LIGHT) {
                val groupedLightIndividual = getGroupedLightFromApi(
                    child.rid, bridge
                )

                // quick sanity check
                if (groupedLightIndividual.data.isNotEmpty()) {
                    val groupedLight = groupedLightIndividual.data[0]
                    return@withContext groupedLight
                }
            }
        }
        // not found
        return@withContext null
    }

    /**
     * Converts a [PHv2Room] datum into a [PhilipsHueRoomInfo]
     * instance.  This may require gathering more info from the
     * bridge, hence the suspend.
     *
     * @param   bridge      The bridge that this is attached to.
     *                      It better be active!
     */
    private suspend fun convertPHv2Room(
        v2Room: PHv2Room,
        bridge: PhilipsHueBridgeInfo
    ) : PhilipsHueRoomInfo = withContext(Dispatchers.IO) {

        // Do we have any grouped_lights?  This is a great short-cut.
        val groupedLightServices = mutableListOf<PHv2ItemInArray>()
        v2Room.services.forEach { service ->
            if (service.rtype == RTYPE_GROUP_LIGHT) {
                Log.d(TAG, "convertPHv2Room() found a grouped_light, yay! (id = ${service.rid})")
                groupedLightServices.add(service)
            }
        }

        if (groupedLightServices.isNotEmpty()) {
            // Yep, we have some light groups.  But a room should have ONLY one.
            if (groupedLightServices.size > 1) {
                Log.e(TAG, "too many grouped light services in convertPHv2Room!  Aborting!")
                return@withContext PhilipsHueRoomInfo(
                    id = "-1",
                    name = EMPTY_STRING,
                    lights = mutableSetOf(),
                    groupedLightServices = groupedLightServices
                )
            }

            // The grouped_light has info on on/off and brightness
            val groupedLight = getGroupedLightFromApi(groupedLightServices[0].rid, bridge)
            var onOff = false
            var brightness = 0
            if (groupedLight.errors.isEmpty()) {
                onOff = groupedLight.data[0].on.on
                brightness = groupedLight.data[0].dimming.brightness
            }

            // get the lights
            val regularLightSet = mutableSetOf<PhilipsHueLightInfo>()
            v2Room.children.forEach { child ->
                if (child.rtype == RTYPE_DEVICE) {
                    // is this device a light?  It's a light iff one of its services is rtype = "light".
                    val device = getDeviceIndividualFromApi(child.rid, bridge)
                    if (device.data.isNotEmpty() && (device.data[0].type == DEVICE)) {
                        // yes, it's a device (probably a light)!  To make sure, search its services.
                        device.data[0].services.forEach { service ->
                            if (service.rtype == RTYPE_LIGHT) {
                                val v2light = getLightInfoFromApi(service.rid, bridge)
                                if (v2light != null) {
                                    val regularLight = PhilipsHueLightInfo(
                                        lightId = v2light.data[0].id,
                                        deviceId = v2light.data[0].owner.rid,
                                        name = v2light.data[0].metadata.name,
                                        state = PhilipsHueLightState(
                                            on = v2light.data[0].on.on,
                                            bri = v2light.data[0].dimming.brightness
                                        ),
                                        type = v2light.data[0].type
                                    )
                                    regularLightSet.add(regularLight)
                                }
                                else {
                                    Log.e(TAG, "problem finding light in convertPHv2Room()!!!")
                                }
                            }
                        }
                    }
                }
            }

            // everything worked out! return that data
            val newRoom = PhilipsHueRoomInfo(
                id = v2Room.id,
                name = v2Room.metadata.name,
                on = onOff,
                brightness = brightness,
                lights = regularLightSet,
                groupedLightServices = groupedLightServices
            )
            return@withContext newRoom
        }

        // at this point, there's a room, but no lights
        val newRoom = PhilipsHueRoomInfo(
            id = v2Room.id,
            name = v2Room.metadata.name,
            on = false,
            brightness = 0,
            lights = mutableSetOf(),
            groupedLightServices = groupedLightServices
        )

        return@withContext newRoom
    }

    /**
     * Helper function.  From a list of [PHv2ResourceIdentifier]s, this finds the
     * first that is of type [RTYPE_LIGHT], finds it's full info, and returns its
     * [PhilipsHueRoomInfo] form.
     *
     * @return  Null if a light can't be found or there was some sort of error.
     */
    private suspend fun getLightInfoFromServiceList(
        serviceList: List<PHv2ResourceIdentifier>,
        bridge: PhilipsHueBridgeInfo
    ) : PhilipsHueLightInfo? {

        serviceList.forEach { service ->
            if (service.rtype == RTYPE_LIGHT) {
                // yep found it!
                val v2ApiLight = getLightInfoFromApi(service.rid, bridge)
                if (v2ApiLight == null) {
                    Log.w(TAG, "Problem getting light in getLightInfoFromServiceList()!  rid = ${service.rid}")
                    return null
                }

                if (v2ApiLight.errors.isNotEmpty()) {
                    Log.w(TAG, "Error occurred getting light in getLightInfoFromServiceList()!")
                    Log.w(TAG, "   error = ${v2ApiLight.errors[0]}")
                }

                // convert to our PhilipsHueLightInfo
                return PhilipsHueLightInfo(
                    lightId = v2ApiLight.data[0].id,
                    deviceId = v2ApiLight.data[0].owner.rid,
                    name = v2ApiLight.data[0].metadata.name,
                    state = PhilipsHueLightState(
                        on = v2ApiLight.data[0].on.on,
                        bri = v2ApiLight.data[0].dimming.brightness
                    ),
                    type = v2ApiLight.data[0].type
                )
            }
        }
        // nothing found
        return null
    }

    /**
     * Takes a [PHv2ResourceLightsAll] and converts it to a Set of
     * [PhilipsHueLightInfo].  If there are errors, the set will
     * be empty.
     */
    private fun convertV2ResourceLightsAllToLightSet(
        v2ResourceLight: PHv2ResourceLightsAll,
    ) : Set<PhilipsHueLightInfo> {

        val newLightSet = mutableSetOf<PhilipsHueLightInfo>()

        // check for errors
        if (v2ResourceLight.errors.isNotEmpty()) {
            // errors!
            Log.e(TAG, "errors found in convertV2ResourceLightToLightSet()!")
            Log.e(TAG, "   error = ${v2ResourceLight.errors[0].description}")
            return newLightSet
        }

        if (v2ResourceLight.data.isEmpty()) {
            // nothing here!
            Log.e(TAG, "no data found in convertV2ResourceLightToLightSet()!")
            return newLightSet
        }

        // Loop through all the data.  For each light found,
        // add it to our set.
        v2ResourceLight.data.forEach { data ->
            if (data.type == LIGHT) {
                newLightSet.add(convertV2Light(data))
            }
        }
        return newLightSet
    }

    /**
     * Converts from a [PHv2Light] to a [PhilipsHueLightInfo].
     */
    private fun convertV2Light(
        v2Light: PHv2Light
    ) : PhilipsHueLightInfo {
        val state = PhilipsHueLightState(
            on = v2Light.on.on,
            bri = v2Light.dimming.brightness,
        )

        val light = PhilipsHueLightInfo(
            lightId = v2Light.id,
            deviceId = v2Light.owner.rid,
            name = v2Light.metadata.name,
            state = state,
            type = v2Light.type,
        )

        return light
    }

    /**
     * Given the id (v2) of a room, this gets the individual info
     * from the bridge about that room.
     *
     * Note that if there's an error, then that info will also be in
     * the data that's returned (in the error json array).
     */
    private suspend fun getRoomIndividualFromApi(
        roomId: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2RoomIndividual = withContext(Dispatchers.IO) {

        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_ROOMS/$roomId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridge.token),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return@withContext PHv2RoomIndividual(response.body)
        }
        else {
            Log.e(TAG, "unable to get device from bridge (id = ${bridge.id})!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2RoomIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    /**
     * Gets the [PHv2ResourceBridge] data from a bridge.  According to
     * the docs, this is IDENTICAL to getting a bridge with the bridge id.
     * Makes sense as a bridge can only know about itself.
     *
     * This call is useful when you don't much info about the bridge yet.
     * You'll get that info here!
     */
    suspend fun getBridgesAllFromApi(
        bridgeIp: String,
        token: String
    ) : PHv2ResourceBridge = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridgeIp,
            suffix = SUFFIX_GET_BRIDGE
        )
        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, token),
            trustAll = true
        )

        if (response.isSuccessful) {
            return@withContext PHv2ResourceBridge(response.body)
        }
        else {
            Log.e(TAG, "unable to get bridge info (ip = ${bridgeIp}!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2ResourceBridge(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    /**
     * Given a device's id (or RID), this will retrieve the data from
     * the bridge.  Essentially just sets up the API call for you.
     *
     * If there's an error, the error portion is filled in.
     */
    private suspend fun getDeviceIndividualFromApi(
        deviceRid: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2ResourceDeviceIndividual = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_DEVICE/$deviceRid"
        )
        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridge.token),
            trustAll = true     // fixme: change when using secure stuff
        )

        if (response.isSuccessful) {
            // We got a valid response.  Parse the body into our data structure
            return@withContext PHv2ResourceDeviceIndividual(response.body)
        }
        else {
            Log.e(TAG, "unable to get device from bridge (id = ${bridge.id}!")
            Log.e(TAG, "   error code = ${response.code}, error message = ${response.message}")
            return@withContext PHv2ResourceDeviceIndividual(
                errors = listOf(PHv2Error(description = response.message))
            )
        }
    }

    /**
     * Given an id for a light group, this goes to the bridge and
     * gets all the info it can about it.
     *
     * note
     *      AFAIK, grouped_lights doesn't know about the lights that it controls.  It's a kind
     *      of setting for a bunch of lights.  The room (or zone?) applies this to all
     *      the lights that it controls.
     *
     * @return  [PHv2GroupedLightIndividual] instance with all the info on this light group.
     */
    private suspend fun getGroupedLightFromApi(
        groupId: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2GroupedLightIndividual = withContext(Dispatchers.IO) {
        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupId"
        )

        val response = synchronousGet(
            url = url,
            header = Pair(HEADER_TOKEN_KEY, bridge.token),
            trustAll = true
        )

        if (response.isSuccessful == false) {
            Log.e(TAG, "unsuccessful attempt at getting grouped_lights!  groupId = $groupId, bridgeId = ${bridge.id}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            // returning empty object
            return@withContext PHv2GroupedLightIndividual(JSONObject())
        }

        val group = PHv2GroupedLightIndividual(JSONObject(response.body))
        return@withContext group
    }

    /**
     * Finds all the lights in a room by polling the bridge.
     *
     * @param   room        A room data (from json)
     *
     * @param   bridge      Info on the bridge in question
     */
    private suspend fun getRoomLightsFromApi(
        room: PHv2Room,
        bridge: PhilipsHueBridgeInfo
    ) : Set<PHv2Light> = withContext(Dispatchers.IO) {

        // find the lights. they'll be part of the children
        val lightSet = mutableSetOf<PHv2Light>()
        room.children.forEach { child ->
            if (child.rtype == RTYPE_DEVICE) {
                try {
                    val device = getLightInfoFromApi(child.rid, bridge)
                    if (device == null) {
                        Log.d(TAG, "device not found in getRoomLightsFromApi()! Skipping rid = ${child.rid}")
                    }
                    else if (device.data.isNotEmpty() && device.data[0].type == LIGHT) {
                            lightSet.add(device.data[0])
                    }

                }
                catch(e: Exception) {
                    Log.e(TAG, "problem interpreting child in getRoomLightsApi().  Perhaps it's something weird (not a light)")
                    e.printStackTrace()
                }
            }
        }

        return@withContext lightSet
    }

    /**
     * Retrieves info about a room from its id.
     *
     * @param   roomId      The v2 id for this room.
     *
     * @param   bridge      The bridge that controls this room.  It needs to be
     *                      connected and active.
     *
     * @return  The [PHv2Room] data associated with this room.
     */
    suspend fun getRoomInfoFromApi(
        roomId: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2Room = withContext(Dispatchers.IO) {

        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_ROOMS/$roomId"
        )

        val response = synchronousGet(
            url = url,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, bridge.token))
            )

        if (response.isSuccessful == false) {
            Log.e(TAG, "unsuccessful attempt at getting room data!  roomId = $roomId, bridgeId = ${bridge.id}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            // returning empty object
            return@withContext PHv2Room(JSONObject())
        }

        val roomData = PHv2Room(response.body)
        return@withContext roomData
    }

    /**
     * Retrieves info about a specific device given its id.
     * Returns null if not found.
     */
    private suspend fun getDeviceInfoFromApi(
        deviceId: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2Device? {

        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_DEVICE/$deviceId"
        )

        val response = synchronousGet(
            url = url,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, bridge.token)),
            trustAll = true     // todo: make more secure in future
        )

        if (response.isSuccessful == false) {
            Log.e(TAG, "can't get device (id = $deviceId) from bridge!")
            return null
        }

        // successful--process this info and return
        return PHv2Device(response.body)
    }

    /**
     * Retrieves info about a specific light, given its id.
     * Returns null on error (light can't be found).
     */
    private suspend fun getLightInfoFromApi(
        lightId: String,
        bridge: PhilipsHueBridgeInfo
    ) : PHv2LightIndividual? = withContext(Dispatchers.IO) {
        // construct the url
        val url = createFullAddress(
            ip = bridge.ip,
            suffix = "$SUFFIX_GET_LIGHTS/$lightId"
        )

        val response = synchronousGet(
            url = url,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, bridge.token)),
            trustAll = true     // todo: be paranoid in the future
        )

        if (response.isSuccessful == false) {
            Log.e(TAG, "unsuccessful attempt at getting light data!  lightId = $lightId, bridgeId = ${bridge.id}")
            Log.e(TAG, "   code = ${response.code}, message = ${response.message}, body = ${response.body}")
            return@withContext null
        }

        val lightData = PHv2LightIndividual(response.body)
        return@withContext lightData
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
//  suffixes
//

/** This is v1. It should ONLY be used when getting the token/username from bridge. */
private const val SUFFIX_API = "/api/"

/** Gets info about the bridge */
private const val SUFFIX_GET_BRIDGE = "/clip/v2/resource/bridge"

/** Used to get all the rooms associated with a bridge.  Goes AFTER the token */
private const val SUFFIX_GET_ROOMS = "/clip/v2/resource/room"

/**
 * Used to get info about a the bridge's lights.
 *
 * To get info about a specific light, add a slash and put the id AFTER!
 */
private const val SUFFIX_GET_LIGHTS = "/clip/v2/resource/light"

/**
 * Gets all the light groups for this bridge.
 *
 * For a specific light group, add a slash and then the id.
 */
private const val SUFFIX_GET_GROUPED_LIGHTS = "/clip/v2/resource/grouped_light"

/**
 * Gets all the devices.  The usual addition to get just one device details.
 */
private const val SUFFIX_GET_DEVICE = "/clip/v2/resource/device"

/** The header key when using a token/username for Philips Hue API calls */
private const val HEADER_TOKEN_KEY = "hue-application-key"


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
