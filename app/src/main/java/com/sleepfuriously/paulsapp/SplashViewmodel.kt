package com.sleepfuriously.paulsapp

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.model.isConnectivityWifiWorking
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeUtils
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
class SplashViewmodel(ctx: Context) : ViewModel() {

    //-------------------------
    //  class data
    //-------------------------

    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()


    private val _philipsHueTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /** when true, all philips hue tests are complete */
    var philipsHueTestStatus = _philipsHueTestsStatus.asStateFlow()


    private val _iotTesting = MutableStateFlow(false)
    /** Will be true only while tests are running */
    var iotTesting = _iotTesting.asStateFlow()


//    private val _iotTestsStatus = MutableStateFlow(TestStatus.NOT_TESTED)
//    /**
//     * When true, all Internet of Things tests are complete.
//     * Results will be in the appropriate varables.
//     */
//    var iotTestsStatus = _iotTestsStatus.asStateFlow()


    private val _addNewBridgeState = MutableStateFlow(BridgeInitStates.NOT_INITIALIZING)
    /** Tells what steps are currently taking place when we are adding a new bridge */
    var addNewBridgeState = _addNewBridgeState.asStateFlow()


    private val _philipsHueBridges = MutableStateFlow<Set<PhilipsHueBridgeInfo>>(setOf<PhilipsHueBridgeInfo>())
    /** Holds the list of all bridges */
    var philipsHueBridges = _philipsHueBridges.asStateFlow()


    /** access to the philips hue bridge and all that stuff that goes with it */
    private lateinit var bridgeUtils : PhilipsHueBridgeUtils

    /** This variable holds a new bridge.  Once it's filled in, it'll be added to the list */
    var newBridge: PhilipsHueBridgeInfo? = null
        private set

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

        _iotTesting.value = true
//        _iotTestsStatus.value = TestStatus.TESTING

        var allTestsSuccessful = true       // start optimistically

