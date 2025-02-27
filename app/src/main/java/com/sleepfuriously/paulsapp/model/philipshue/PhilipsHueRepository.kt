package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
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

    private val _bridgesSet = MutableStateFlow<Set<PhilipsHueBridgeInfo>>(setOf())

    /** the complete list of all the bridges and associated data */
    val bridgesSet = _bridgesSet.asStateFlow()


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
            model.bridgeFlowSet.collectLatest {
                Log.d(
                    TAG,
                    "collecting bridgeFlowSet from bridgeModel. change = $it, hash = ${
                        System.identityHashCode(it)
                    }"
                )

                // rebuilding a copy of the bridge set
                val newBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
                it.forEach { bridge ->
                    newBridgeSet.add(bridge)
                    Log.d(TAG, "Setting bridge for flow:")
                    bridge.rooms.forEach { room ->
                        Log.d(
                            TAG,
                            "   room ${room.name}, on = ${room.on}, bri = ${room.brightness}"
                        )
                    }
                }
                // producing flow
                _bridgesSet.update { newBridgeSet }
            }
        }

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

    //-------------------------------
    //  delete
    //-------------------------------

    /**
     * Tell the model to remove this bridge.  Permanently.
     */
    /**
     * Tell the model to remove this bridge.  Permanently.
     */
    fun deletePhilipsHueBridge(bridgeId: String): Boolean {
        return model.deleteBridge(bridgeId)
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