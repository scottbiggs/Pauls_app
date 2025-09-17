package com.sleepfuriously.paulsapp

import android.content.Context
import android.util.Log
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeApi
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeStorage
import com.sleepfuriously.paulsapp.model.philipshue.isBridgeActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This class handles the handshake and startup
 * routines for the Philips Hue bridges.
 *
 * @param   bridgeID    must be valid
 */
class PhilipsHueInitializer(
    private val bridgeID: String,
    private val ctx: Context
) {
    /**
     * todo:  see if these function calls repeat work already done
     *
     * Call this function each time to initialize. This will create a
     * [PhilipsHueBridgeInfo] for the given bridge id. A good way
     * to get the bridge info if you don't already have it.
     *
     * SUGGESTION:
     *  Good to do after this:
     *  [getDevices]
     *  [getRooms]
     *  [getScenes]
     *  [getZones]
     *
     * NOTE
     *  This does NOT set up the server sent events!
     *
     * @return  returns a copy of the bridge or null on failure
     */
    suspend fun initialize() : PhilipsHueBridgeInfo? = withContext(Dispatchers.IO) {

        //----------------
        //  1. Load bridge info from long-term storage - todo not necessary?
        //
        //  2. For each active bridge, get rooms for that bridge
        //     (includes control data for the room). Convert info
        //     to interpreted data if needed.
        //
        //  3. Setup callbacks (events) for the bridges so we can be
        //     updated when anything changes.
        //

        withContext(Dispatchers.IO) {

            // load IP and token from prefs
            val ip = PhilipsHueBridgeStorage.loadBridgeIp(bridgeId = bridgeID, ctx = ctx)
            val token = PhilipsHueBridgeStorage.loadBridgeToken(bridgeId = bridgeID, ctx = ctx)

            if (ip.isEmpty() || token.isEmpty()) {
                Log.w(TAG, "unable to find ip or token for bridge id = $bridgeID")
                return@withContext null
            }

            val isActive = isBridgeActive(ip, token)
            var bridgeId = ""
            if (isActive) {
                val v2bridge = PhilipsHueBridgeApi.getBridgeApi(
                    bridgeIpStr = ip,
                    token = token
                )
                if (v2bridge.hasData() == false) {
                    Log.e(TAG, "error getting v2bridge info. error = ${v2bridge.getError()}")
                    return@withContext null
                }
                bridgeId = v2bridge.getDeviceName()
            }

            val bridge = PhilipsHueBridgeInfo(
                v2Id = bridgeID,
                bridgeId = bridgeId,
                ipAddress = ip,
                token = token,
                active = isActive,
                connected = false,
                humanName = "foo"
            )

            Log.d(TAG,"returning bridge $bridge")
            return@withContext bridge
        }
    }
/*
    /**
     * Returns a list of all the devices on the given bridge.
     */
    suspend fun getDevices(bridgeInfo: PhilipsHueBridgeInfo) : List<PHv2Device> {

    }

    /**
     * Returns a list of all the rooms on the given bridge.
     */
    suspend fun getRooms(bridgeInfo: PhilipsHueBridgeInfo) : List<PHv2Device> {

    }

    /**
     * Returns a list of all the scenes on the given bridge.
     */
    suspend fun getScenes(bridgeInfo: PhilipsHueBridgeInfo) : List<PHv2Device> {

    }

    /**
     * Returns a list of all the zones on the given bridge.
     */
    suspend fun getZones(bridgeInfo: PhilipsHueBridgeInfo) : List<PHv2Device> {

    }
*/
}

private const val TAG = "PhilipsHueInitializer"