        // launch off the main thread, just in case things take a while
        viewModelScope.launch(Dispatchers.IO) {

            // fixme
//            delay(1000)

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
//            delay(1000)

            //------------
            // 2.  check philips hue system
            //
            checkPhilipsHue(ctx)
            allTestsSuccessful =
                philipsHueTestStatus.value == TestStatus.TEST_GOOD

            //------------
            // 3.  todo: test other IoT devices
            //
//            delay(2000)

        }.invokeOnCompletion {
            // Note: this code will be called when the above coroutine exits,
            // even if by a return statement.

            // signal tests complete
            Log.d(TAG, "completion: allTestsSuccessful = $allTestsSuccessful")
            _iotTesting.value = false
//            _iotTestsStatus.value =
//                if (allTestsSuccessful) TestStatus.TEST_GOOD
//                else TestStatus.TEST_BAD
        }
    }

    /**
     * Call this to change the IP for an EXISTING philips hue bridge.
     * If you want to set a new bridge, call [AddPhilipsHueBridgeIp]
     *
     * NOTE
     *  No testing is done for the bridge in question (other than that
     *  it exists in our list).  The new ip could work, or it might not--this
     *  function doesn't care (because we might need to change an ip to a
     *  bridge that isn't around, or isn't turned on, etc.).
     *
     * @param   bridgeId        Id of the bridge to set the ip for
     *
     * @param   newIp           A new IP string.  This function will check
     *                          for proper formatting.
     *
     * @return      True if all went well.
     *              False if the bridge could not be found.  Nothing is done.
     *
     * side effects:
     *      The bridge will be permanently changed to hold the new id
     */
    fun setPhilipsHueIp(bridgeId: String, newIp: String) : Boolean {

        if (isValidBasicIp(newIp)) {
            bridgeUtils.saveBridgeIpStr(bridgeId, newIp)
            return true
        }
        else {
            Log.e(TAG, "bad ip format in setPhilipsHueIp(bridgeId = $bridgeId, newIp = $newIp")
            return false
        }
    }


    //-------------------------
    //  add bridge functions
    //-------------------------

    /**
     * Begins the logical part of initializing the philips hue bridge.
     *
     * side effects:
     *      - initializingBridgeState is set to STAGE_1
     *      - newBridge is initialized with a unique id
     */
    fun beginAddPhilipsHueBridge() {
        _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP
        newBridge = PhilipsHueBridgeInfo(id = bridgeUtils.getNewId())
        Log.d(TAG, "newBridge is created, id = ${newBridge?.id}")
    }

    /**
     * Call this to tell the viewmodel that the user has typed in an ip for
     * their bridge.
     *
     * @param   newIp       The string that the user typed in.  This function
     *                      do error checking (correcting if possible).
     *
     * side effects
     *      _addNewBridgeState      changed to reflect the addition of the
     *                              ip or an error.
     *
     *      newBridge               may have a valid ip (if user typed it in right)
     */
    fun AddPhilipsHueBridgeIp(newIp: String) {

        // this includes a test and may take a while
        viewModelScope.launch(Dispatchers.IO) {

            // check format
            if (isValidBasicIp(newIp) == false) {
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT
                return@launch
            }

            // is there actually a bridge there?
            if (bridgeUtils.doesBridgeRespondToIp(newIp) == false) {
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP
                return@launch
            }

            // The ip looks good.  Save it and signal to move on.
            // (yes, I want to crash if newBridge is null)
            newBridge!!.ip = newIp
            _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
        }
    }

    /**
     * Call this during the bridge initialization when an error message has been
     * displayed.  This will signal that the viewmodel no longer has to maintain
     * an error state.
     *
     * side effects
     *      - initializingBridgeState will be set to the previous state (before
     *      the error state.  If it is not in an error state, then an error is
     *      logged and nothing is done.
     */
    fun bridgeAddErrorMsgIsDisplayed() {
        when (_addNewBridgeState.value) {
            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP,
            BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT -> {
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP
            }

            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE -> {
                _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
            }

            else -> {
                Log.e(TAG, "error in bridgeInitErrorMsgDisplayed()!  State = ${_addNewBridgeState.value} is not an error state!")
            }
        }
    }


    //-------------------------
    //  private functions
    //-------------------------

    /**
     * Checks the status of the Philips Hue bridge.  For each bridge that
     * is known (already saved):
     *  1. Do we have the philips hue IP?
     *
     *  2. Do we have the philips hue token?
     *
     *  3. Is the bridge responding to the token?
     *
     * After all these checks are complete we're done checking the
     * Philips Hue bridges.
     *
     * side effects
     *      - as described above
     */
    private suspend fun checkPhilipsHue(ctx: Context) {

        // Get the bridge utils started.  This will go through
        // its test routines during initialization.
        _philipsHueTestsStatus.value = TestStatus.TESTING
        bridgeUtils = PhilipsHueBridgeUtils(ctx)

        // Get all the bridges.  If there are none, signal
        // this condition.
        _philipsHueBridges.value = bridgeUtils.getAllActiveBridges()
        if (philipsHueBridges.value.isEmpty()) {
            // todo: the fact that the bridges list is empty should suffice
            return
        }


        // check each bridge one by one to see if its info is current
        // and active.
        for (bridge in _philipsHueBridges.value) {

            // does this bridge have an ip?
            val ip = bridgeUtils.getBridgeIPStr(bridge.id)
            if (ip.isNullOrBlank()) {
                bridge.active = false
                continue
            }

            // check bridge is responding
            if (bridgeUtils.doesBridgeRespondToIp(bridge.id) == false) {
                bridge.active = false
                continue
            }

            // At this point, the bridge is definitely active.  We may or
            // may not have a token that works though!
            bridge.active = true

            // Do we have a token?
            val token = bridgeUtils.getBridgeToken(bridge.id)
            if (token.isNullOrBlank()) {
                continue
            }

            // check that token works
            val tokenWorks = bridgeUtils.testBridgeToken(bridge.id, token)
            if (tokenWorks == false) {
                continue
            }
        }

        // All tests complete.  The results are stored within each
        // bridge in the list.  We'll signal that the tests are complete
        // by setting the status to TEST_GOOD.
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


/**
 * These are the UI states for initializing a new bridge.
 *
 * The first stage is where the user types in the ip of the bridge.
 * todo: get bridge ip via udp broadcast to eliminate this step
 * If there's an error, the state reflects this.
 *
 * Second stage is for the user to tap on the bridge's button and then
 * signal for this app to ping the bridge.  The bridge should then send
 * us a token (name) that we'll use to get info from the bridge.  This is
 * part of the security of the system.  If there's an error, it's probably
 * because the user didn't tap the bridge or something timed out.
 *
 * The third stage is simply a happy message that we successfully added the
 * bridge (we got the token and can communicate with it).  Once acknowledged,
 * the state will go back to [NOT_INITIALIZING].
 */
enum class BridgeInitStates {
    NOT_INITIALIZING,
    STAGE_1_GET_IP,
    STAGE_1_ERROR__BAD_IP_FORMAT,
    STAGE_1_ERROR__NO_BRIDGE_AT_IP,

    STAGE_2_PRESS_BRIDGE_BUTTON,
    STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,

    STAGE_3_ALL_GOOD_AND_DONE
}

private const val TAG = "SplashViewModel"