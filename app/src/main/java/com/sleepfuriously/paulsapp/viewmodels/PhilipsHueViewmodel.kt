package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.model.isConnectivityWifiWorking
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.GetBridgeTokenErrorEnum
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueModel
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueNewBridge
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Viewmodel for all the Philips Hue devices.
 */
class PhilipsHueViewmodel : ViewModel() {

    //-------------------------
    //  class data
    //-------------------------

    private val _wifiWorking = MutableStateFlow<Boolean?>(null)
    /** Will be true or false depending on wifi state.  Null means it hasn't been tested yet */
    var wifiWorking = _wifiWorking.asStateFlow()


    private val _philipsHueTestStatus = MutableStateFlow(TestStatus.NOT_TESTED)
    /** when true, all philips hue tests are complete */
    var philipsHueTestStatus = _philipsHueTestStatus.asStateFlow()


    // todo: move to MainViewmodel
    private val _iotTestingState = MutableStateFlow(TestStatus.NOT_TESTED)
    /** Will be true only while tests are running */
    var iotTestingState = _iotTestingState.asStateFlow()

    // todo: move to MainViewmodel
    private val _iotTestingErrorMsg = MutableStateFlow("")
    /** This will hold any message about the current iot testing errors */
    var iotTestingErrorMsg = _iotTestingErrorMsg.asStateFlow()


    private val _addNewBridgeState = MutableStateFlow(BridgeInitStates.NOT_INITIALIZING)
    /** Tells what steps are currently taking place when we are adding a new bridge */
    var addNewBridgeState = _addNewBridgeState.asStateFlow()


    /**
     * This list of bridges is just an intermediary--it's set to change when the Philips
     * Hue Model changes.  It then passes along info the the View.  I should not be
     * accessing this directly except from within the collector (of the Model).
     */
    var philipsHueBridgesCompose by mutableStateOf<Set<PhilipsHueBridgeInfo>>(mutableSetOf())
        private set


    private val _crashNow = MutableStateFlow(false)
    /** when true, the Activity should call finish() */
    var crashNow = _crashNow.asStateFlow()

    private val _waitingForResponse = MutableStateFlow(false)
    /** when true, we are in the process of waiting for an important response from a bridge (probably) */
    var waitingForResponse = _waitingForResponse.asStateFlow()

    /** access to the philips hue bridge and all that stuff that goes with it */
    private var bridgeModel = PhilipsHueModel(coroutineScope = viewModelScope)

    /** This variable holds a new bridge while working on it.  Once filled in, it'll be added to the list */
    var workingNewBridge: PhilipsHueNewBridge? = null
        private set


    init {
        // Setup a coroutine that listens for changes (works as a consumer) to
        // the flow from the Philips Hue model.
        // When there is a change, pass that change along to philipsHueBridgesCompose
        // (this makes us now a producer).  Thus this viewmodel is an intermediary (both).
        viewModelScope.launch {
            bridgeModel.bridgeFlowSet.collectLatest {
//            bridgeModel.bridgeFlowSet.collect {
                // hmmm, this doesn't seem to work???
//                philipsHueBridgesCompose = it

                Log.d(TAG, "collecting bridgeFlowSet from bridgeModel. change = $it, hash = ${System.identityHashCode(it)}")

                // rebuilding a copy of the
                val newBridgeSet = mutableSetOf<PhilipsHueBridgeInfo>()
                it.forEach { bridge ->
                    newBridgeSet.add(bridge)
                    Log.d(TAG, "Setting philipsHueBridgesCompose:")
                    bridge.rooms.forEach { room ->
                        Log.d(TAG, "   room ${room.name}, on = ${room.on}, bri = ${room.brightness}")
                    }
                }
                philipsHueBridgesCompose = newBridgeSet
            }
        }

        // example from my working test
//        viewModelScope.launch {
//            bridgeRepository.bridges.collectLatest {
//                Log.d(TAG, "bridges = $it")
//                composeBridgeSet = it
//            }
//        }

    }

    //-------------------------
    //  public functions
    //-------------------------

