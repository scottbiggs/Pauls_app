package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.utils.OkHttpUtils.synchronousPost
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueStorage.assembleFlockNameKey
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueStorage.assembleFlockRoomsKey
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueStorage.assembleFlockZonesKey
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueNewBridge
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.WorkingFlock
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceBridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import com.sleepfuriously.paulsapp.model.philipshue.json.ZONE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plus

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
 * I use [PhilipsHueStorage] to store and retrieve basic info about
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
                updateFlocks(
                    roomSet = getAllRoomsForBridgeModels(bridgeModels),
                    zoneSet = getAllZonesForBridgeModels(bridgeModels)
                )

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
    val flockList: StateFlow<List<PhilipsHueFlockInfo>> = flockModelList
        .flatMapLatest { flockModels ->
            if (flockModels.isEmpty()) {
                MutableStateFlow(emptyList())
            }
            else {
                val listOfStateFlowFlockInfo =
                    flockModels.map(transform = PhilipsHueFlockModel::flock)
                combine(
                    flows = listOfStateFlowFlockInfo,
                    transform = Array<PhilipsHueFlockInfo>::toList
                )
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(100),
            initialValue = listOf()
        )

    private val _phRepoLoading = MutableStateFlow(false)
    /** When true, Philips Hue components are being loaded (bridges and flocks). */
    val phRepoLoading = _phRepoLoading.asStateFlow()

    //-------------------------------
    //  class variables
    //-------------------------------

    //-------------------------------
    //  init
    //

    init {
        coroutineScope.launch {
            while (true) {
                bridgeInfoList.collect {
                    Log.d(TAG, "bridgeInfoList collected: size = ${it.size}")
                    it.forEach { bridgeInfo ->
                        Log.d(TAG, "   bridgeInfo: ${bridgeInfo.humanName}, lights size = ${bridgeInfo.lights.size}")
                    }

                    updateFlocks(
                        roomSet = getAllRoomsForBridgeModels(bridgeModelList.value),
                        zoneSet = getAllZonesForBridgeModels(bridgeModelList.value)
                    )
                }
            }
        }


        // Now for the real initializations!  Load up the bridge information,
        // make our list of bridges, and start talking to them.
        coroutineScope.launch(Dispatchers.IO) {

            // todo: use addBridge() here

            // 1. Load bridge ids from prefs
            //
            _phRepoLoading.update { true }      // indicate loading started

            val ctx = MyApplication.appContext
            val bridgeIdsFromPrefs = PhilipsHueStorage.loadAllBridgeIds(ctx)
            if (bridgeIdsFromPrefs.isNullOrEmpty()) {
                _phRepoLoading.update { false }     // no longer loading
                return@launch
            }

            /**  temp holder */
            val workingBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()

            // use these ids to load the ips & tokens from prefs
            bridgeIdsFromPrefs.forEach { id ->

                // get the IP for this bridge
                val ip = PhilipsHueStorage.loadBridgeIp(id, ctx)

                // get the token for this bridge
                var token = PhilipsHueStorage.loadBridgeToken(
                    bridgeId = id,
                    ctx = ctx,
                )

                // test to see if the bridge accespts this token.  If not, then
                // we need to update the token to the new secure system.
                if (doesBridgeAcceptToken(bridgeIp = ip, token = token) == false) {
                    // Yep, this is probably an old unencrypted token.  Update it.
                    PhilipsHueStorage.updateToken(id, ctx)

                    // and now retrieve the token (correctly interpreted)
                    token = PhilipsHueStorage.loadBridgeToken(id, ctx)
                }

                val isActive = isBridgeActive(ip, token)
                var name = ""
                if (isActive) {
                    val jsonBridgeResponseString = PhilipsHueApi.getBridgeStrFromApi(ip, token)
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
                val newBridgeModel = PhilipsHueBridgeModel(
                    bridgeV2Id = workingBridge.v2Id,
                    bridgeIpAddress = workingBridge.ipAddress,
                    bridgeToken = workingBridge.token,
                    coroutineScope = coroutineScope
                )
                // wait for the Bridge Model to finish
                while (newBridgeModel.initializing) {
                    delay(50)
                }
                tmpBridgeModels.add(newBridgeModel)
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

            // 4. Now that the flow is updated, load the Flocks
            //
            loadAllFlocks(ctx)

            // 5. Done
            _phRepoLoading.update { false }
            Log.d(TAG, "done loading: phRepoLoading = ${phRepoLoading.value}")
        }

    }


    //-----------------------------------------------------------
    //  bridge functions
    //-----------------------------------------------------------

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

        val fullAddress = PhilipsHueApi.createFullAddress(ip = bridgeIp, suffix = SUFFIX_API)
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
        val v2Bridge = PhilipsHueApi.getBridgeFromApi(
            bridgeIpStr = newBridge.ip,
            token = newBridge.token
        )

        if (v2Bridge.hasErrors()) {
            Log.e(TAG, "Unable to get info about bridge in addBridge--aborting!")
            Log.e(TAG, "   error msg: ${v2Bridge.getError()}")
            return false
        }

        val id = v2Bridge.getId()

        val newBridgeModel = PhilipsHueBridgeModel(
            bridgeV2Id = id,
            bridgeIpAddress = newBridge.ip,
            bridgeToken = newBridge.token,
            coroutineScope = CoroutineScope(currentCoroutineContext())
        )

        bridgeModelList.update {
            Log.d(TAG, "updating _bridgeModelFlowList(2) by adding $newBridgeModel")
            it + newBridgeModel
        }

        // update long-term data
        PhilipsHueStorage.saveBridge(
            bridgeId = id,
            bridgeipAddress = newBridge.ip,
            newToken = newBridge.token,
            synchronize = true,
            ctx = MyApplication.appContext
        )

        return true
    }

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
     * Finds all the rooms for all the bridges.
     */
    fun getAllRooms() : Set<PhilipsHueRoomInfo> {
        return getAllRoomsForBridgeModels(bridgeModelList.value)
    }

    /**
     * Helper. Finds all the rooms used by all the given [PhilipsHueBridgeModel].
     */
    private fun getAllRoomsForBridgeModels(
        bridgeModels: List<PhilipsHueBridgeModel>
    ) : Set<PhilipsHueRoomInfo> {

        val roomSet = mutableSetOf<PhilipsHueRoomInfo>()
        bridgeModels.forEach { bridgeModel ->
            roomSet += bridgeModel.bridge.value.rooms
        }
        return roomSet
    }

    /**
     * Like [getAllRooms] this returns all the zones for all the bridges.
     */
    fun getAllZones() : Set<PhilipsHueZoneInfo> {
        return getAllZonesForBridgeModels(bridgeModelList.value)
    }

    /**
     * Helper. Finds all the rooms used by all the given [PhilipsHueBridgeModel].
     */
    private fun getAllZonesForBridgeModels(
        bridgeModels: List<PhilipsHueBridgeModel>
    ) : Set<PhilipsHueZoneInfo> {

        val zoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        bridgeModels.forEach { bridgeModel ->
            zoneSet += bridgeModel.bridge.value.zones
        }
        return zoneSet
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

    /**
     * Finds all the bridges used by the given set of rooms.
     *
     * @return  A Set of [PhilipsHueBridgeInfo] that fits the bill.
     *          Could be empty.
     */
    fun getBridgesFromRoomSet(roomSet: Set<PhilipsHueRoomInfo>) : Set<PhilipsHueBridgeInfo> {
        val foundBridges = mutableSetOf<PhilipsHueBridgeInfo>()

        roomSet.forEach { room ->
            // check to see if this room's bridge IP matches any of our bridges
            val found = bridgeInfoList.value.find { it.ipAddress == room.bridgeIpAddress }
            if (found != null) {
                foundBridges.add(found)
            }
        }

        return foundBridges
    }

    /**
     * Similar to [getBridgesFromRoomSet] except for zones!
     */
    fun getBridgesFromZoneSet(zoneSet: Set<PhilipsHueZoneInfo>) : Set<PhilipsHueBridgeInfo> {
        val foundBridges = mutableSetOf<PhilipsHueBridgeInfo>()
        zoneSet.forEach { zone ->
            val found = bridgeInfoList.value.find { it.ipAddress == zone.bridgeIpAddress }
            if (found != null) {
                foundBridges.add(found)
            }
        }
        return foundBridges
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
    fun sendZoneOnOffToBridge(
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
     * Another intermediary: tell the bridge to change a room so
     * that it's displaying the given scene.  The bridge model will in
     * turn tell the bridge itself to implement the scene.
     *
     * Note
     *  Unlike most send... functions, this also does an update.  That's
     *  because the scene name needs to be passed to the room, which does
     *  not occur during the sse after the lights have changed.
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

        // Now that the scene and room matches, just tell the scene to turn on.
        coroutineScope.launch(Dispatchers.IO) {
            val response = PhilipsHueApi.sendSceneToLightGroup(
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

            // Update the room about its current scene. But first find the
            // appropriate bridgeModel.
            //
            // This step is necessary so that the room knows what scene it
            // is currently displaying (that info is not provided in the sse).
            //
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
            daBridgeModel.updateRoomCurrentScene(room = room, scene = scene)
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
            val response = PhilipsHueApi.sendSceneToLightGroup(
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
        if (PhilipsHueStorage.removeBridgeTokenPrefs(bridgeId, true, ctx) == false) {
            Log.e(TAG, "Problem removing token for bridgeId = $bridgeId")
            return
        }

        PhilipsHueStorage.removeBridgeIP(bridgeId, true, ctx)

        if (PhilipsHueStorage.removeBridgeId(bridgeId, true, ctx) == false) {
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


    /**
     * Finds the [PhilipsHueRoomInfo] with a given id.  Also finds the
     * [PhilipsHueBridgeModel] that holds that room.
     *
     * @return      A [BridgeModelAndRoom] class holding both data.
     *              Null on error or not found.
     */
    fun findBridgeModelAndRoom(
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
    fun findBridgeModelAndZone(
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

    /**
     * Finds the given bridge and tells it to reset.
     */
    fun resetBridge(bridge: PhilipsHueBridgeInfo) {
        val bridgeModel = bridgeModelList.value.find { it.bridgeIpAddress == bridge.ipAddress }
        if (bridgeModel == null) {
            Log.e(TAG, "resetBridge() - unable to find bridge!!! Aborting!")
            return
        }

        coroutineScope.launch {
            bridgeModel.refresh()
        }
    }

    //-----------------------------------------------------------
    //  flock functions
    //-----------------------------------------------------------

    /**
     * Does the work of loading all the flocks from long-term storage.
     * This involves creating the Flocks and the [PhilipsHueFlockModel]s.
     *
     * NOTE
     *  This needs to be called AFTER all the bridges and their stuff has
     *  already been set up.  This will actually make a slight delay before
     *  beginning so that it gives the sharedprefs time to recover from any
     *  other work that has been done.
     *
     *  @return     True if all went well.  False on error.
     */
    suspend fun loadAllFlocks(ctx: Context) = withContext(Dispatchers.IO) {

        // wait just a bit for the shared prefs to settle
        delay(150)

        Log.d(TAG, "loadAllFlocks() - bridges = ${bridgeInfoList.value.size}")

        // Get all the working flocks from long-term storage.
        // Start by getting the ids.
        val prefs = ctx.getSharedPreferences(PHILIPS_HUE_FLOCK_PREFS_FILENAME, Context.MODE_PRIVATE)
        val idSet = prefs.getStringSet(PHILIPS_HUE_FLOCK_IDS_KEY, emptySet())
        if (idSet.isNullOrEmpty()) {
            Log.w(TAG, "loadAllFlocks() - no ids to gather--nothing to do.")
            return@withContext
        }

        // go through the ids and load the data
        for (flockId in idSet) {
            // get the name for this flock
            val nameKey = assembleFlockNameKey(flockId)
            val name = prefs.getString(nameKey, EMPTY_STRING)
            if (name.isNullOrEmpty()) {
                Log.e(TAG, "loadAllFlocks() - unable to find name for flock id = $flockId! skipping")
                continue
            }
            // get room ids
            val roomKey = assembleFlockRoomsKey(flockId)
            val roomIds = prefs.getStringSet(roomKey, emptySet())
            if (roomIds == null) {
                Log.e(TAG, "loadAllFlocks() - big problem getting room ids! skipping!")
                break
            }

            // get zone ids
            val zoneKey = assembleFlockZonesKey(flockId)
            val zoneIds = prefs.getStringSet(zoneKey, emptySet())
            if (zoneIds == null) {
                Log.e(TAG, "loadAllFlocks() - big problem getting zone ids! skipping!")
                break
            }

            // convert to working flock and add id
            val workingFlock = WorkingFlock(
                id = flockId,
                name = name,
                roomIdSet = roomIds,
                zoneIdSet = zoneIds
            )

            // convert to regular flock
            val flockInfo = makeFlockInfoFromWorkingFlock(workingFlock)

            // finally create a Flock Model and get it going
            flockModelList.update {
                it + PhilipsHueFlockModel(
                    coroutineScope = coroutineScope,
                    bridgeSet = flockInfo.bridgeSet,
                    roomSet = flockInfo.roomSet,
                    zoneSet = flockInfo.zoneSet,
                    humanName = flockInfo.name,
                    id = flockId,
                    repository = this@PhilipsHueRepository
                )
            }
        } // for each flockId
    }


    /**
     * Adds a flock to our list.
     *
     * @param   roomSet     Set of all the rooms to be controlled by the flock
     *
     * @param   zoneSet     all the zones controlled by this flock
     *
     * @param   longTermStorage     When TRUE, save this info to long-term
     *                              storage--used only when creating a brand-new
     *                              Flock, not when loading a known flock.
     *
     * NOTE
     *      This does NOT save anything to long-term.  The caller is responsible
     *      for figuring out it that's necessary or not.
     *
     * side effect
     *  A brand-new [PhilipsHueFlockModel] will be created to deal with the new
     *  flock.
     */
    fun addFlock(
        name: String,
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>,
        longTermStorage: Boolean
    ) {
        // figure out the bridges we're using for this flock
        val bridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
        bridgeSet += getBridgesFromRoomSet(roomSet)
        bridgeSet += getBridgesFromZoneSet(zoneSet)

        // create a new Flock Model and add it to our list
        val newFlockModel = PhilipsHueFlockModel(
            humanName = name,
            roomSet = roomSet,
            zoneSet = zoneSet,
            bridgeSet = bridgeSet,
            repository = this,
            coroutineScope = coroutineScope
        )

        flockModelList.update {
            it + newFlockModel
        }

        if (longTermStorage) {
            Log.d(TAG, "addFlock() saving to long-term storage ${newFlockModel.flock.value.name}")
            PhilipsHueStorage.saveFlock(
                ctx = MyApplication.appContext,
                flock = newFlockModel.flock.value
            )
        }
    }


    /**
     * Removes the given flock from the current data and permanently as well.
     * Doesn't ask questions.  If the given flock doesn't exist, then of course
     * nothing is done.
     */
    fun deleteFlock(flock: PhilipsHueFlockInfo) {

        // find the flock model that holds this flock
        val flockModel = flockModelList.value.find { flockModel -> flockModel.flock.value.id == flock.id }
        if (flockModel == null) {
            Log.d(TAG, "deleteFlock() - unable to find flock!  Nothing to do.")
            return
        }

        // long-term deletion
        PhilipsHueStorage.removeFlock(MyApplication.appContext, flock, coroutineScope)

        // remove the flock model from the list
        flockModelList.update {
            it - flockModel
        }
    }

    /**
     * Use this function to add a flock that was loaded from long-term storage.
     * This uses a [WorkingFlock], which has just some of an entire [PhilipsHueFlockInfo]
     * data.  This is needed as when loading not all the data has been figured out yet.
     *
     * side effects
     *  - a new [PhilipsHueFlockModel] is created with the given flock.
     */
    fun loadFlockFromLongTerm(workingFlock: WorkingFlock) {

        // get the room set by checking all the rooms in all the bridges
        // to find the matching ids
        val roomSet = mutableSetOf<PhilipsHueRoomInfo>()
        for (roomId in workingFlock.roomIdSet) {
            for (bridge in bridgeInfoList.value) {
                val room = bridge.getRoomById(roomId)
                if (room != null) {
                    // found it!
                    Log.d(TAG, "loadFlockFromLongTerm() - found a room: ${room.name}")
                    roomSet.add(room)
                    break       // no more reason to check in this bridge
                }
            }
        }

        // similar for zones
        val zoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        for (zoneId in workingFlock.zoneIdSet) {
            for (bridge in bridgeInfoList.value) {
                val zone = bridge.getZoneById(zoneId)
                if (zone != null) {
                    zoneSet.add(zone)
                    break
                }
            }
        }

        // Figure out the bridges
        val bridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
        bridgeSet += getBridgesFromRoomSet(roomSet)
        bridgeSet += getBridgesFromZoneSet(zoneSet)


        // create a new Flock Model and add it to our list
        val newFlockModel = PhilipsHueFlockModel(
            humanName = workingFlock.name,
            roomSet = roomSet,
            zoneSet = zoneSet,
            bridgeSet = bridgeSet,
            repository = this,
            coroutineScope = coroutineScope
        )

        flockModelList.update {
            it + newFlockModel
        }
    }

    /**
     * Since there are special functions that exist only in a [PhilipsHueFlockModel],
     * it might be nice to find that when all you have is a [PhilipsHueFlockInfo].
     *
     * @return      The Flock Model that controls the give flock.  Null if not found.
     */
    fun findFlockModelFromFlock(flock: PhilipsHueFlockInfo) : PhilipsHueFlockModel? {
        return flockModelList.value.find {
            it.flock.value.id == flock.id
        }
    }

    /**
     * All you have is the ID of a flock?  Well this'll return
     * the [PhilipsHueFlockModel] for that flock.  Then you can
     * have some fun.
     *
     * @return      Returns NULL if not found.
     */
    fun findFlockModelFromFlockId(flockId: String) : PhilipsHueFlockModel? {
        return flockModelList.value.find {
            it.flock.value.id == flockId
        }
    }

    /**
     * Call this function to indicate that a flock has changed.
     *
     * @param   origFlockId     The ID of the flock that has changed.
     *
     */
    fun editFlock(
        origFlockId: String,
        name: String,
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>
    ) {
        val model = findFlockModelFromFlockId(origFlockId)
        if (model == null) {
            Log.d(TAG, "editFlock() - unable to find flock model for given ID. Aborting!")
            return
        }

        // figure out the bridges we're using for this flock
        val bridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
        bridgeSet += getBridgesFromRoomSet(roomSet)
        bridgeSet += getBridgesFromZoneSet(zoneSet)

        val changedFlock = model.flock.value.copy(
            name = name,
            roomSet = roomSet,
            zoneSet = zoneSet,
            bridgeSet = bridgeSet
        )

        // tell the Model to update its flock
        model.updateFlock(changedFlock)

        // and finally save this new info
        PhilipsHueStorage.saveFlock(
            ctx = MyApplication.appContext,
            flock = changedFlock
        )

    }

    /**
     * Helper function that figures out how to turn a working flock into a
     * full-fledged [PhilipsHueFlockInfo].
     *
     * @param       workingFlock    Flock data in working format.
     *                              NOTE that if the ID is blank a new one
     *                              will be generated.
     *
     * preconditions
     *  - bridges already loaded
     *
     *  @return     A ready-to-go flock info.  Null on some sort of error.
     */
    fun makeFlockInfoFromWorkingFlock(
        workingFlock: WorkingFlock
    ) : PhilipsHueFlockInfo {

        // get the room set by checking all the rooms in all the bridges
        // to find the matching ids
        val roomSet = mutableSetOf<PhilipsHueRoomInfo>()
        for (roomId in workingFlock.roomIdSet) {
            for (bridge in bridgeInfoList.value) {
                val room = bridge.getRoomById(roomId)
                if (room != null) {
                    // found it!
                    Log.d(TAG, "loadFlockFromLongTerm() - found a room: ${room.name}")
                    roomSet.add(room)
                    break       // no more reason to check in this bridge
                }
            }
        }

        // similar for zones
        val zoneSet = mutableSetOf<PhilipsHueZoneInfo>()
        for (zoneId in workingFlock.zoneIdSet) {
            for (bridge in bridgeInfoList.value) {
                val zone = bridge.getZoneById(zoneId)
                if (zone != null) {
                    zoneSet.add(zone)
                    break
                }
            }
        }

        // Figure out the bridges
        val bridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
        bridgeSet += getBridgesFromRoomSet(roomSet)
        bridgeSet += getBridgesFromZoneSet(zoneSet)

        // Use a temp flock to calculate the brightness and the on/off state
        val tmpFlock = PhilipsHueFlockInfo(
            id = workingFlock.id,
            name = workingFlock.name,
            brightness = 0,         // not set yet
            onOffState = false,     // not set yet
            bridgeSet = bridgeSet,
            roomSet = roomSet,
            zoneSet = zoneSet
        )

        return tmpFlock.copy(
            brightness = tmpFlock.calculateBrightness(),
            onOffState = tmpFlock.calculateOnOff()
        )
    }

    /**
     * Similar to [sendRoomSceneToBridge], except that this has the extra
     * complication of Flocks.  So that's where most of the work happens.
     */
    fun sendFlockSceneToBridge(
        flock: PhilipsHueFlockInfo,
        scene: PHv2Scene
    ) {
        Log.d(TAG, "sendFlockSceneToBridge() - flock = ${flock.name}, ${flock.id}")

        // find the flock model
        var flockModel: PhilipsHueFlockModel? = null
        for (fm in flockModelList.value) {
            if (fm.flock.value.id == flock.id) {
                flockModel = fm
                break
            }
        }
        if (flockModel == null) {
            Log.e(TAG, "sendFlockSceneToBridge() - unable to find flock model! Aborting!")
            return
        }

        // finally tell the flock to send the scene stuff to the bridge and up
        coroutineScope.launch(Dispatchers.IO) {
            val worked = flockModel.sendSceneNameToBridges(scene.metadata.name)
            if (worked) {
                flockModel.updateFlockCurrentScene(scene)
            }
        }
    }


    /**
     * Tells the flock to send an on or off event to its bridges for its lights.
     */
    fun sendFlockOnOffToBridges(
        changedFlock: PhilipsHueFlockInfo,
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
    fun sendFlockBrightnessToBridges(
        changedFlock: PhilipsHueFlockInfo,
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
     * an sse from one or more bridges.  Or possibly during initialization?
     *
     * todo: this is called from two places--make sure that both are really needed.
     */
    fun updateFlocks(
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>
    ) {
        Log.d(TAG, "updateFlocksLights() begin")
        flockModelList.value.forEach { flockModel ->
            flockModel.updateAllFromBridges(roomSet, zoneSet)
        }
    }


    //-----------------------------------------------------------
    //  server-sent events
    //-----------------------------------------------------------

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
    //  data classes & enums
    //-------------------------------

    /**
     * Quick and dirty little class that holds a reference to a
     * [PhilipsHueBridgeModel] and a Room.  Used for return values that need both.
     */
    data class BridgeModelAndRoom(
        val bridgeModel: PhilipsHueBridgeModel,
        val room: PhilipsHueRoomInfo
    )

    /**
     * Similar to [BridgeModelAndRoom] this data class holds both the
     * [PhilipsHueBridgeModel] and a [PhilipsHueZoneInfo].
     */
    data class BridgeModelAndZone(
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