package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPost
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueNewBridge
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceBridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.ZONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus
import kotlin.coroutines.coroutineContext

/**
 * This is the communicator between all the Philips Hue components
 * and the viewmodel.  This middle layer will receive flows and pass
 * them along.
 *
 * The main part of this is that it keeps track of all the bridges and their
 * components, creating lists that span bridges.
 *
 * But first, initialization.
 *  - Find all the long-term storage info for the known bridges.
 *  - Connect to those bridges (if possible).
 *
 * After initialization, receive flow updates on the bridges and pass that
 * along to whatever is observing [bridgeInfoList].
 *
 * NOTE: because [bridgeModelList] and [bridgeInfoList] are actually different,
 * we should ONLY USE _bridgeModelList within this class
 *
 * ---------------------------------------------------
 *
 * CONVENTIONS
 *
 *  - Quick functions that access in-memory data use get...(), is...(), and
 *    set...() in their function names.  Long-term data storage use load...()
 *    and save...().  Delete functions are always both short and long-term.
 *
 *  - Functions that access a bridge's version of the data always are appended
 *    with ...[From|To]Api() since these use the Philips Hue api.
 *
 * ---------------------------------------------------
 *
 * DETAILS
 *
 * The Philips Hue bridge [PhilipsHueBridgeModel] is the central device of the
 * Philips Hue lights.  It contains all the information about the light system
 * and is the primary point of contact for this app.
 *
 * I use [PhilipsHueBridgeStorage] to store and retrieve basic info about
 * bridges that have been found in the past.  There the ip and token are
 * generally stored in shared prefs (todo: use encrypted prefs),
 * but once the info is found, it is stored here for quick retrieval.
 * Saving this information is done asynchronously by default.  But for
 * special needs, it may be done synchronously (which of course needs
 * to be done off the Main thread).
 *
 *      Specifics on data storage
 *
 * Most of the data is stored in the default prefs file.  This includes
 * the info on how to access the different bridges.  The keys to access
 * the bridges are constructed thusly (based on [com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo]):
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
class PhilipsHueRepository(
    /** Anything that takes a while should be done within this scope */
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------
    //  flows to observe
    //-------------------------------

    /**
     * Holds our list of [PhilipsHueBridgeModel] as a StateFlow.  It's converted
     * to a list of [com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo] as a StateFlow (called [bridgeInfoList]).
     *
     * Changes anywhere down the line should percolate up and be sent along to any
     * observer of bridgeInfoList.
     */
    private val bridgeModelList = MutableStateFlow<List<PhilipsHueBridgeModel>>(listOf())

    /**
     * The complete list of all the bridges and associated data
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val bridgeInfoList: StateFlow<List<PhilipsHueBridgeInfo>> = bridgeModelList
        .flatMapLatest { bridgeModels ->
            Log.d(TAG, "change noted in _bridgeModelList -- trying to pass it along")
            if (bridgeModels.isEmpty()) {
                Log.d(TAG, "   passing along an empty list")
                MutableStateFlow(emptyList())
            }
            else {
                // tell the flocks about this change
                Log.d(TAG, "   while converting, sending info to flocks")
                updateFlocksLights(getAllLights())

                // convert
                val listOfStateFlowBridgeInfo =
                    bridgeModels.map(transform = PhilipsHueBridgeModel::bridge)
                Log.d(TAG, "   transforming into BridgeInfoList. size = ${bridgeModels.size}")
                combine(
                    flows = listOfStateFlowBridgeInfo,
                    transform = Array<PhilipsHueBridgeInfo>::toList
                )
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(100),
            initialValue = listOf()
        )


    /**
     * Our list of [PhilipsHueFlockModel]s.  When any data changes within
     * a FlockModel, it's change will be reflected in the flow below.
     */
    private val flockModelList = MutableStateFlow<List<PhilipsHueFlockModel>>(emptyList())

    /** Complete list of the Flocks with their associated data.  Ready to observe! */
    @OptIn(ExperimentalCoroutinesApi::class)
    val flockList: StateFlow<List<PhilipsHueFlock>> = flockModelList
        .flatMapLatest { flockModels ->
            if (flockModels.isEmpty()) {
                MutableStateFlow(emptyList())
            }
            else {
                val listOfStateFlowFlockInfo =
                    flockModels.map(transform = PhilipsHueFlockModel::flock)
                combine(
                    flows = listOfStateFlowFlockInfo,
                    transform = Array<PhilipsHueFlock>::toList
                )
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(100),
            initialValue = listOf()
        )

    //-------------------------------
    //  class variables
    //-------------------------------

    //-------------------------------
    //  init
    //

    init {

//        // inform the flocks when bridgeInfoFlist changes
//        coroutineScope.launch {
//            bridgeInfoList.collect {
//                sendChangeToFlocks(it)
//            }
//        }

        coroutineScope.launch {
            while (true) {
                bridgeInfoList.collect {
                    Log.d(TAG, "bridgeInfoList collected: size = ${it.size}")
                    it.forEach { bridgeInfo ->
                        Log.d(TAG, "   bridgeInfo: ${bridgeInfo.humanName}, lights size = ${bridgeInfo.lights.size}")
                    }

                    // create new light set
                    val newLightSet = mutableSetOf<PhilipsHueLightInfo>()
                    it.forEach { bridgeInfo ->
                        bridgeInfo.lights.forEach { lightInfo ->
                            newLightSet.add(lightInfo)
                        }
                    }
                    // create set of lights
                    updateFlocksLights(newLightSet)
                }
            }
        }


        // Now for the real initializations!  Load up the bridge information,
        // make our list of bridges, and start talking to them.
        coroutineScope.launch(Dispatchers.IO) {

            // todo: use addBridge() here

            // 1. Load bridge ids from prefs
            //
            val ctx = MyApplication.appContext
            val bridgeIdsFromPrefs = PhilipsHueBridgeStorage.loadAllBridgeIds(ctx)
            if (bridgeIdsFromPrefs.isNullOrEmpty()) {
                return@launch
            }

            /**  temp holder */
            val workingBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()

            // use these ids to load the ips & tokens from prefs
            bridgeIdsFromPrefs.forEach { id ->

                // get the IP for this bridge
                val ip = PhilipsHueBridgeStorage.loadBridgeIp(id, ctx)

                // get the token for this bridge
                val token = PhilipsHueBridgeStorage.loadBridgeToken(id, ctx)

                val isActive = isBridgeActive(ip, token)
                var name = ""
                if (isActive) {
                    val jsonBridgeResponseString = PhilipsHueBridgeApi.getBridgeDataStrFromApi(ip, token)
                    if (jsonBridgeResponseString.isEmpty()) {
                        Log.e(TAG, "Unable to get bridge data (ip = $ip) in properInit()!")
                    } else {
                        val v2Bridge = PHv2ResourceBridge(jsonBridgeResponseString)
                        if (v2Bridge.hasData()) {
                            // finally we can get the name!
                            name = v2Bridge.getDeviceName()
                        }
                        else {
                            Log.e(TAG, "Bridge data empty (ip = $ip) in properInit()!")
                            Log.e(TAG, "   error = ${v2Bridge.getError()}")
                        }
                    }
                }

                val bridge = PhilipsHueBridgeInfo(
                    v2Id = id,
                    bridgeId = name,
                    ipAddress = ip,
                    token = token,
                    active = isActive,
                    connected = false,
                    humanName = "initializing...",
                )

                workingBridgeSet += bridge
            }

            // 2. Load up the bridges and put them in our bridge list
            //
            val tmpBridgeModels = mutableListOf<PhilipsHueBridgeModel>()
            workingBridgeSet.forEach { workingBridge ->
                tmpBridgeModels.add(PhilipsHueBridgeModel(
                    bridgeV2Id = workingBridge.v2Id,
                    bridgeIpAddress = workingBridge.ipAddress,
                    bridgeToken = workingBridge.token,
                    coroutineScope = coroutineScope
                ))
            }

            // 3. update the flow (well, the variable that is reflected in the flow)
            //
            bridgeModelList.update {
                Log.d(TAG, "updating bridgeModelList to $tmpBridgeModels, size = ${tmpBridgeModels.size}")
                tmpBridgeModels.forEach { bridgeModel ->
                    Log.d(TAG, "    id = ${bridgeModel.bridgeV2Id}")
                    Log.d(TAG, "    bridge.value = ${bridgeModel.bridge}")
                }
                tmpBridgeModels
            }
        }

    }


    //-------------------------------
    //  starters & stoppers
    //-------------------------------

    /**
     * Connects the given bridge to this app, enabling this app to
     * receive updates on changes to the Philips Hue world (sse).
     */
    fun startSseConnection(bridge: PhilipsHueBridgeInfo) {

        if (bridge.connected) {
            Log.e(TAG, "Trying to connect to a bridge that's already connected! bridge.id = ${bridge.v2Id}")
            return
        }

        // find the bridge model and tell it to connect for sse
        val foundBridgeModel = bridgeModelList.value.find { bridgeModel ->
            bridgeModel.bridge.value.v2Id == bridge.v2Id
        }
        foundBridgeModel?.connectSSE()
    }

    /**
     * Stop receiving updates about the Philps Hue IoT for this bridge (sse).
     * If the bridge is not found, nothing is done.
     *
     * Should be no need to do any changes: the bridge itself should call onClosed()
     * which will be processed.
     */
    fun stopSseConnection(bridge: PhilipsHueBridgeInfo) {
        Log.d(TAG, "disconnect() called on bridge ${bridge.v2Id} at ${bridge.ipAddress}")

        // find the bridge model and disconnect sse
        val foundBridgeModel = bridgeModelList.value.find { bridgeModel ->
            bridgeModel.bridge.value.v2Id == bridge.v2Id
        }
        foundBridgeModel?.disconnectSSE()
    }

    //-------------------------------
    //  create
    //-------------------------------

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
    suspend fun registerAppToPhilipsHueBridge(
        bridgeIp: String
    ) : Pair<String, GetBridgeTokenErrorEnum> {

        val fullAddress = PhilipsHueBridgeApi.createFullAddress(ip = bridgeIp, suffix = SUFFIX_API)
        Log.d(TAG, "registerAppToBridge() - fullAddress = $fullAddress")

        val response = synchronousPost(
            url = fullAddress,
            bodyStr = generateGetTokenBody(ctx = MyApplication.appContext),
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
     *
     * @return  True if all went well
     */
    suspend fun addNewBridge(
        newBridge: PhilipsHueNewBridge
    ) : Boolean {

        // grab the id for this bridge
        val jsonResponseStr = PhilipsHueBridgeApi.getBridgeDataStrFromApi(
            bridgeIp = newBridge.ip,
            token = newBridge.token
        )
        val v2Bridge = PHv2ResourceBridge(jsonResponseStr)
        if (v2Bridge.hasData() == false) {
            Log.e(TAG, "Unable to get info about bridge in addBridge--aborting!")
            Log.e(TAG, "   error msg: ${v2Bridge.getError()}")
            return false
        }

        val id = v2Bridge.getId()

        val newBridgeModel = PhilipsHueBridgeModel(
            bridgeV2Id = id,
            bridgeIpAddress = newBridge.ip,
            bridgeToken = newBridge.token,
            coroutineScope = CoroutineScope(coroutineContext)
        )

        bridgeModelList.update {
            Log.d(TAG, "updating _bridgeModelFlowList(2) by adding $newBridgeModel")
            it + newBridgeModel
        }

        // update long-term data
        PhilipsHueBridgeStorage.saveBridge(
            bridgeId = id,
            bridgeipAddress = newBridge.ip,
            newToken = newBridge.token,
            synchronize = true,
            ctx = MyApplication.appContext
        )

        return true
    }

    /**
     * Adds a flock to our list.
     *
     * side effect
     *  A brand-new [PhilipsHueFlockModel] will be created to deal with the new
     *  flock.
     */
    fun addFlock(
        name: String,
        brightness: Int,
        onOffState: Boolean,
        /** Set of all the lights to be controlled by the flock */
        lightsAndBridges: Set<PhilipsHueLightSetAndBridge>
    ) {

        // create a new Flock Model and add it to our list
        val newFlockModel = PhilipsHueFlockModel(
            coroutineScope = coroutineScope,
            startLightsAndBridges = lightsAndBridges,
            humanName = name,
            brightness = brightness,
            onOff = onOffState
        )

        flockModelList.update {
            it + newFlockModel
        }
    }

    //-------------------------------
    //  read
    //-------------------------------

    /**
     * Checks to see if this bridge already exists in our list of bridges
     * Doesn't matter if it's active or not--just if it exists.
     */
    fun doesBridgeExist(ip: String) : Boolean {
        bridgeInfoList.value.forEach { bridgeInfo ->
            if (bridgeInfo.ipAddress == ip) {
                return true
            }
        }
        return false
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
    suspend fun doesPhilipsHueBridgeRespondToIp(ip: String) : Boolean {
        return doesBridgeRespondToIp(ip)
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
    suspend fun doesPhilipsHueBridgeAcceptToken(bridgeIp: String, token: String) : Boolean {
        return doesBridgeAcceptToken(bridgeIp, token)
    }

    /**
     * Finds all the scenes that can be displayed for this room.
     */
    fun getScenesForRoom(
        room: PhilipsHueRoomInfo,
        bridge: PhilipsHueBridgeInfo
    ) : List<PHv2Scene> {
        return PhilipsHueModelScenes.getAllScenesForRoom(room, bridge)
    }


    /**
     * Finds all the lights used by a bridge
     */
    fun getBridgeLights(bridgeIpAddress: String) : Set<PhilipsHueLightInfo> {
        val bridgeModel = bridgeModelList.value.find { it.bridgeIpAddress == bridgeIpAddress }
        if (bridgeModel == null) {
            Log.e(TAG, "Unable to find bridge in getBridgeLights() ip = $bridgeIpAddress! Returning empty Set.")
            return emptySet()
        }
        return bridgeModel.bridge.value.lights.toSet()
    }

    /**
     * Finds ALL the lights used in all the bridges!
     */
    fun getAllLights() : Set<PhilipsHueLightInfo> {
        val lightSet = mutableSetOf<PhilipsHueLightInfo>()
        bridgeModelList.value.forEach { bridgeModel ->
            lightSet.addAll(bridgeModel.bridge.value.lights)
        }
        return lightSet
    }

    /**
     * Searches through all the bridges to find which one controls the
     * specified light.  Returns NULL if not found.
     */
    fun getBridgeUsedByLight(lightV2Id: String) : PhilipsHueBridgeInfo? {
        // go through each bridge
        bridgeInfoList.value.forEach { bridge ->
            // try to find that light
            val foundLight = bridge.lights.find {
                light -> light.lightId == lightV2Id
            }
            if (foundLight != null) {
                // Yes, found it!  Return this bridge
                return bridge
            }
        }
        // No luck, didn't find it.
        return null
    }

    //-------------------------------
    //  senders (tell the bridge what to do)
    //-------------------------------

    /**
     * Inform the bridge that we want to change the over-all brightness of
     * the given room.
     *
     * @param   room            The room that is changing its on/off or brightness.
     *
     * @param   newBrightness   New brightness for this room [0..MAX_BRIGHTNESS]
     */
    fun sendRoomBrightnessToBridge(
        room: PhilipsHueRoomInfo,
        newBrightness: Int
    ) {
        val bridgeModelAndRoom = findBridgeModelAndRoom(room)
        if (bridgeModelAndRoom == null) {
            Log.e(TAG, "changeRoomBrightness() - can't because the room cannot be found!  room = ${room.name}")
            return
        }

        val foundBridgeModel = bridgeModelAndRoom.bridgeModel
        val foundRoom = bridgeModelAndRoom.room

        foundBridgeModel.sendRoomBrightnessToBridge(
            roomInfo = foundRoom,
            brightness = newBrightness
        )
    }

    /**
     * Tells a room to either turn on all lights or turn off all lights via the
     * bridge (down the line).
     *
     * @param   room            The room that is changing its on/off or brightness.
     *
     * @param   newOnStatus     When TRUE, switch room on.  FALSE: turn room off.
     */
    fun sendRoomOnOffToBridge(
        room: PhilipsHueRoomInfo,
        newOnStatus: Boolean
    ) {
        val bridgeModelAndRoom = findBridgeModelAndRoom(room)
        if (bridgeModelAndRoom == null) {
            Log.e(TAG, "changeRoomOnOff() - can't because the room cannot be found!  room = ${room.name}")
            return
        }

        val foundBridgeModel = bridgeModelAndRoom.bridgeModel
        val foundRoom = bridgeModelAndRoom.room

        foundBridgeModel.sendRoomOnOffStatusToBridge(
            roomInfo = foundRoom,
            onStatus = newOnStatus
        )
    }

    /**
     * Change the brightness of a given zone.
     */
    fun sendZoneBrightnessToBridge(
        zone: PhilipsHueZoneInfo,
        newBrightness: Int
    ) {
        val bridgeModelAndRoom = findBridgeModelAndZone(zone)
        if (bridgeModelAndRoom == null) {
            Log.e(TAG, "changeZoneBrightness() - can't because the room cannot be found!  room = ${zone.name}")
            return
        }

        val foundBridgeModel = bridgeModelAndRoom.bridgeModel
        val foundZone = bridgeModelAndRoom.zone

        foundBridgeModel.sendZoneBrightnessToBridge(
            zone = foundZone,
            brightness = newBrightness
        )
    }

    /**
     * Tell the relevant bridge to turn on or off a given zone.
     *
     * @param   newOnOff        True -> turn zone on.
     *                          False -> turn zone off.
     */
    fun sendZoneOnOffToBridges(
        zone: PhilipsHueZoneInfo,
        newOnOff: Boolean
    ) {
        val bridgeModelAndRoom = findBridgeModelAndZone(zone)
        if (bridgeModelAndRoom == null) {
            Log.e(TAG, "changeZoneOnOff() - can't because the room cannot be found!  room = ${zone.name}")
            return
        }

        val foundBridgeModel = bridgeModelAndRoom.bridgeModel
        val foundZone = bridgeModelAndRoom.zone

        foundBridgeModel.sendZoneOnOffToBridge(
            zone = foundZone,
            onStatus = newOnOff
        )
    }

    /**
     * Another intermediary: tell the bridge model to change a room so
     * that it's displaying the given scene.  The bridge model will in
     * turn tell the bridge itself to implement the scene.
     */
    fun sendRoomSceneToBridge(
        bridge: PhilipsHueBridgeInfo,
        room: PhilipsHueRoomInfo,
        scene: PHv2Scene
    ) {
        // First check to make sure that this scene actually references the correct
        // room.  If it doesn't, bail.
        if ((scene.group.rtype != ROOM) || (scene.group.rid != room.v2Id)) {
            Log.e(TAG, "updateRoomScene() - room does not match with scene. Aborting!")
            return
        }

        // Now that the scene and room matches, just tell the scene to turn on.  That's it.
        coroutineScope.launch(Dispatchers.IO) {
            val response = PhilipsHueBridgeApi.sendSceneToLightGroup(
                bridgeIp = bridge.ipAddress,
                bridgeToken = bridge.token,
                sceneToDisplay = scene
            )
            Log.d(TAG, "updateRoomScene() response:")
            Log.d(TAG, "    successful = ${response.isSuccessful}")
            Log.d(TAG, "    code = ${response.code}")
            Log.d(TAG, "    message = ${response.message}")
            Log.d(TAG, "    body = ${response.body}")
            Log.d(TAG, "    headers = ${response.headers}")

            // update the room about its current scene. But first find the
            // appropriate bridgeModel
            val daBridgeModel = bridgeModelList.value.find { bridgeModel ->
                // Gotta do this manually because the room's current scene can
                // change before it's finally saved (which will make the built-in
                // equals function return false when all we care about it the v2Id).
                var found = false
                for (roomToTest in bridgeModel.bridge.value.rooms) {
                    if (roomToTest.v2Id == room.v2Id) {
                        found = true
                        break
                    }
                }
                found
            }
            if (daBridgeModel == null) {
                Log.e(TAG, "updateRoomScene() could not find a BridgeModel with the given room! Current bridge won't work!!!")
                return@launch
            }
        }
    }

    /** similar to [sendRoomSceneToBridge] */
    fun sendZoneSceneToBridge(
        bridge: PhilipsHueBridgeInfo,
        zone: PhilipsHueZoneInfo,
        scene: PHv2Scene
    ) {
        if ((scene.group.rtype != ZONE) || (scene.group.rid != zone.v2Id)) {
            Log.e(TAG, "updateZoneScene() - zone does not match with scene. Aborting!")
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            val response = PhilipsHueBridgeApi.sendSceneToLightGroup(
                bridgeIp = bridge.ipAddress,
                bridgeToken = bridge.token,
                sceneToDisplay = scene
            )
            Log.d(TAG, "updateZoneScene() response:")
            Log.d(TAG, "    successful = ${response.isSuccessful}")
            Log.d(TAG, "    code = ${response.code}")
            Log.d(TAG, "    message = ${response.message}")
            Log.d(TAG, "    body = ${response.body}")
            Log.d(TAG, "    headers = ${response.headers}")

            // update the zone about its current scene. But first find the
            // appropriate bridgeModel
            val daBridgeModel = bridgeModelList.value.find { bridgeModel ->
                // Gotta do this manually (see updateRoomScene)
                var found = false
                for (zoneToTest in bridgeModel.bridge.value.zones) {
                    if (zoneToTest.v2Id == zone.v2Id) {
                        found = true
                        break
                    }
                }
                found
            }
            if (daBridgeModel == null) {
                Log.e(TAG, "updateZoneScene() could not find a BridgeModel with the given zone! Current bridge won't work!!!")
                return@launch
            }

            daBridgeModel.updateZoneCurrentScene(zone = zone, scene = scene)
        }
    }

    /**
     * Tells the flock to send an on or off event to its bridges for its lights.
     */
    suspend fun sendFlockOnOffToBridges(
        changedFlock: PhilipsHueFlock,
        newOnOff: Boolean
    ) {
        // find the FlockModel
        for (flockModel in flockModelList.value) {
            if (flockModel.flock.value.id == changedFlock.id) {
                // just do the first that matches (should be no other matching flocks!)
                flockModel.sendOnOffToBridges(newOnOff)
                return
            }
        }
        Log.e(TAG, "changeFlockOnOff() Unable to find flock id = ${changedFlock.id}, name = ${changedFlock.name}!")
    }

    //-------------------------------
    //  update (based on sse from bridge)
    //-------------------------------

    /**
     * Tell the flock that it should change its brightness.  This will eventually
     * propogate down to the bridges that the flock uses.  Those bridges will
     * actually tell its lights to change their brightness.
     */
    suspend fun sendFlockBrightnessToBridges(
        changedFlock: PhilipsHueFlock,
        newBrightness: Int
    ) {
        // find the flockModel
        for (flockModel in flockModelList.value) {
            if (flockModel.flock.value.id == changedFlock.id) {
                flockModel.sendBrightnessToBridges(newBrightness)
                return
            }
        }
        Log.e(TAG, "changeFlockBrightness() Unable to find flock id = ${changedFlock.id}, name = ${changedFlock.name}!")
    }

    /**
     * Tells the flocks to update their lights.  Should be called after
     * an sse from one or more bridges.
     *
     * todo: this is called from two places--make sure that both are really needed.
     */
    fun updateFlocksLights(lightSet: Set<PhilipsHueLightInfo>) {
        Log.d(TAG, "updateFlocksLights() begin")
        flockModelList.value.forEach { flockModel ->
            flockModel.updateAllFromBridges(lightSet)
        }
    }


    //-------------------------------
    //  delete
    //-------------------------------

    /**
     * Tell the model to remove this bridge.  Permanently.
     * Results will be propogated through a flow.
     */
    fun deletePhilipsHueBridge(bridgeId: String) {

        // make sure that the bridge exists
        var found = false
        for (bridgeModel in bridgeModelList.value) {
            if (bridgeModel.bridgeV2Id == bridgeId) {
                found = true
                break
            }
        }
        if (found == false) {
            Log.e(TAG, "unable to delete bridge id = $bridgeId")
            return
        }

        // 1) Tell the bridge to remove this app's username (token)
        //      UNABLE TO COMPLY:  Philps Hue removed this ability, so it cannot be implemented!
        //      The user has to do a RESET on the bridge to get this data removed.  What a pain!

        // 2) Remove bridge data from our permanent storage
        val ctx = MyApplication.appContext
        if (PhilipsHueBridgeStorage.removeBridgeTokenPrefs(bridgeId, true, ctx) == false) {
            Log.e(TAG, "Problem removing token for bridgeId = $bridgeId")
            return
        }

        PhilipsHueBridgeStorage.removeBridgeIP(bridgeId, true, ctx)

        if (PhilipsHueBridgeStorage.removeBridgeId(bridgeId, true, ctx) == false) {
            Log.e(TAG, "Problem removing id from bridgeId = $bridgeId")
            return
        }

        // 3) Remove bridge data from our temp storage.
        //  As this is a complicated data structure, we need to use a
        //  (kind of) complicated remove.
        bridgeModelList.update {
            // find the index of the bridge to remove
            var index = -1
            for (i in 0 until bridgeModelList.value.size) {
                if (bridgeModelList.value[i].bridge.value.v2Id == bridgeId) {
                    index = i
                    break
                }
            }
            if (index == -1) {
                Log.e(TAG, "Unable to find bridge id = $bridgeId in deleteBridge(). Aborting!")
                bridgeModelList.value
            }
            else {
                // todo: crashes here!!!  "lateinit property phSse has not been initialized"
                Log.d(TAG, "updating _bridgeModelFlowList by removing ${bridgeModelList.value[index]}")
                bridgeModelList.value - bridgeModelList.value[index]
            }
        }
    }

    //-------------------------------
    //  private functions
    //-------------------------------

    /**
     * Finds the [PhilipsHueRoomInfo] with a given id.  Also finds the
     * [PhilipsHueBridgeModel] that holds that room.
     *
     * @return      A [BridgeModelAndRoom] class holding both data.
     *              Null on error or not found.
     */
    private fun findBridgeModelAndRoom(
        room: PhilipsHueRoomInfo
    ) : BridgeModelAndRoom? {

        // go through the bridgeModels until we find one with the right room.
        for (bridgeModel in bridgeModelList.value) {

            // loop through all the rooms for this bridge
            for (bridgeRoom in bridgeModel.bridge.value.rooms) {
                if (bridgeRoom.v2Id == room.v2Id) {
                    // found it!
                    return BridgeModelAndRoom(
                        room = bridgeRoom,
                        bridgeModel = bridgeModel,
                    )
                }
            }
        }

        // didn't find it
        Log.d(TAG, "Unable to find room ${room.name} in findBridgeModelAndRoom()")
        return null
    }

    /**
     * Finds the [PhilipsHueZoneInfo] with a given id and the
     * [PhilipsHueBridgeModel] that holds that room.
     *
     * @return      A [BridgeModelAndRoom] class holding both data.
     *              Null on error or not found.
     */
    private fun findBridgeModelAndZone(
        zone: PhilipsHueZoneInfo
    ) : BridgeModelAndZone? {
        // loop through the BridgeModels until we find the right one
        for (bridgeModel in bridgeModelList.value) {
            // and try the zones
            for (bridgeZone in bridgeModel.bridge.value.zones) {
                if (bridgeZone.v2Id == zone.v2Id) {
                    // yippee, it's here!
                    return BridgeModelAndZone(
                        zone = bridgeZone,
                        bridgeModel = bridgeModel
                    )
                }
            }
        }

        Log.d(TAG, "findBridgeModelAndZone() - could not find zone ${zone.name} in any of the bridges")
        return null
    }

    //-------------------------------
    //  private data classes
    //-------------------------------

    /**
     * Quick and dirty little class that holds a reference to a
     * [PhilipsHueBridgeModel] and a Room.  Used for return values that need both.
     */
    private data class BridgeModelAndRoom(
        val bridgeModel: PhilipsHueBridgeModel,
        val room: PhilipsHueRoomInfo
    )

    /**
     * Similar to [BridgeModelAndRoom] this data class holds both the
     * [PhilipsHueBridgeModel] and a [PhilipsHueZoneInfo].
     */
    private data class BridgeModelAndZone(
        val bridgeModel: PhilipsHueBridgeModel,
        val zone: PhilipsHueZoneInfo
    )

}

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


private const val TAG = "PhilipsHueRepository"