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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
class SplashViewModel : ViewModel() {

    //-------------------------
    //  class data
    //-------------------------

    private val _bridgeStatus = MutableStateFlow(PhilipsHueBridgeStatus.BRIDGE_UNINITIALIZED)
    /** Holds the state of the Philips Hue Bridge. This is the main variable here. */
    var bridgeStatus = _bridgeStatus.asStateFlow()

    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()

    private val _philipsHueIpSet = MutableStateFlow(false)
    /** will be false if we do not have an ip for the philips hue bridge */
    var philipsHueIpSet = _philipsHueIpSet.asStateFlow()

    private val _philipsHueTokenSet = MutableStateFlow(false)
    /** will be false if we need to get the token for the philips hue bridge */
    var philipsHueTokenSet = _philipsHueTokenSet.asStateFlow()

    private val _philipsHueBridgeResponding = MutableStateFlow<Boolean?>(null)
    val philipsHueBridgeResponding = _philipsHueBridgeResponding.asStateFlow()

    private val _philipsHueTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /** when true, all philips hue tests are complete */
    var philipsHueTestStatus = _philipsHueTestsStatus.asStateFlow()


    private val _iotTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /**
     * When true, all Internet of Things tests are complete.
     * Results will be in the appropriate varables.
     */
    var iotTestsStatus = _iotTestsStatus.asStateFlow()


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

            //------------
            // 2.  check philips hue system
            //
            delay(1000)
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

    //-------------------------
    //  private functions
    //-------------------------

    /**
     * Just pauses a bit for the splash screen animation to look nice.
     */
//    init {
//        checkPhilipsHue(ctx)
//    }


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

        // check token
        val token = getBridgeToken(ctx)
        if (token == null) {
            // no token
            _philipsHueTokenSet.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }

        // check bridge is responding
        if (doesBridgeRespondToIp(ctx) == false) {
            // not responding to connection request
            _philipsHueBridgeResponding.value = false
            _philipsHueTestsStatus.value = TestStatus.TEST_BAD
            return
        }

        // passed all tests--must be good to go!
        _philipsHueTestsStatus.value = TestStatus.TEST_GOOD
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

private const val TAG = "SplashViewModel"