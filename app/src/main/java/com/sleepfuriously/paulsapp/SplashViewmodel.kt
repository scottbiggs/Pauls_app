package com.sleepfuriously.paulsapp

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.model.isConnectivityWifiWorking
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeStatus
import com.sleepfuriously.paulsapp.model.philipshue.doesBridgeRespondToIp
import com.sleepfuriously.paulsapp.model.philipshue.getBridgeIPStr
import com.sleepfuriously.paulsapp.model.philipshue.getBridgeToken
import com.sleepfuriously.paulsapp.model.philipshue.setBridgeIpStr
import com.sleepfuriously.paulsapp.model.philipshue.testBridgeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * ViewModel for the startup splash screen.  While this is displaying,
 * many initializations take place.
 *
 * 1. Wifi
 *   a. is wifi turned on for the device?
 *   b. is wifi connected?
 *
 * 2. IoT - Philips Hue
 *   a. do we have a token?
 *   b. does token work?
 *
 * 3. todo - all other IoT devices
 */
class SplashViewmodel : ViewModel() {

    //-------------------------
    //  class data
    //-------------------------

    private val _bridgeStatus = MutableStateFlow(PhilipsHueBridgeStatus.BRIDGE_UNINITIALIZED)
    /** Holds the state of the Philips Hue Bridge. This is the main variable here. */
    var bridgeStatus = _bridgeStatus.asStateFlow()

    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()

    private val _philipsHueIpSet = MutableStateFlow<Boolean?>(null)
    /** Set to true if we have an ip for the philips hue bridge. Null means not checked yet. */
    var philipsHueIpSet = _philipsHueIpSet.asStateFlow()

    private val _philipsHueIpStr = MutableStateFlow("")
    /** If the ip is set (see [philipsHueIpSet]), then this will hold the ip string. */
    var philipsHueIpStr = _philipsHueIpStr.asStateFlow()

    private val _philipsHueTokenSet = MutableStateFlow<Boolean?>(null)
    /** will be false if we need to get the token for the philips hue bridge. Null until tested. */
    var philipsHueTokenSet = _philipsHueTokenSet.asStateFlow()

    private val _philipsHueTokenStr = MutableStateFlow("")
    /** If the token is set (see [philipsHueTokenSet]) this will hold the token. */
    var philipsHueTokenStr = _philipsHueTokenStr.asStateFlow()

    private val _philipsHueBridgeIpWorking = MutableStateFlow<Boolean?>(null)
    /** Set to True if the bridge responds to queries, false otherwise.  Null means not tested yet. */
    val philipsHueBridgeIpWorking = _philipsHueBridgeIpWorking.asStateFlow()

    private val _philipsHueBridgeTokenWorks = MutableStateFlow<Boolean?>(null)
    /** Will be set to TRUE after a successful test of the token on the bridge. Null before testing. */
    val philipsHueBridgeTokenWorks = _philipsHueBridgeTokenWorks.asStateFlow()

    private val _philipsHueTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /** when true, all philips hue tests are complete */
    var philipsHueTestStatus = _philipsHueTestsStatus.asStateFlow()


    private val _iotTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /**
     * When true, all Internet of Things tests are complete.
     * Results will be in the appropriate varables.
     */
    var iotTestsStatus = _iotTestsStatus.asStateFlow()


    private val _initializingBridgeState = MutableStateFlow(BridgeInitStates.NOT_INITIALIZING)
    /** Tells what steps are currently taking place when  we are initializing the bridge */
    var initializingBridgeState = _initializingBridgeState.asStateFlow()

    private val _lastIpValid = MutableStateFlow<Boolean?>(null)
    /** Was the last entered ip a valid entry?  null means no entry yet */
    var lastIpValid = _lastIpValid.asStateFlow()


    //-------------------------
    //  public functions
    //-------------------------

    /**
     * Runs all the initalization tests of the IoT devices.
     *
     * side effects
     *   [iotTestsStatus]      set to true when this is done
     */
    fun checkIoT(ctx: Context) {

        _iotTestsStatus.value = TestStatus.TESTING
        var allTestsSuccessful = true       // start optimistically

        // launch off the main thread, just in case things take a while
        viewModelScope.launch(Dispatchers.IO) {

            // fixme
            delay(2000)

            //------------
            // 1.  check wifi
            //
            _wifiWorking.value = isConnectivityWifiWorking(ctx)
            if (_wifiWorking.value == false) {
                // abort testing--no point doing more tests without wifi
                // Caution: this will skip the succeeding tests to never happen,
                // causing them to never indicate that they completed.
                allTestsSuccessful = false
                return@launch
            }

            // fixme
            delay(2000)

            //------------
            // 2.  check philips hue system
            //
            checkPhilipsHue(ctx)
            allTestsSuccessful =
                philipsHueTestStatus.value == TestStatus.TEST_GOOD

            //------------
            // 3.  todo: test other IoT devices
            //
            delay(2000)

        }.invokeOnCompletion {
            // Note: this code will be called when the above coroutine exits,
            // even if by a return statement.

            // signal tests complete
            Log.d(TAG, "completion: allTestsSuccessful = $allTestsSuccessful")
            _iotTestsStatus.value =
                if (allTestsSuccessful) TestStatus.TEST_GOOD
                else TestStatus.TEST_BAD
        }
    }

