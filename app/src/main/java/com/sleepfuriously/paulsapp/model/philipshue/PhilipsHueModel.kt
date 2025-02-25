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
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPut
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.json.PHBridgePostTokenResponse
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
//            super.onEvent(eventSource, id, type, data)
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
            val bridgeIdsFromPrefs = PhilipsHueBridgeStorage.loadAllBridgeIds(ctx)
            if (bridgeIdsFromPrefs.isNullOrEmpty()) {
                return@runBlocking
            }

            /**  temp holder */
            val workingBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()

            // use these ids to load the ips & tokens from prefs
            bridgeIdsFromPrefs.forEach() { id ->

                // get the IP for this bridge
                val ip = PhilipsHueBridgeStorage.loadBridgeIp(id, ctx)

                // get the token for this bridge
                val token = PhilipsHueBridgeStorage.loadBridgeToken(id, ctx)

                val isActive = isBridgeActive(ip, token)
                var name = ""
                if (isActive) {
                    val jsonString = PhilipsHueBridgeApi.getBridgeDataStrFromApi(ip, token)
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
                    val roomsFromApi = PhilipsHueBridgeApi.getAllRooms(bridge)
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
        val responseStr = PhilipsHueBridgeApi.getBridgeDataStrFromApi(
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
        PhilipsHueBridgeStorage.saveBridge(bridgeToAdd, true, ctx)
    }


    //-------------------------------------
    //  read
    //-------------------------------------

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
    @Suppress("MemberVisibilityCanBePrivate")
    fun getBridge(bridgeId: String) : PhilipsHueBridgeInfo? {
        val bridge = bridgeFlowSet.value.find { bridge ->
            bridge.id == bridgeId
        }
        return bridge
    }

    //-------------------------------------
    //  update
    //-------------------------------------

    /**
     * The brightness of a room has changed.  Do the necessary work so that
     * the bridge flow reports the change.
     */
    suspend fun roomBrightnessChanged(
        newBrightness: Int,
        newOnStatus: Boolean,
        changedRoom: PhilipsHueRoomInfo,
        changedBridge: PhilipsHueBridgeInfo
    ) {
        // what's the name of the group of lights in this room?
        var groupedLightId = ""
        changedRoom.groupedLightServices.forEach { groupedLight ->
            if (groupedLight.rtype == "grouped_light") {
                groupedLightId = groupedLight.rid
            }
        }
        if (groupedLightId.isEmpty()) {
            Log.e(TAG, "Unable to find grouped_lights in roomBrightnessChanged(). room id = ${changedRoom.id}. Aborting!")
            return
        }

        // construct the body. consists of on and brightness in json format
        val body = "{\"on\": {\"on\": $newOnStatus}, \"dimming\": {\"brightness\": $newBrightness}}"
        Log.d(TAG, "---> body = $body")

        // construct the url
        val url = PhilipsHueBridgeApi.createFullAddress(
            ip = changedBridge.ip,
            suffix = "$SUFFIX_GET_GROUPED_LIGHTS/$groupedLightId"
        )

        // send the PUT
        val response = synchronousPut(
            url = url,
            bodyStr = body,
            headerList = listOf(Pair(HEADER_TOKEN_KEY, changedBridge.token)),
            trustAll = true     // fixme when we have full security going
        )

        // did it work?
        if (response.isSuccessful) {
            Log.d(TAG, "roomBrightnessChanged() PUT request successful!")
        }
        else {
            Log.e(TAG, "error sending PUT request in roomBrightnessChanged()!")
            Log.e(TAG, "response:")
            Log.e(TAG, "   code = ${response.code}")
            Log.e(TAG, "   message = ${response.message}")
            Log.e(TAG, "   headers = ${response.headers}")
            Log.e(TAG, "   body = ${response.body}")
        }
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
        if (PhilipsHueBridgeStorage.removeBridgeTokenPrefs(bridgeId, true, ctx) == false) {
            Log.e(TAG, "Problem removing token for bridgeId = $bridgeId")
            return false
        }

        PhilipsHueBridgeStorage.removeBridgeIP(bridgeId, true, ctx)

        if (PhilipsHueBridgeStorage.removeBridgeId(bridgeId, true, ctx) == false) {
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
    fun deleteAllBridges() : Boolean {

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

        val newBridgeList = mutableListOf<PhilipsHueBridgeInfo>()

        // rebuild the bridge list
        bridgeFlowSet.value.forEach { bridge ->
            // Is this the bridge in question?
            if (bridge.id == eventBridge.id) {
                // Yes, this is the bridge.  What kind of event was it?
                when (event.type) {
                    "update" -> {
                        Log.v(TAG, "interpeting UPDATE event")

                        interpretUpdateEvent(bridge, event)
                    }
                    "add" -> {
                        Log.e(TAG, "todo: implement interpeting ADD event")
                        TODO()
                    }
                    "delete" -> {
                        Log.e(TAG, "todo: implement interpeting DELETE event")
                        TODO()
                    }
                    "error" -> {
                        Log.e(TAG, "todo: implement interpeting ERROR event")
                        TODO()
                    }
                    else -> {
                        Log.e(TAG, "Unknown event type!!! Aborting!")
                        return
                    }
                }
            }

            // creating new bridge list for the flow to work properly
            newBridgeList.add(bridge)
        }

        // for debugging purposes...
        Log.d(TAG, "-->Setting bridgeFlowSet to newBridgeSet. Viewmodel should be updating!")
        newBridgeList.forEach { bridge ->
            bridge.rooms.forEach { room ->
                Log.d(TAG, "   ${room.name}: on = ${room.on}, bri = ${room.brightness}")
            }
        }
        _bridgeFlowSet.update { newBridgeList.toSet() }
    }

    /**
     * Interprets an UPDATE sse that was sent from a bridge.
     *
     * @param   bridge      The bridge that sent the event
     *
     * @param   event       The event it sent (should be an UPDATE)
     *
     * side effects
     *  - aspects of this bridge will change based on the event.  For
     *  example: a room's brightness and the status of its light can change.
     */
    private fun interpretUpdateEvent(
        bridge: PhilipsHueBridgeInfo,
        event: PHv2ResourceServerSentEvent
    ) {
        event.data.forEach { eventDatum ->
            // What kind of device changed?
            when (eventDatum.type) {
                "light" -> {
                    val light = findLightFromId(eventDatum.owner!!.rid, bridge)
                    if (light == null) {
                        Log.e(TAG, "unable to find changed light in interpretEvent()!")
                    }
                    else {
                        // yep, there is indeed a light. What changed?
                        Log.d(TAG, "updating light event (id = ${light.lightId}, deviceId = ${light.deviceId})")
                        if (eventDatum.on != null) {
                            // On/Off changed.  Set the light approprately.
                            light.state.on = eventDatum.on.on
                        }

                        if (eventDatum.dimming != null) {
                            light.state.bri = eventDatum.dimming.brightness
                        }

                        // todo: handle other changes to light
                    }
                }

                "grouped_light" -> {
                    Log.d(TAG, "updating grouped_light event. owner = ${eventDatum.owner}")

                    // figure out what rtype the owner of this grouped_light event is
                    when (eventDatum.owner?.rtype) {
                        RTYPE_PRIVATE_GROUP -> {
                            // todo
                            Log.e(TAG, "implement me!!!")
                        }
                        RTYPE_BRIDGE_HOME -> {
                            // todo
                            Log.e(TAG, "implement me!!!")
                        }
                        RTYPE_ROOM -> {
                            // Ah, the owner is a room.  Signal that room to change according to the event.
                            val roomId = eventDatum.owner.rid
                            val room = findRoomFromId(roomId, bridge)
                            if (room != null) {
                                if (eventDatum.dimming != null) {
                                    room.brightness = eventDatum.dimming.brightness
                                }
                                if (eventDatum.on != null) {
                                    room.on = eventDatum.on.on
                                }
                            }
                            else {
                                Log.e(TAG, "Error in interpretEvent()--can't find room that owns grouped_light event. Aborting!")
                            }
                        }
                    }
                }

                "room" -> {
                    Log.e(TAG, "Unhandled room event. Crashing now.")
                    TODO()
                }

            } // when (eventDatum.type)
        }
    }

    /**
     * Goes through all the rooms in the given bridge.  If an id matches, then
     * return that bridge.
     *
     * Null is returned of not found.
     */
    private fun findRoomFromId(
        roomId: String, bridge: PhilipsHueBridgeInfo
    ) : PhilipsHueRoomInfo? {
        bridge.rooms.forEach { room ->
            if (room.id == roomId) {
                return room
            }
        }
        return null
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
            val fullIp = PhilipsHueBridgeApi.createFullAddress(
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

        val fullAddress = PhilipsHueBridgeApi.createFullAddress(ip = bridgeIp, suffix = SUFFIX_API)
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
     * @param       bridgeIp    Ip of the bridge in question.  Does not check to see
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
        val fullAddress = PhilipsHueBridgeApi.createFullAddress(
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

    //~~~~~~~~~~~
    //  delete
    //~~~~~~~~~~~


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

            val v2RoomIndividual = PhilipsHueBridgeApi.getRoomIndividualFromApi(v2Room.id, bridge)
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
            val groupedLight = PhilipsHueBridgeApi.getGroupedLightFromApi(groupedLightServices[0].rid, bridge)
            var onOff = false
            var brightness = 0
            if ((groupedLight != null) && (groupedLight.errors.isEmpty())) {
                onOff = groupedLight.data[0].on.on
                brightness = groupedLight.data[0].dimming.brightness
            }

            // get the lights
            val regularLightSet = mutableSetOf<PhilipsHueLightInfo>()
            v2Room.children.forEach { child ->
                if (child.rtype == RTYPE_DEVICE) {
                    // is this device a light?  It's a light iff one of its services is rtype = "light".
                    val device = PhilipsHueBridgeApi.getDeviceIndividualFromApi(child.rid, bridge)
                    if (device.data.isNotEmpty() && (device.data[0].type == DEVICE)) {
                        // yes, it's a device (probably a light)!  To make sure, search its services.
                        device.data[0].services.forEach { service ->
                            if (service.rtype == RTYPE_LIGHT) {
                                val v2light = PhilipsHueBridgeApi.getLightInfoFromApi(service.rid, bridge)
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
                val v2ApiLight = PhilipsHueBridgeApi.getLightInfoFromApi(service.rid, bridge)
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