    /**
     * User has changed the brightness of a room, either by turning a light on/off
     * or by changing the dimming level.  Tell the model about it.
     *
     * @param   newBrightness       The new brightness level: Int [0..MAX_BRIGHTNESS].
     *
     * @param   newOnStatus         Has the room been turned on or off?  Depends
     *                              on the previous state.
     *
     * @param   changedRoom         The room in question.
     *
     * @param   changedBridge       The bridge that holds the room.
     */
    fun roomBrightnessChanged(
        newBrightness: Int,
        newOnStatus: Boolean,
        changedRoom: PhilipsHueRoomInfo,
        changedBridge: PhilipsHueBridgeInfo
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            bridgeModel.roomBrightnessChanged(
                newBrightness = newBrightness,
                newOnStatus = newOnStatus,
                changedRoom = changedRoom,
                changedBridge = changedBridge
            )
        }
    }

    /**
     * This is for testing the UI for adding a bridge.  Here we simply add a dummy
     * bridge with some random data.
     */
    fun addBridgeTest() {
        workingNewBridge = PhilipsHueNewBridge(
            ip = "hey, I'm supposed to be an ip!!",
            labelName = "testBridge",
            token = "goblllelsldlsy gook"
        )
        bridgeAddAllGoodAndDone()
    }

    /**
     * todo: move to MainViewmodel
     *
     * Runs all the initalization tests of the IoT devices.
     *
     * preconditions
     *  [bridgeModel] is already setup and running
     *
     * side effects
     *   [_iotTestingState]      set to true when this is done
     */
    fun checkIoT(ctx: Context) {

        Log.d(TAG, "checkIoT() start. _iotTest = ${_iotTestingState.value}, philipsHueTestStatus = ${_philipsHueTestStatus.value}")

        _iotTestingState.value = TestStatus.TESTING

        Log.d(TAG, "checkIoT() part 1. _iotTest = ${_iotTestingState.value}, philipsHueTestStatus = ${_philipsHueTestStatus.value}")

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
                _iotTestingState.value = TestStatus.TEST_BAD
                _iotTestingErrorMsg.value = ctx.getString(R.string.wifi_not_working)
                Log.d(TAG, "checkIoT() cannot find wifi connectivity: aborting!")
                return@launch
            }

            //------------
            // 2.  check philips hue system
            //
            checkPhilipsHue()
            allTestsSuccessful = philipsHueTestStatus.value == TestStatus.TEST_GOOD

        }.invokeOnCompletion {
            // Note: this code will be called when the above coroutine exits,
            // even if by a return statement.

            // signal tests complete
            Log.d(TAG, "checkIoT() completion: allTestsSuccessful = $allTestsSuccessful")
            _iotTestingState.value = TestStatus.TEST_GOOD
            _iotTestingErrorMsg.value = ""
        }
    }

    /**
     * Removes the specified bridge from our list.  Also tries to remove
     * the token (username) from the bridge's list of approved devices.
     * All this happens in the background.
     *
     * If the bridgeId is invalid, then nothing is done (of course).
     */
    fun deleteBridge(bridgeId: String) {
        if (bridgeModel.deleteBridge(bridgeId) == false) {
            Log.e(TAG, "Unable to remove bridge $bridgeId from our Set of bridges!")
        }
    }

    /**
     * Tells the philips hue model to stop receiving server-sent events to
     * this bridge.
     */
    fun disconnectBridge(bridge: PhilipsHueBridgeInfo) {
        bridgeModel.disconnectFromBridge(bridge)
    }

    /**
     * Tells the model to try to re-connect to this bridge so we can receive
     * server-sent events.
     */
    fun connectBridge(bridge: PhilipsHueBridgeInfo) {
        bridgeModel.connectToBridge(bridge)
    }


    //-------------------------
    //  add bridge functions
    //-------------------------

    /**
     * Called when the back button is hit during bridge registration.
     * It's the responsibility of this function to figure out the current
     * state and change to the appropriate back state.
     */
    fun bridgeInitGoBack() {
        Log.d(TAG, "bridgeInitGoBack() started.  Current init state = ${_addNewBridgeState.value}")

        when (_addNewBridgeState.value) {
            BridgeInitStates.NOT_INITIALIZING -> {
                Log.e(TAG, "   Error: should never go back while in ${_addNewBridgeState.value}. Returning to NOT_INIALIZING state.")
                _addNewBridgeState.value = BridgeInitStates.NOT_INITIALIZING
            }

            BridgeInitStates.STAGE_1_GET_IP,
            BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT,
            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> {
                _addNewBridgeState.value = BridgeInitStates.NOT_INITIALIZING
            }

            BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON,
            BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE,
            BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE -> {
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP
            }

            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,
            BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED -> {
                _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
            }

            BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE -> {
                _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
            }

            BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
                _addNewBridgeState.value = BridgeInitStates.NOT_INITIALIZING
            }
        }
        Log.d(TAG, "   moving to ${_addNewBridgeState.value}")
    }


    /**
     * Begins the logical part of initializing the philips hue bridge.
     *
     * side effects:
     *      - initializingBridgeState is set to STAGE_1
     *      - newBridge is initialized with a unique id
     */
    fun beginAddPhilipsHueBridge() {
        _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP
        workingNewBridge = PhilipsHueNewBridge()
        Log.d(TAG, "newBridge is created. Ready to start adding data to it.")
    }

    /**
     * This gets the ip that's currently stored as the ip of a bridge
     * that is in the process of being connected to.  If there is no
     * bridge or no ip has been set, then an empty string is returned.
     */
    fun getNewBridgeIp() : String {
        Log.d(TAG, "getNewBridgeIp() -> ${workingNewBridge?.ip}")
        return workingNewBridge?.ip ?: ""
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
     *      _workingNewBridge       may have a valid ip (if user typed it in right)
     */
    fun addPhilipsHueBridgeIp(newIp: String) {

        Log.d(TAG, "begin addPhilipsHueBridgeIp( newIp = $newIp )")

        // this includes a test and may take a while
        viewModelScope.launch(Dispatchers.IO) {

            Log.d(TAG, "setting _waitingForResponse to TRUE!!!")
            _waitingForResponse.value = true

            // check format
            if (isValidBasicIp(newIp) == false) {
                Log.e(TAG, "invalid ip format in addPhilipsHueBridgeIp($newIp)")
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT
                _waitingForResponse.value = false
                return@launch
            }

            // At this point, we might as well save the ip to this new bridge.
            // The user may need to modify it later.
            Log.d(TAG, "newbridge.ip set to $newIp")
            workingNewBridge!!.ip = newIp

            // is there actually a bridge there?
            if (bridgeModel.doesBridgeRespondToIp(newIp) == false) {
                Log.e(TAG, "bridge does not respond to ip in addPhilipsHueBridgeIp($newIp)")
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP
                _waitingForResponse.value = false
                return@launch
            }

            // The ip looks good.  Save it and signal to move on.
            // (yes, I want to crash if newBridge is null)
            _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON

            Log.d(TAG, "setting _waitingForResponse back to false")
            _waitingForResponse.value = false
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
            BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT -> {
                Log.d(TAG, "bridgeAddErrorMsgIsDisplayed() - reset to stage 1")
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP

                // also reset the bridge ip as it was messed up to begin with
                Log.d(TAG, "newbridge.ip attempted to set to BLANK")
                workingNewBridge?.ip = ""
            }

            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> {
                Log.d(TAG, "bridgeAddErrorMsgIsDisplayed() - reset to stage 1")
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_GET_IP
            }

            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,
            BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE,
            BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED,
            BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE,
            BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE -> {
                Log.d(TAG, "bridgeAddErrorMsgIsDisplayed() - reset to stage 2")
                _addNewBridgeState.value = BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
            }

            BridgeInitStates.NOT_INITIALIZING,
            BridgeInitStates.STAGE_1_GET_IP,
            BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON,
            BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
                Log.e(TAG, "error in bridgeInitErrorMsgDisplayed()!  State = ${_addNewBridgeState.value} is not an error state!")
            }
        }
    }

    /**
     * During the bridge initialization process, this function should be called
     * immediately after the user presses the button on their bridge.  This enables
     * the bridge to respond to a name (token) request.
     *
     * side effects
     *      addNewBridgeState - If successful, this is changed to STAGE_3_ALL_GOOD_AND_DONE.
     *                          Otherwise it's changed to STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE.
     */
    fun bridgeButtonPushed() {

        if (workingNewBridge == null) {
            Log.e(TAG, "bridgeButtonPushed() while newBridge is null.  Aborting!")
            return
        }
        val bridge = workingNewBridge ?: return        // yah, redundant.  But that's kotlin for ya!

        viewModelScope.launch(Dispatchers.IO) {
            val registerResponse = bridgeModel.registerAppToBridge(bridge.ip)
            val token = registerResponse.first
            val err = registerResponse.second

            if (token.isBlank()) {
                Log.d(TAG, "bridgeButtonPushed(), error returned from registerAppToBridge()")

                when (err) {
                    GetBridgeTokenErrorEnum.NO_ERROR,
                    GetBridgeTokenErrorEnum.BAD_IP -> {
                        Log.e(TAG, "Error state ${registerResponse.second} should not occur at this point in bridge registration.")
                        // we should crash now!!!
                        _crashNow.value = true
                    }
                    GetBridgeTokenErrorEnum.UNSUCCESSFUL_RESPONSE -> {
                        Log.d(TAG, "bridgeButtonPushed(), unsuccessful response")
                        _addNewBridgeState.value =
                            BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE
                    }
                    GetBridgeTokenErrorEnum.TOKEN_NOT_FOUND -> {
                        Log.d(TAG, "bridgeButtonPushed() - could not find token")
                        _addNewBridgeState.value =
                            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE
                    }

                    GetBridgeTokenErrorEnum.CANNOT_PARSE_RESPONSE_BODY -> {
                        Log.d(TAG, "bridgeButtonPushed() - unable to parse the response body")
                        _addNewBridgeState.value =
                            BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE
                    }
                    GetBridgeTokenErrorEnum.BUTTON_NOT_HIT -> {
                        Log.d(TAG, "bridgeButtonPushed() - button not hit")
                        _addNewBridgeState.value = BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED
                    }
                }
                _addNewBridgeState.value = BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE
            }
            else {
                // yay, it worked!  Add the token to the new bridge and set the success state.
                // The adding of the data will wait until bridgeAddAllGoodAndDone() is called.
                Log.d(TAG, "Successfully found the token in bridgeButtonPushed()")
                workingNewBridge!!.token = token
                _addNewBridgeState.value = BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE
            }
        }
    }

    /**
     * After receiving the notification that the bridge was successfully registered,
     * the new bridge needs to be added to the bridge list (and saved).  The
     * UI should call this to reset everything to [BridgeInitStates.NOT_INITIALIZING].
     */
    fun bridgeAddAllGoodAndDone() {

        Log.d(TAG, "bridgeAllGoodAndDone() begin")

        viewModelScope.launch(Dispatchers.IO) {

            // Get the name of the bridge (I'm using the printed name on the bridge itself)
            val v2bridge = bridgeModel.getBridgesAllFromApi(
                bridgeIp = workingNewBridge!!.ip,
                token = workingNewBridge!!.token
            )
            if (v2bridge.hasData() == false) {
                Log.e(TAG, "Unable to get info from bridge (id = ${workingNewBridge?.labelName})in bridgeAllGoodAndDone()! Aborting!")
                Log.e(TAG, "   data error msg = ${v2bridge.getError()}")
                _addNewBridgeState.value = BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE
                return@launch
            }

            workingNewBridge?.labelName = v2bridge.getName()

            // Add this new bridge to our permanent data (and the Model).
            bridgeModel.addBridge(workingNewBridge!!)

            // lastly signal that we're done with the new bridge stuff
            workingNewBridge = null
            _addNewBridgeState.value = BridgeInitStates.NOT_INITIALIZING
        }
    }


    /**
     * This will try to cancel any network testing that's going on (user hit a cancel
     * button).
     */
    fun bridgeCancelTest() {
        // todo
    }

    //-------------------------
    //  private functions
    //-------------------------

    /**
     * Checks the status of the Philips Hue bridges.  For each bridge that
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
    private suspend fun checkPhilipsHue() {

        // Get the bridge model started.  This will go through
        // its test routines during initialization.
        _philipsHueTestStatus.value = TestStatus.TESTING
        _iotTestingErrorMsg.value = ""

        // Get all the bridges.
        if (philipsHueBridgesCompose.isEmpty()) {
            // nothing to do if there are no bridges. signal done w/ no problems.
            _philipsHueTestStatus.value = TestStatus.TEST_GOOD
            return
        }

        // check each bridge one by one to see if its info is current
        // and active.
        for (bridge in philipsHueBridgesCompose) {
            // does this bridge have an ip?
            val ip = bridge.ip
            if (ip.isBlank()) {
                bridge.active = false
                continue
            }

            // check bridge is responding
            if (bridgeModel.doesBridgeRespondToIp(ip) == false) {
                bridge.active = false
                continue
            }

            // At this point, the bridge is definitely active.  We may or
            // may not have a token that works though!
            bridge.active = true

            // Do we have a token?
            val token = bridge.token
            if (token.isBlank()) {
                // no token, done with this bridge
                continue
            }

            // check that token works
            val tokenWorks = bridgeModel.doesBridgeAcceptToken(bridge.ip, token)
            if (tokenWorks == false) {
                continue
            }
        }

        // All tests complete.  The results are stored within each
        // bridge in the list.  We'll signal that the tests are complete
        // by setting the status to TEST_GOOD.
        _philipsHueTestStatus.value = TestStatus.TEST_GOOD
        _iotTestingErrorMsg.value = ""
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

    /** User needs to hit the bridge button and then hit next so we can initiate registration */
    STAGE_2_PRESS_BRIDGE_BUTTON,

    /** we tried to get a token, but didn't succeed--possibly didn't push the button? */
    STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,

    /** the response returned cannot be parsed--probably because this wasn't a Philips Hue bridge? */
    STAGE_2_ERROR__CANNOT_PARSE_RESPONSE,

    /** we tried to register the app with the bridge without pushing the button on it first */
    STAGE_2_ERROR__BUTTON_NOT_PUSHED,

    /** The bridge didn't respond or responded in a completely unintelligiable way (not RESTful) */
    STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE,

    STAGE_3_ALL_GOOD_AND_DONE,

    /** An error occurred when adding the new bridge to our list of bridges */
    STAGE_3_ERROR_CANNOT_ADD_BRIDGE
}

private const val TAG = "PhilipsHueViewmodel"