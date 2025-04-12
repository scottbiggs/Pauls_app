package com.sleepfuriously.paulsapp.model.philipshue

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeStorage
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone

/**
 * Separates the bridge from the Philips Hue model.
 *
 * Usage
 *  - Instantiate with info that this can use to find the bridge (just an ID,
 *  an ip, and token are needed).  This will try to contact the bridge and get
 *  all relevant data (rooms, scenes, zones).
 *
 *  - Receive flow.  Once the bridge is setup, it will start a stateflow, which
 *  will notify all listeners about the state of the bridge.  Any changes will
 *  be sent through the flow.  Null means that there is no bridge (yet).
 *
 *  - Connect SSE.  This tells the bridge to start receive server-sent events
 *  to this class.  It's done automatically the first time if initialization was
 *  successful.  Use [bridge] flow to see if the bridge is currently connected.
 *
 * Communication with the outside is done through flows.
 *
 * @param   bridgeId            The id used to differentiate this bridge.
 *                              Note that this is the v2 id (long) and NOT
 *                              the v1 (short) bridge id.
 *
 * @param   bridgeIpAddress     Ip of this bridge on the local wifi network
 *
 * @param   bridgeToken         The token necessary to communicate with this bridge.
 *                              Also called username in the PH docs.
 *
 * @param   coroutineScope      Used for all the suspend functions within this class
 */
class PhilipsHueBridgeModel(
    private val bridgeId: String,
    private val bridgeIpAddress: String,
    private val bridgeToken: String,
    private val coroutineScope: CoroutineScope
) {

    //-------------------------------------
    //  flow data
    //-------------------------------------

    private val _bridge = MutableStateFlow<PhilipsHueBridgeInfo?>(null)
    /** Flow for this bridge. Collectors will be notified of changes to this bridge. */
    val bridge = _bridge.asStateFlow()


    //-------------------------------------
    //  class data
    //-------------------------------------

    //-------------------------------------
    //  constructors & initializers
    //-------------------------------------

    /**
     * Alternate constructor:
     *
     * @param   bridgeInfo          Takes a [PhilipsHueBridgeInfo] and turns it
     *                              into this Model.
     *
     * @param   coroutineScope      Used for all the suspend functions within this class
     */
    operator fun invoke(
        bridgeInfo: PhilipsHueBridgeInfo,
        coroutineScope: CoroutineScope
    ) : PhilipsHueBridgeModel {
        return PhilipsHueBridgeModel(
            bridgeId = bridgeInfo.id,
            bridgeIpAddress = bridgeInfo.id,
            bridgeToken = bridgeInfo.token,
            coroutineScope = coroutineScope
        )
    }

    /**
     * Called every time this class is instantiated.  Does NOT do the work of
     * retrieving info from the long-term storage (that stuff is done in [PhilipsHueBridgeStorage]).
     */
    init {
        coroutineScope.launch(Dispatchers.IO) {

            // grab the bridge data
            _bridge.update {
                val tmpBridge = loadAllBridgeDataFromApi()

                // start sse (but only if the bridge is actually here!)
                if (tmpBridge != null) {
                    connectSSE()
                }

                // finally return the new bridge data structure so it can flow
                tmpBridge
            }
        }
    }

    //-------------------------------------
    //  public functions
    //-------------------------------------

    /**
     * Connects this app to the bridge so that it can receive updates from
     * the bridge itself.
     *
     * side effects
     *  The bridge will start receiving server-sent events from the bridge
     *
     * @return  True if successful. False if something went wrong (bridge not available, etc)
     */
    fun connectSSE() : Boolean {
        TODO()
    }

    /**
     * Stop the bridge from receiving server-sent events from the bridge.
     *
     * side effects
     *  The bridge's
     */
    fun disconnectSSE() {
        TODO()
    }

    //-------------------------------------
    //  private functions
    //-------------------------------------

    /**
     * Asks the bridge all about itself.  The data is then converted and stored
     * for quick retrieval.
     *
     * If there's an error, a message is spit out, and null is returned.
     *
     * @param   connected       Currently receiving sse from bridge. Defaults to False.
     *
     * Note:    This is a stand-alone function.  Doesn't involve flows or save
     *          anything to this class (no side effects).
     */
    private suspend fun loadAllBridgeDataFromApi(
        connected: Boolean = false
    ) : PhilipsHueBridgeInfo? {

        // is this bridge active?
        val active = isBridgeActive(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )
        if (active == false) {
            Log.v(TAG, "getDataFromBridgeApi(), but bridge is not responding.")
            return PhilipsHueBridgeInfo(
                id = bridgeId,
                labelName = "",
                ipAddress = bridgeIpAddress,
                token = bridgeToken,
                active = false,
                connected = false
            )
        }

        // get the info from bridge and check for errors
        val v2Bridge = PhilipsHueBridgeApi.getBridge(bridgeIpAddress, bridgeToken)
        val apiErr = v2Bridge.getError()
        if (apiErr.isNotEmpty()) {
            // yep, there's an error here
            Log.d(TAG, "getDataFromBridgeApi() error: $apiErr")
            return null
        }

        // convert to our bridge data struct
        val bridgeInfo = PhilipsHueDataConverter.convertV2Bridge(
            v2Bridge = v2Bridge.getData(),
            bridgeIp = bridgeIpAddress,
            token = bridgeToken,
            active = true,
            connected = connected,
        )

        // now the big things (rooms, zones, scenes)
        bridgeInfo.rooms = loadRoomsFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )?.toMutableSet() ?: mutableSetOf()

        bridgeInfo.scenes = loadScenesFromApi()?.toMutableList() ?: mutableListOf()
        bridgeInfo.zones = loadZonesFromApi()?.toMutableList() ?: mutableListOf()

        return bridgeInfo
    }

    /**
     * Asks the bridge to get all the rooms.  Caller should know what to do with
     * this data.
     *
     * On error, returns null (probably the bridge is not connected).
     * Empty list means that there simply aren't any rooms.
     */
    private suspend fun loadRoomsFromApi(
        bridgeIp: String,
        bridgeToken: String
    ) : List<PhilipsHueRoomInfo>? {

        val bridgeRooms = PhilipsHueBridgeApi.getAllRoomsFromApi(
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        )

        if (bridgeRooms.errors.isNotEmpty()) {
            Log.e(TAG, "loadRoomsFromApi() error getting rooms:")
            Log.e(TAG, "   err msg = ${bridgeRooms.errors[0].description}")
            return null
        }

        return PhilipsHueDataConverter.convertV2RoomAll(
            phV2Rooms = bridgeRooms,
            bridgeIp = bridgeIp,
            bridgeToken = bridgeToken
        ).toList()
    }

    /**
     * Finds all the scenes from the bridge.  Returns null if bridge is not
     * responding.  Could be an empty list.
     */
    private suspend fun loadScenesFromApi() : List<PHv2Scene> {
        val v2AllScenes = PhilipsHueBridgeApi.getAllScenesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        return PhilipsHueDataConverter.convertV2ScenesAll(v2AllScenes)
    }

    /**
     * Asks the bridge to get all its zones.  Will return null if the bridge
     * isn't working.  Note that this could be an empty list.
     */
    private suspend fun loadZonesFromApi() : List<PHv2Zone> {
        val v2AllZones = PhilipsHueBridgeApi.getAllZonesFromApi(
            bridgeIp = bridgeIpAddress,
            bridgeToken = bridgeToken
        )

        return PhilipsHueDataConverter.convertV2ZonesAll(v2AllZones)
    }

}

//-------------------------------------
//  constants
//-------------------------------------

private const val TAG = "PhilipsHueBridgeModel"