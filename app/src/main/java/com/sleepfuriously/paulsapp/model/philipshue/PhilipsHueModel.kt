package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.*
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPost
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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
 *  [PHILIPS_HUE_BRIDGE_ID_PREFS_FILENAME]
 *
 *
 * This file is a Set of strings that can be converted to Ints that
 * will be the IDs of all the bridges that this app has seen (unless
 * deleted by the user).
 *
 * As you can see, it's very important that all the ids are unique.
 * Philips Hue has a brilliant way of making sure all the bridges,
 * devices, services, etc are all unique.  I just use those ids.
 */
class PhilipsHueModel(
    private val ctx: Context = MyApplication.appContext,

    // This is only used in the constructor, so it doesn't have to be a real
    // property.  Defining it here without a VAL makes its scope only during
    // init(), so it can be cleaned up more quickly by the garbage collector.
    coroutineScope: CoroutineScope
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

    /** Controls server-sent events (sse) */
    private val phServerSentEvents = PhilipsHueServerSentEvents(coroutineScope)


    //-------------------------------------
    //  initializations
    //-------------------------------------

    init {
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
                    } else {
                        val v2Bridge = PHv2ResourceBridge(jsonString)
                        if (v2Bridge.hasData() == false) {
                            Log.e(TAG, "Bridge data empty (ip = $ip) in properInit()!")
                            Log.e(TAG, "   error = ${v2Bridge.getError()}")
                        } else {
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
                    bridge.rooms = PhilipsHueDataConverter.convertV2RoomAll(roomsFromApi, bridge)
                } else {
                    bridge.active = false
                }

                newBridgeSet.add(bridge)
            }

            // 3. Connect each active bridge so we can receive server-sent events
            newBridgeSet.forEach { bridge ->
                if (bridge.active) {
                    startSseConnection(bridge)
                }
            }

            // update the flow for all the bridges
            _bridgeFlowSet.update { newBridgeSet }  // stateflow reflects changes
        }

        coroutineScope.launch {
            while (true) {
                phServerSentEvents.serverSentEvent.collect { sseEventPair ->
                    interpretEvent(sseEventPair.first, sseEventPair.second)
                }
            }
        }

        coroutineScope.launch {
            while (true) {
                phServerSentEvents.openEvent.collect { pair ->
                    Log.d(TAG, "collecting phServerSentEvents.openEvent. connect = ${pair.second}")
                    interpretOpenEvent(pair.first, pair.second)
                }
            }
        }
    }

    //-------------------------------------
    //  starters & stoppers
    //-------------------------------------

    /**
     * Connects the given bridge to this app, enabling this app to
     * receive updates on changes to the Philips Hue world.
     */
    fun startSseConnection(bridge: PhilipsHueBridgeInfo) {

        if (bridge.connected) {
            Log.e(TAG, "Trying to connect to a bridge that's already connected! bridge.id = ${bridge.id}")
            return
        }

        phServerSentEvents.startSse(bridge)
    }

    /**
     * Stop receiving updates about the Philps Hue IoT for this bridge.
     * If the bridge is not found, nothing is done.
     *
     * Should be no need to do any changes: the bridge itself should call onClosed()
     * which will be processed.
     */
    fun disconnectFromBridge(bridge: PhilipsHueBridgeInfo) {
        Log.d(TAG, "disconnect() called on bridge ${bridge.id} at ${bridge.ip}")
        phServerSentEvents.cancelSSE(bridge.id)
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
    fun getLoadedBridgeFromId(bridgeId: String): PhilipsHueBridgeInfo? {
        val bridge = bridgeFlowSet.value.find { bridge ->
            bridge.id == bridgeId
        }
        return bridge
    }

    //-------------------------------------
    //  update
    //-------------------------------------

    /**
     * The brightness of a room has changed (also includes on/off).
     * Do the necessary work so that the bridge flow reports the change.
     */
    suspend fun updateRoomBrightness(
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
        } else {
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
    fun deleteBridge(bridgeId: String): Boolean {

        val bridgeToDelete = getLoadedBridgeFromId(bridgeId) ?: return false

        // 1) Tell the bridge to remove this app's username (token)
        //      UNABLE TO COMPLY:  Philps Hue removed this ability, so it cannot be implemented!

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
            } else {
                removed = true
            }
        }
        if (removed == false) {
            Log.e(TAG, "Unable to remove bridge $bridgeToDelete at the final stage of deleteBridge(bridgeId = $bridgeId)!")
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
    @Suppress("unused")
    fun deleteAllBridges(): Boolean {

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
     * @param   eventBridgeId   The id of the bridge that the event relates to
     *
     * @param   event           Data structure describing the event.
     *
     * side effects
     *  [bridgeFlowSet] - modified to reflect whatever changed according the the bridge's sse.
     */
    private fun interpretEvent(eventBridgeId: String, event: PHv2ResourceServerSentEvent) {

        val eventBridge = getLoadedBridgeFromId(eventBridgeId)
        if (eventBridge == null) {
            Log.e(TAG, "Can't find the bridge in interpretEvent()! Aborting!")
            return
        }

        Log.d(TAG, "interpretEvent()  eventBridge = ${eventBridge.id}")
        Log.d(TAG, "interpretEvent()  event.type = ${event.type}")
        Log.d(TAG, "interpretEvent()  event.eventId = ${event.eventId}")
        Log.d(TAG, "interpretEvent()  event.data = ${event.data}")

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
                        Log.e(TAG, "interpretEvent() - can't do anything with an error, skipping!")
                        return
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
                    } else {
                        // yep, there is indeed a light. What changed?
                        Log.d(
                            TAG,
                            "updating light event (id = ${light.lightId}, deviceId = ${light.deviceId})"
                        )
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
                            } else {
                                Log.e(
                                    TAG,
                                    "Error in interpretEvent()--can't find room that owns grouped_light event. Aborting!"
                                )
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
     * A bridge connection for sse has either begun or ended.  Make changes
     * accordingly.
     *
     * @param   bridgeId        The bridge that changed.
     *
     * @param   isOpen          true -> bridge connection started
     *                          false -> bridge connection close
     */
    private fun interpretOpenEvent(bridgeId: String, isOpen: Boolean) {
        Log.d(TAG, "interpretOpenEvent() bridgeId = $bridgeId, isOpen = $isOpen")
        val bridge = getLoadedBridgeFromId(bridgeId)
        if (bridge == null) {
            Log.e(TAG, "Can't find bridge in interpretOpenEvent(). Aborting!")
            return
        }

        // rebuild the bridge list
        val newBridgeList = mutableListOf<PhilipsHueBridgeInfo>()
        bridgeFlowSet.value.forEach { aBridge ->
            // Is this the bridge in question?
            if (aBridge.id == bridgeId) {
                // yes, make the change, but to a copy (so the flow works correctly)
                Log.d(TAG, "Changing aBridge.connected from ${aBridge.connected} to $isOpen")
                val bridgeCopy = aBridge.copy(connected = isOpen)
                newBridgeList.add(bridgeCopy)
            }
            else {
                newBridgeList.add(aBridge)
            }
        }
        // assign new Set
        Log.d(TAG, "   - bridgeFlowSet should change")
        _bridgeFlowSet.update { newBridgeList.toSet() }
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
    ): Pair<String, GetBridgeTokenErrorEnum> {

        val fullAddress = PhilipsHueBridgeApi.createFullAddress(ip = bridgeIp, suffix = SUFFIX_API)
        Log.d(TAG, "registerAppToBridge() - fullAddress = $fullAddress")

        val response = synchronousPost(
            url = fullAddress,
            bodyStr = generateGetTokenBody(ctx = ctx),
            trustAll = true     // fixme when we have full security going
        )

        if (response.isSuccessful) {
            // get token
            val token = PhilipsHueDataConverter.getTokenFromBodyStr(response.body)
            Log.i(TAG, "requestTokenFromBridge($bridgeIp) -> $token")

            if (token != null) {
                return Pair(token, GetBridgeTokenErrorEnum.NO_ERROR)
            } else {
                Log.e(TAG, "unable to parse body in requestTokenFromBridge($bridgeIp)")
                Log.e(TAG, "   response => \n$response")
                return Pair("", GetBridgeTokenErrorEnum.CANNOT_PARSE_RESPONSE_BODY)
            }
        } else {
            Log.e(TAG, "error: unsuccessful attempt to get token from bridge at ip $bridgeIp")
            Log.e(TAG, "   response => \n$response")
            return Pair("", GetBridgeTokenErrorEnum.UNSUCCESSFUL_RESPONSE)
        }
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

