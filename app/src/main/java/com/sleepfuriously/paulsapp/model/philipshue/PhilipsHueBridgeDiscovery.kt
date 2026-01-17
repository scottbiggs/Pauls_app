package com.sleepfuriously.paulsapp.model.philipshue

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.sleepfuriously.paulsapp.utils.convertRawBytesToIpAddress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Use this to auto-find bridges that are connected to the current network.
 * The [NsdManager] is the library used for this purpose.
 *
 * USAGE:
 *  Just instantiate this class.  When 750 ms have passed [TIME_TO_WAIT]
 *  the data in [foundBridges] will be ready.  You can then let this go
 *  out of scope--it'll clean itself up.  Pretty simple.
 *
 *  todo: this should just be a free suspended function that waits 750ms and
 *      then returns the bridges.  VERY SIMPLE.
 */
class PhilipsHueBridgeDiscovery(ctx: Context) {

    //---------------------------
    //  data
    //---------------------------

    /**
     * The current bridges that this class has found.  Will be safe to use after
     * [TIME_TO_WAIT] milliseconds.
     */
    var foundBridges = emptyList<String>()
        private set

    private var nsdMgr: NsdManager =
        ctx.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var mDiscoveryListener: DiscoveryListener? = null

    /**
     * I think this is to identify THIS device so that it isn't included in the mDNS result.
     * Since I don't register this app that way, this isn't needed.
     */
    private var mServiceName: String = "I am not used"

    /** fixme: what does this do? */
    private var chosenServiceInfo: NsdServiceInfo? = null

    //---------------------------
    //  functions
    //---------------------------

    /**
     * Your one call to find all the Philips Hue bridges on the local network!
     * Instantiate and then call this function.  Wait for the result, and that's
     * it.  You can then let this fall out of scope safely.
     *
     * WARNING:     This can take up to a second to return, so do not call
     *              on the Main UI thread.
     *
     *              You should NOT call this more than once every 2 seconds!
     *              This violates IETF requirements for playing nicely in a
     *              network.
     *
     * side effects
     *  [foundBridges]  Will also hold the result
     *
     * @return      A list of ip address of the various bridges that respond
     *              to the mDNS request.
     *              Null means that we haven't waited long enough between calls.
     */
    suspend fun getAllBridges() : List<String> {

        foundBridges = emptyList()

        initializeNsd()

        // now to start the discovery
        startDiscovery()      // let the code that already works do its thing

        delay(750)      // wait for all the bridges to be found

        stopDiscovery()           // discovery has run its course, so turn it off

        // and return what we've found so far. Done!
        return foundBridges
    }


    fun initializeNsd() {
        initializeDiscoveryListener()
    }


    fun startDiscovery() {
        Log.d(TAG, "discoverServices()")
        nsdMgr.discoverServices(
            SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener
        )
        Log.d(TAG, "discoverServices() done.")
    }


    fun stopDiscovery() {
        nsdMgr.stopServiceDiscovery(mDiscoveryListener)
    }


    //---------------------------
    //  private functions
    //---------------------------

    /**
     * todo - dox
     */
    private fun initializeDiscoveryListener() {
        Log.i(TAG, "initializing discovery listener!")
        mDiscoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success: $service")
                if (service.serviceType != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
                } else if (service.serviceName == mServiceName) {
                    Log.d(TAG, "Same machine: $mServiceName")
                } else {
                    Log.d(TAG, "Trying to resolve $service")
                    nsdMgr.resolveService(
                        service,
                        MyResolveListener({ newBridgeIp ->
                            foundBridges += newBridgeIp
                        }))
                }
            }

            override fun onServiceLost(service: NsdServiceInfo?) {
                Log.e(TAG, "service lost$service")
                if (chosenServiceInfo == service) {
                    chosenServiceInfo = null
                }
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdMgr.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdMgr.stopServiceDiscovery(this)
            }
        }
    }


    //---------------------------
    //  classes
    //---------------------------

    /**
     * @param   onBridgeFound       Param will contain the IP of a found bridge.
     */
    private class MyResolveListener(
        private val onBridgeFound: (newBridgeIp: String) -> Unit
    ) : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            Log.e(TAG, "onResolveFailed. errorCode = $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            Log.d(TAG, "onServiceResolved() - serviceInfo = $serviceInfo")
            val ipStr = convertRawBytesToIpAddress(serviceInfo?.hostAddresses[0]?.address ?: byteArrayOf())
            onBridgeFound(ipStr)
        }
    }


}

//---------------------------
//  constants
//---------------------------

/** used to identify philips hue bridges on the network */
private const val SERVICE_TYPE: String = "_hue._tcp."

/** Number of milliseconds for this class to reveal all the bridges. */
const val TIME_TO_WAIT = 750

private const val TAG = "PhilipsHueBridgeDiscovery"