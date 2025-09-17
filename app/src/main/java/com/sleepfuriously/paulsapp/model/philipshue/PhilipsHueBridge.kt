package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi.getAllDevicesFromApi
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi.getDeviceIndividualFromApi
import com.sleepfuriously.paulsapp.model.philipshue.json.BRIDGE
import com.sleepfuriously.paulsapp.model.philipshue.json.DEVICE
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2ResourceDevicesAll
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Zone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This is the main bridge class. It should be instantiated
 * for each bridge. It'll take care of the initializations
 * only when initialize is called, which can be called
 * at any later time to reset the bridge data.
 *
 * A [PhilipsHueBridgeInfo] is produced as a flow so
 * observers can detect changes.
 *
 * Other information that is produced in flows includes:
 *  - scenes
 *  - zones
 *  - rooms
 *  - devices
 *  - lights
 *  - whatever Paul needs
 *
 *  Long-term storage of bridge data is NOT included here.
 */
class PhilipsHueBridge(
    private val bridgeIpStr: String,
    private val bridgeToken: String,
    private val ctx: Context,
    coroutineScope: CoroutineScope
) {

    //---------------------------------------
    //  data
    //---------------------------------------

    /** true during initialization */
    private var initializing = false

    /**
     * The data for this bridge
     */
    private val _bridgeInfo = MutableStateFlow<PhilipsHueBridgeInfo?>(null)
    /** visible part of this bridge's data */
    val bridgeInfo = _bridgeInfo.asStateFlow()

    private val _devices = MutableStateFlow<PHv2ResourceDevicesAll?>(null)
    /** master list of all devices on this bridge */
    val devices = _devices.asStateFlow()

    private val _rooms = MutableStateFlow<List<PhilipsHueRoomInfo>>(listOf())
    val rooms = _rooms.asStateFlow()

    private val _scenes = MutableStateFlow<List<PHv2Scene>>(listOf())
    val scenes = _scenes.asStateFlow()

    private val _zones = MutableStateFlow<List< PHv2Zone>>(listOf())
    val zones = _zones.asStateFlow()




    //---------------------------------------
    //  public functions
    //---------------------------------------

    /**
     * Call at any time to reset the bridge.
     *
     * MUST BE CALLED BEFORE ANYTHING ELSE!
     *
     * NOTE:
     *      Does NOT bother with sse!
     *
     * side effects
     *  [bridgeInfo]    completely updated with fresh data
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        initializing = true

        //----------------
        //     For each active bridge, get rooms for that bridge
        //     (includes control data for the room). Convert info
        //     to interpreted data if needed.
        //

        val isActive = isBridgeActive(bridgeIpStr = bridgeIpStr, bridgeToken = bridgeToken)
        var bridgeId = ""
        var id = ""
        var humanName = EMPTY_STRING

        if (isActive) {
            val v2bridge = PhilipsHueBridgeApi.getBridgeApi(
                bridgeIpStr = bridgeIpStr,
                token = bridgeToken
            )
            if (v2bridge.hasData() == false) {
                Log.e(TAG, "error getting v2bridge info. error = ${v2bridge.getError()}")
                return@withContext null
            }
            id = v2bridge.getId()
            bridgeId = v2bridge.getDeviceName()

            // human name is more difficult
            humanName = PhilipsHueDataConverter.getBridgeUsername(
                bridgeIp = bridgeIpStr,
                bridgeToken = bridgeToken
            )
        }

        _bridgeInfo.value = PhilipsHueBridgeInfo(
            v2Id = id,
            bridgeId = bridgeId,
            ipAddress = bridgeIpStr,
            token = bridgeToken,
            active = isActive,
            connected = false,
            humanName = humanName
        )
        Log.d(TAG,"initialize() - bridge is now = $bridgeInfo")

        TODO("sse needs implementing")

        initializing = false

    }


    //---------------------------------------
    //  private functions
    //---------------------------------------

    init {
        coroutineScope.launch {
            initialize()
        }
    }
}

//---------------------------------------
//  constants
//---------------------------------------

private const val TAG = "PhilipsHueBridge"