    /**
     * Begins the logical part of initializing the philips hue bridge.
     *
     * side effects:
     *      initializingBridgeState is set to STAGE_1
     */
    fun beginInitializePhilipsHue(ctx: Context) {
        _initializingBridgeState.value = BridgeInitStates.STAGE_1
    }

    /**
     * Call this when the new IP for the philips hue bridge is known.
     * This completes stage one of the bridge initialization.
     *
     * side effects:
     *      initializingBridgeState is set to STAGE_2
     */
    fun setPhilipsHueIp(ctx: Context, newIp: String) {

        if (newIp.isNotBlank() && newIp.isValidBasicIp()) {
            _lastIpValid.value = true
            viewModelScope.launch(Dispatchers.IO) {
                setBridgeIpStr(ctx, newIp)
                _initializingBridgeState.value = BridgeInitStates.STAGE_2
            }
        }
        else {
            _lastIpValid.value = false
        }
    }

    /**
     * This resets the last valid ip to a null state (as if it had never
     * been entered).
     */
    fun setLastValidIpNull() {
        _lastIpValid.value = null
    }

    //-------------------------
    //  private functions
    //-------------------------

    /**
     * Checks the status of the Philips Hue bridge.  This consists of:
     *  1. Do we have the philips hue IP? Yes -> [philipsHueIpSet] = true
     *      - No -> [philipsHueIpState] = false.
     *        IP needs to be retrieved from the user.
     *
     *  2. Do we have the philips hue token? Yes -> [philipsHueTokenSet] = true
     *      - No -> signal that a token request is needed.  This is
     *        done by making sure that [philipsHueTokenState] is false.
     *
     * After all these checks are complete
     *
     * side effects
     *      - as described above
     */
    private suspend fun checkPhilipsHue(ctx: Context) {

        _philipsHueTestsStatus.value = TestStatus.TESTING

        // check ip
        val bridgeIp = getBridgeIPStr(ctx)
        if (bridgeIp == null) {
            // no bridge ip. save this info and exit
            _philipsHueIpSet.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }
        _philipsHueIpStr.value = bridgeIp
        _philipsHueIpSet.value = true

        // fixme
        delay(2000)

        // check bridge is responding
        if (doesBridgeRespondToIp(ctx) == false) {
            // not responding to connection request
            _philipsHueBridgeIpWorking.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }
        _philipsHueBridgeIpWorking.value = true

        // fixme
        delay(2000)

        // check token
        val token = getBridgeToken(ctx)
        if (token == null) {
            // no token
            _philipsHueTokenSet.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }
        _philipsHueTokenStr.value = token
        _philipsHueTokenSet.value = true

        // fixme
        delay(2000)

        // check that token works
        val tokenWorks = testBridgeToken(ctx)
        if (tokenWorks == false) {
            _philipsHueBridgeTokenWorks.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }
        _philipsHueBridgeTokenWorks.value = true

        // fixme
        delay(2000)

        // passed all tests--must be good to go!
        _philipsHueTestsStatus.value = TestStatus.TEST_GOOD
    }


    /**
     * Determines if the given string is a valid basic ip.
     * In other words, does it have the basic format #.#.#.#
     * and is not a domain name, nor does it have a prefix.
     *
     * Note that this is an extension function of String.  :)
     */
    private fun String.isValidBasicIp() : Boolean {
        if (this.isBlank()) {
            return false
        }

        // only allow numbers and dots
        if (this.contains(Regex("[^0-9.]")) == true) {
            return false
        }

        // there must be exactly 3 periods
        if (this.count { ".".contains(it) } != 3) {
            return false
        }

        // get list of numbers
        val numList = this.split(".")

        // there should be 4 items
        if (numList.size != 4) {
            return false
        }

        // Each item should exist (ie not be blank or null).
        // Also, each should be a number:  0 <= n <= 255
        numList.forEach { s ->
            if (s.isEmpty()) {
                return false
            }
            val n = s.toInt()
            if ((n < 0) || (n > 255)) {
                return false
            }
        }

        // that's all I can think of!
        return true
    }

}

//-------------------------
//  classes & enums
//-------------------------

enum class TestStatus {
    /** test hos not taken place yet, nor has it been started */
    NOT_TESTED,
    /** currently testing */
    TESTING,
    /** test complete and successful */
    TEST_GOOD,
    /** test complete, but failed */
    TEST_BAD
}


enum class BridgeInitStates {
    NOT_INITIALIZING,
    STAGE_1,
    STAGE_2,
    STAGE_3
}

private const val TAG = "SplashViewModel"