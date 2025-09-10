package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.ROOM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * along to whatever is observing [bridgesList].
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
    /**
     * Anything that takes a while should be done within this scope.
     */
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------
    //  flows to observe
    //-------------------------------

    private val _bridgesList = MutableStateFlow<List<PhilipsHueBridgeModel>>(listOf())
    /** the complete list of all the bridges and associated data (comes from [PhilipsHueModel]) */
    val bridgesList = _bridgesList.asStateFlow()


    //-------------------------------
    //  class variables
    //-------------------------------

    /** Accessor to the Philips Hue Model */
    private val model = PhilipsHueModel(coroutineScope = coroutineScope)

    //-------------------------------
    //  init
    //

    init {
        // start consuming bridge flow from Model
        coroutineScope.launch {
            model.bridgeModelFlowList.collectLatest {
                Log.d(TAG, "collecting bridgeFlowSet from bridgeModel. change = $it, hash = ${System.identityHashCode(it)}")

                // rebuilding a copy of the bridge set
                val newBridgeList = mutableListOf<PhilipsHueBridgeModel>()
                it.forEach { bridge ->
                    newBridgeList.add(bridge)
                    Log.d(TAG, "Setting bridge for flow:")
//                    bridge.rooms.forEach { room ->
//                        Log.d(TAG, "   room ${room.name}, on = ${room.on}, bri = ${room.brightness}")
//                    }
                }
                // producing flow
                _bridgesList.update { newBridgeList }
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
        val foundBridgeModel = bridgesList.value.find { bridgeModel ->
            bridgeModel.bridge.value?.v2Id == bridge.v2Id
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
    fun disconnectSseFromBridge(bridge: PhilipsHueBridgeInfo) {
        Log.d(TAG, "disconnect() called on bridge ${bridge.v2Id} at ${bridge.ipAddress}")

        // find the bridge model and disconnect sse
        val foundBridgeModel = bridgesList.value.find { bridgeModel ->
            bridgeModel.bridge.value?.v2Id == bridge.v2Id
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
        return model.registerAppToBridge(bridgeIp)
    }

    /**
     * Connects the given bridge to this app, enabling this app to
     * receive updates on changes to the Philips Hue world.
     */
    fun startPhilipsHueSseConnection(bridge: PhilipsHueBridgeInfo) {
        model.startSseConnection(bridge)
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
     */
    suspend fun addPhilipsHueBridge(
        newBridge: PhilipsHueNewBridge
    ) = withContext(Dispatchers.IO) {
        model.addBridge(newBridge)
    }

    //-------------------------------
    //  read
    //-------------------------------

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

    /**
     * Pass the call along to the Philips Hue model (after placing
     * this in a coroutine).
     */
    /**
     * Pass the call along to the Philips Hue model (after placing
     * this in a coroutine).
     */
    fun updatePhilipsHueRoomBrightness(
        newBrightness: Int,
        newOnStatus: Boolean,
        changedRoom: PhilipsHueRoomInfo,
        changedBridge: PhilipsHueBridgeInfo
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            model.updateRoomBrightness(
                newBrightness = newBrightness,
                newOnStatus = newOnStatus,
                changedRoom = changedRoom,
                changedBridge = changedBridge
            )
        }
    }

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
        model.deleteBridge(bridgeId)
    }

    /**
     * Stop receiving updates about the Philps Hue IoT for this bridge.
     * If the bridge is not found, nothing is done.
     */
    /**
     * Stop receiving updates about the Philps Hue IoT for this bridge.
     * If the bridge is not found, nothing is done.
     */
    fun stopPhilipsHueSseConnection(bridge: PhilipsHueBridgeInfo) {
        model.disconnectFromBridge(bridge)
    }
}


private const val TAG = "PhilipsHueRepository"