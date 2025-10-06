package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.model.OkHttpUtils.synchronousPost
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceBridge
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
class PhilipsHueRepository(
    /** Anything that takes a while should be done within this scope */
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------
    //  flows to observe
    //-------------------------------

    /**
     * Holds our list of [PhilipsHueBridgeModel] as a StateFlow.  It's converted
     * to a list of [PhilipsHueBridgeInfo] as a StateFlow (called [bridgeInfoList]).
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

    //-------------------------------
    //  class variables
    //-------------------------------

    //-------------------------------
    //  init
    //

    init {
        // start consuming bridge flow from Model
        coroutineScope.launch {
            bridgeModelList.collectLatest {
                Log.d(TAG, "collecting bridgeFlowSet from bridgeModel:")
                Log.d(TAG, "    size = ${it.size}")
                Log.d(TAG, "    change = $it")
                Log.d(TAG, "    hash = ${System.identityHashCode(it)}")

                // rebuilding a copy of the bridge set
                val newBridgeList = mutableListOf<PhilipsHueBridgeModel>()
                it.forEach { bridge ->
                    newBridgeList.add(bridge)
                    Log.d(TAG, "Setting bridge for flow:")
                }
                // producing flow
                bridgeModelList.update {
                    Log.d(TAG, "updating bridgeList: $bridgeModelList")
                    newBridgeList
                }
            }
        }

        // Now for the real initializations!  Load up the bridge information,
        // make our list of bridges, and start talking to them.
        //
        // todo question:  should this be done BEFORE the flow is collected above?
        //
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
    suspend fun addBridge(
        newBridge: PhilipsHueNewBridge) : Boolean {

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

    //-------------------------------
    //  update
    //-------------------------------

//    /**
//     * Pass the call along to the Philips Hue model (after placing
//     * this in a coroutine).
//     */
//    /**
//     * Pass the call along to the Philips Hue model (after placing
//     * this in a coroutine).
//     */
//    fun updatePhilipsHueRoomBrightness(
//        newBrightness: Int,
//        newOnStatus: Boolean,
//        changedRoom: PhilipsHueRoomInfo,
//        changedBridge: PhilipsHueBridgeInfo
//    ) {
//        coroutineScope.launch(Dispatchers.IO) {
//            model.updateRoomBrightness(
//                newBrightness = newBrightness,
//                newOnStatus = newOnStatus,
//                changedRoom = changedRoom,
//                changedBridge = changedBridge
//            )
//        }
//    }

    /**
     * Another intermediary: tell the PH model to update a room so
     * that it's displaying the given scene.
     */
    fun updateRoomScene(
        bridge: PhilipsHueBridgeInfo,
        room: PhilipsHueRoomInfo,
        scene: PHv2Scene
    ) {
        // First check to make sure that this scene actually references the correct
        // room.  If it doesn't, bail.
        if ((scene.group.rtype != ROOM) || (scene.group.rid != room.id)) {
            Log.e(TAG, "updateRoomScene() - room does not match with scene. Aborting!")
            return
        }

        // Now that the scene and room matches, just tell the scene to turn on.  That's it.
        coroutineScope.launch(Dispatchers.IO) {
            val response = PhilipsHueBridgeApi.sendSceneToRoom(
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

            // nothing else to do.  The bridge will update everything with an sse.
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
                Log.d(TAG, "updating _bridgeModelFlowList by removing ${bridgeModelList.value[index]}")
                bridgeModelList.value - bridgeModelList.value[index]
            }
        }
    }

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