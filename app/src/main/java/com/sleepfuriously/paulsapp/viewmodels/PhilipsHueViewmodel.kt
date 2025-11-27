package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.model.isConnectivityWifiWorking
import com.sleepfuriously.paulsapp.model.isValidBasicIp
import com.sleepfuriously.paulsapp.model.philipshue.GetBridgeTokenErrorEnum
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueModelScenes
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueNewBridge
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRepository
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.WorkingFlock
import com.sleepfuriously.paulsapp.model.philipshue.doesBridgeAcceptToken
import com.sleepfuriously.paulsapp.model.philipshue.doesBridgeRespondToIp
import com.sleepfuriously.paulsapp.model.philipshue.generateV2Id
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Viewmodel for all the Philips Hue devices.
 *
 * Organization of this file:
 *
 *      Flows
 *      -----
 *
 *      Data
 *      ----
 *
 *      Init
 *      ----
 *
 *      Bridge functions
 *      ------
 *          These are ways of returning and sending data to a bridge
 *
 *          Update
 *          ------
 *
 *          Add
 *          ---
 *
 *      Room & Zone functions
 *      ----------
 *
 *      Flock functions
 *      -----
 *          All about Flocks!
 *          READ
 *          ----
 *
 *          SEND
 *          ----
 *
 *      Scenes functions
 *      ------
 *          Various scene manipulations and data gathering
 *          READ
 *          ----
 *
 *          SEND
 *          ----
 *
 *      Miscellaneous functions
 *      -------------
 *          For everything else, especially helper and private functions
 */
class PhilipsHueViewmodel : ViewModel() {

    //-------------------------
    //  flow data
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
     * The bridges that are to be displayed in the views
     */
    var philipsHueBridgesCompose by mutableStateOf<List<PhilipsHueBridgeInfo>>(mutableListOf())
        private set

    private val _crashNow = MutableStateFlow(false)
    /** when true, the Activity should call finish() */
    var crashNow = _crashNow.asStateFlow()

    private val _waitingForResponse = MutableStateFlow(false)
    /** when true, we are in the process of waiting for an important response from a bridge (probably) */
    var waitingForResponse = _waitingForResponse.asStateFlow()

    /** This variable holds a new bridge while working on it.  Once filled in, it'll be added to the list */
    var workingNewBridge: PhilipsHueNewBridge? = null
        private set

    /** todo: combine this with  */
    private val _sceneDisplayStuffForRoom = MutableStateFlow<SceneDataForRoom?>(null)
    /**
     * When not null, a room's info should be displayed.  That consists of:
     * 1. The bridge controlling the room
     * 2. The room itself
     * 3. A list of all the scenes for this room
     *  - todo: The lights for this room
     */
    var sceneDisplayStuffForRoom = _sceneDisplayStuffForRoom.asStateFlow()

    private val _sceneDisplayStuffForZone = MutableStateFlow<SceneDataForZone?>(null)
    /** similar to [sceneDisplayStuffForRoom] */
    val sceneDisplayStuffForZone = _sceneDisplayStuffForZone.asStateFlow()

    private val _sceneDisplayStuffForFlock = MutableStateFlow<SceneDataForFlock?>(null)
    /** If not null -> a Flock's scene info should be displayed. */
    val sceneDisplayStuffForFlock = _sceneDisplayStuffForFlock.asStateFlow()

    private val _flocks = MutableStateFlow<List<PhilipsHueFlockInfo>>(emptyList())
    /** Holder of the Flocks observed from the repository  */
    val flocks = _flocks.asStateFlow()

    private val _showAddOrEditFlockDialog = MutableStateFlow(false)
    /**
     * When True, the UI should show the add Flock dialog.  It'll be an EDIT if
     * [originalFlock] is not null.
     */
    val showAddOrEditFlockDialog = _showAddOrEditFlockDialog.asStateFlow()

    private val _addFlockErrorMsg = MutableStateFlow<String>(EMPTY_STRING)
    /** When not empty, display as an error message */
    val addFlockErrorMsg = _addFlockErrorMsg.asStateFlow()

    //-------------------------
    //  public data
    //-------------------------

    // Source - https://stackoverflow.com/a/77033204
    /** Holds the state of a lazyList (column). Allows UI to return to proper scroll position. */
    var lazyColumnState: LazyGridState by mutableStateOf(LazyGridState(0,0))


    //-------------------------
    //  private data
    //-------------------------

    /** access to repository */
    private var phRepository = PhilipsHueRepository(viewModelScope)

    /**
     * This holds data while a flock is being constructed. Should be reset
     * before starting a new one.
     */
    private var workingFlock = WorkingFlock(id = generateV2Id())

    /** Holder for the original flock while the user edits it. Null means not applicable. */
    var originalFlock: PhilipsHueFlockInfo? = null
        private set

    //-------------------------
    //  init
    //-------------------------

    init {
        viewModelScope.launch {
            // convert list of BridgeModels to a list Bridges
            phRepository.bridgeInfoList.collectLatest { bridgeModelList ->
                Log.d(TAG, "phRepository change noticed...")
                val tmpBridgeList = mutableListOf<PhilipsHueBridgeInfo>()
                bridgeModelList.forEach { bridgeInfo ->
                    tmpBridgeList.add(bridgeInfo)
                }
                Log.d(TAG, "    - collecting bridge list from phRepository: bridgeList size = ${tmpBridgeList.size}")
                Log.d(TAG, "      bridgeList = $tmpBridgeList")
                philipsHueBridgesCompose = tmpBridgeList
                Log.d(TAG, "   - and now philipsHueBridgesCompose size = ${philipsHueBridgesCompose.size}")
            }
        }

        viewModelScope.launch {
            // convert the BridgeModels list to a list of Flocks too.
            // I'm pretty sure we can't combine this with the bridge list above.
            phRepository.flockList.collectLatest { flockList ->
                Log.d(TAG, "repository flock info changed")
                _flocks.update { flockList }
            }
        }
    }

    /**
     * todo: move to MainViewmodel
     *
     * Runs all the initalization tests of the IoT devices.
     *
     * preconditions
     *  [philipsHueBridgesCompose] is active and working
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


    //-------------------------
    //  Bridge functions
    //-------------------------

    //---------
    //  Update
    //

    /**
     * User has changed the brightness of a room, either by turning a light on/off
     * or by changing the dimming level.  Tell the repository about it.
     *
     * @param   newBrightness       The new brightness level: Int [0..MAX_BRIGHTNESS].
     *
     * @param   changedRoom         The room in question.
     */
    fun sendRoomBrightness(
        newBrightness: Int,
        changedRoom: PhilipsHueRoomInfo,
    ) {
        phRepository.sendRoomBrightnessToBridge(
            room = changedRoom,
            newBrightness = newBrightness
        )
    }

    /**
     * Tells the repository to change the on/off state of the given room.
     *
     * @param   newOnOffState       Turned on = true.  Off = false.
     *
     * @param   changedRoom         The room in question.
     */
    fun sendRoomOnOff(
        newOnOffState: Boolean,
        changedRoom: PhilipsHueRoomInfo
    ) {
        phRepository.sendRoomOnOffToBridge(
            room = changedRoom,
            newOnStatus = newOnOffState
        )
    }

    /**
     * Inform the repository that the user wants to change the brightness
     * of a zone.
     */
    fun sendZoneBrightness(
        newBrightness: Int,
        changedZone: PhilipsHueZoneInfo,
    ) {
        phRepository.sendZoneBrightnessToBridge(
            zone = changedZone,
            newBrightness = newBrightness
        )
    }

    /**
     * Tell the repository that the user has changed the on/off status
     * of a zone.
     */
    fun sendZoneOnOff(
        newOnOffState: Boolean,
        changedZone: PhilipsHueZoneInfo
    ) {
        phRepository.sendZoneOnOffToBridge(
            zone = changedZone,
            newOnOff = newOnOffState
        )
    }

    /**
     * Tell the flock that the user wants a brightness change.
     */
    fun sendFlockBrightness(
        newBrightness: Int,
        changedFlock: PhilipsHueFlockInfo
    ) {
        viewModelScope.launch {
            phRepository.sendFlockBrightnessToBridges(
                changedFlock = changedFlock,
                newBrightness = newBrightness
            )
        }
    }

    /**
     * Tell the given flock that the user want it to turn on or off.  This will eventually
     * cause the flock to send on/off signals to relevant bridges.
     */
    fun sendFlockOnOffToBridges(
        newOnOffState: Boolean,
        changedFlock: PhilipsHueFlockInfo
    ) {
        viewModelScope.launch {
            phRepository.sendFlockOnOffToBridges(
                changedFlock = changedFlock,
                newOnOff = newOnOffState
            )
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
        phRepository.deletePhilipsHueBridge(bridgeId)       // fixme: crash!!!
    }

    /**
     * Tells the model to try to re-connect to this bridge so we can receive
     * server-sent events.
     */
    fun connectBridge(bridge: PhilipsHueBridgeInfo) {
        phRepository.startSseConnection(bridge)
    }

    /**
     * Tells the philips hue model to stop receiving server-sent events to
     * this bridge.
     */
    fun disconnectBridge(bridge: PhilipsHueBridgeInfo) {
        phRepository.stopSseConnection(bridge)
    }

    //---------
    //  Add
    //

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
            BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED,
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
        workingNewBridge = PhilipsHueNewBridge(humanName = "tmp name")
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
                Log.w(TAG, "addPhilipsHueBridgeIp() - invalid ip format: $newIp")
                _addNewBridgeState.value = BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT
                _waitingForResponse.value = false
                return@launch
            }

            // At this point, we might as well save the ip to this new bridge.
            // The user may need to modify it later.
            Log.d(TAG, "newbridge.ip set to $newIp")
            workingNewBridge!!.ip = newIp

            // check to see if we already know about this bridge
            if (phRepository.doesBridgeExist(newIp)) {
                Log.w(TAG, "addPhilipsHueBridgeIp() - bridge already exists: ip = $newIp")
                _addNewBridgeState.update { BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED }
                _waitingForResponse.update { false }
                return@launch
            }

            // is there actually a bridge there?
            if (doesBridgeRespondToIp(newIp) == false) {
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

            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP,
            BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED -> {
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
    fun addBridgeButtonPushed() {

        // sanity checks: make sure that the bridge exists and is operating
        if (workingNewBridge == null) {
            Log.e(TAG, "bridgeButtonPushed() while newBridge is null.  Aborting!")
            _addNewBridgeState.update { BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE }
            return
        }
        if (workingNewBridge == null) {
            _addNewBridgeState.update { BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE }
            return
        }
        // Should never return here, but that's kotlin!  It's the only way to make sure bridge
        // is not null.
        val bridge = workingNewBridge ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val registerResponse = phRepository.registerAppToPhilipsHueBridge(bridge.ip)
            val token = registerResponse.first
            val err = registerResponse.second

            if (token.isBlank()) {
                Log.e(TAG, "bridgeButtonPushed(), error returned from registerAppToBridge()")

                when (err) {
                    GetBridgeTokenErrorEnum.NO_ERROR,       // This should not happen, but if it does, crash to get our attention!
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
                _addNewBridgeState.update { BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE }
            }
        }
    }

    /**
     * After receiving the notification that the bridge was successfully registered,
     * the new bridge needs to be added to the bridge list (and saved).  The
     * UI should call this to reset everything to [BridgeInitStates.NOT_INITIALIZING].
     */
    fun bridgeAddAllGoodAndDone() {

        Log.d(TAG, "bridgeAllGoodAndDone() begin - addNewBridgeState = ${addNewBridgeState.value}")

        viewModelScope.launch(Dispatchers.IO) {
            if (phRepository.addNewBridge(workingNewBridge!!)) {
                // signal that we're done with the new bridge stuff
                workingNewBridge = null
                _addNewBridgeState.update { BridgeInitStates.NOT_INITIALIZING }
                Log.d(TAG,"bridgeAddAllGoodAndDone() - added succefully. addNewBridgeState = $addNewBridgeState")
            }
            else {
                // error adding this bridge
                _addNewBridgeState.update { BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE }
                Log.e(TAG, "bridgeAllGoodAndDone() - addBridge() returned false! addNewBridgeState = $addNewBridgeState")
            }
        }
    }

    //---------
    //  Rooms & Zones
    //

    /**
     * Finds all the rooms for all the bridges.
     */
    fun getAllRooms() : Set<PhilipsHueRoomInfo> {
        return phRepository.getAllRooms()
    }

    /**
     * Finds all the zones used by all the bridges
     */
    fun getAllZones() : Set<PhilipsHueZoneInfo> {
        return phRepository.getAllZones()
    }

    //---------
    //  Scenes
    //

        //
        // Read Scenes
        //

    /**
     * Finds out which scenes are used by a room.
     *
     * @return  A List of scenes that a room can use.  Empty list if none found.
     */
    fun getSceneListForRoom(
        bridge: PhilipsHueBridgeInfo,
        room: PhilipsHueRoomInfo
    ) : List<PHv2Scene> {
        return PhilipsHueModelScenes.getAllScenesForRoom(room, bridge)
    }

    /**
     * Finds out which scenes are used by a zone.
     *
     * @return  A List of scenes that a zone can use.  Empty list if none found.
     */
    fun getSceneListForZone(
        bridge: PhilipsHueBridgeInfo,
        zone: PhilipsHueZoneInfo
    ) : List<PHv2Scene> {
        return PhilipsHueModelScenes.getAllScenesForZone(zone = zone, bridge = bridge)
    }

        //
        // Send Scenes
        //

    /**
     * UI calls this to indicate that user wants to show the scenes for a
     * given room.
     *
     * side effects
     *  - [sceneDisplayStuffForRoom] - Loaded with all the scenes that this room currently
     *                        can access
     */
    fun showScenesForRoom(bridge: PhilipsHueBridgeInfo, room: PhilipsHueRoomInfo) {
        _sceneDisplayStuffForRoom.update {
            Log.d(TAG, "showScenesForRoom(), room = ${room.name}")
            val sceneList = getSceneListForRoom(room =room, bridge = bridge)
            SceneDataForRoom(bridge, room, sceneList)
        }
    }

    /**
     * Similar to [showScenesForRoom].
     */
    fun showScenesForZone(bridge: PhilipsHueBridgeInfo, zone: PhilipsHueZoneInfo) {
        _sceneDisplayStuffForZone.update {
            Log.d(TAG, "showScenesForZone, zone = ${zone.name}")
            val sceneList = getSceneListForZone(zone = zone, bridge = bridge)
            SceneDataForZone(bridge, zone, sceneList)
        }
    }

    /**
     * UI wants to show the scenes for a flock.  This is trickier than Rooms
     * or Zones as Flocks are supersets of such.
     *
     * side effects
     *  - [sceneDisplayStuffForFlock] will be loaded with all the scenes
     *      available to this flock (which may be far less than everything).
     *
     * todo - do this right, so that scenes can be applied universally to all
     *  grouped lights in this flock
     */
    fun showScenesForFlock(flock: PhilipsHueFlockInfo) {
        // figure out which flockmodel we're using for this flock
        val flockModel = phRepository.findFlockModelFromFlock(flock)
        if (flockModel == null) {
            Log.e(TAG, "showScenesForFlock() unable to find flock model! Aborting!")
            return
        }

        _sceneDisplayStuffForFlock.update {
            Log.d(TAG, "showScenesForFlock() flock = ${flock.name}")

            val roomScenes = flockModel.getRoomSceneListForFlock(flock)
            val zoneScenes = flockModel.getZoneSceneListForFlock(flock)

            if (roomScenes.isEmpty() && zoneScenes.isEmpty()) {
                Log.w(TAG, "showScenesForFlock() - no scenes for this flock")
            }
            SceneDataForFlock(flock, roomScenes, zoneScenes)
        }
    }

    /**
     * Call when the user no longer needs to see scene info for a room.
     *
     * side effects
     *  - [sceneDisplayStuffForRoom] - set to null
     */
    fun stopShowingScenes() {
        Log.d(TAG, "dontShowScenes()")
        _sceneDisplayStuffForRoom.update { null }
        _sceneDisplayStuffForZone.update { null }
        _sceneDisplayStuffForFlock.update { null }
    }

    /**
     * UI calls this when the user selects a scene to run for a given room or zone.
     * This in turn calls the repository to have that place changed.  The result
     * should make the lights change and then bubble up (through a sse) and we'll
     * see changes in our UI.
     *
     * side effects
     *  room - It will be changed (down the path) so that it'll know its scene changed.
     *
     * Note
     *  This does NOT cause the scenes info to go away.
     */
    fun setSceneSelectedForRoom(
        bridge: PhilipsHueBridgeInfo,
        room: PhilipsHueRoomInfo,
        scene: PHv2Scene
    ) {
        // Tell the repo that something changed in the room.
        phRepository.sendRoomSceneToBridge(bridge, room, scene)
    }

    /**
     * Similar to [setSceneSelectedForRoom].
     */
    fun setSceneSelectedForZone(
        bridge: PhilipsHueBridgeInfo,
        zone: PhilipsHueZoneInfo,
        scene: PHv2Scene
    ) {
        phRepository.sendZoneSceneToBridge(bridge, zone, scene)
    }

    /**
     * Similar to [setSceneSelectedForRoom].
     */
    fun setSceneSelectedForFlock(
        flock: PhilipsHueFlockInfo,
        scene: PHv2Scene
    ) {
        phRepository.sendFlockSceneToBridge(flock, scene)
    }


    //---------
    //  Flocks
    //

    /**
     * Call to cause the UI to start displaying the construct flock dialog.
     *
     * side effects
     *  - [workingFlock] reset to empty.
     */
    fun beginAddFlockDialog() {
        workingFlock = WorkingFlock(id = generateV2Id())
        _showAddOrEditFlockDialog.update { true }
    }

    /**
     * UI calls this function to initiate editing a flock.
     *
     * @param   flock       The flock to make changes to
     */
    fun beginEditFlockDialog(flock: PhilipsHueFlockInfo) {
        originalFlock = flock.copy()
        workingFlock = WorkingFlock(
            id = flock.id,
            name = flock.name,
            roomIdSet = flock.roomSet.map { it.v2Id }.toSet(),
            zoneIdSet = flock.zoneSet.map { it.v2Id }.toSet()
        )
        _showAddOrEditFlockDialog.update { true }
    }

    /**
     * Change the UI state so that it'll no longer show the flock
     * construction stuff.
     *
     * side effects
     *  - [originalFlock]       Set to null
     *  - [showAddOrEditFlockDialog]  Set to false
     *  - [addFlockErrorMsg]    Set to empty
     */
    fun cancelAddOrEditFlock() {
        originalFlock = null
        clearFlockErrorMsg()
        _showAddOrEditFlockDialog.update { false }
    }

    /**
     * UI should call this to tell the viewmodel that the error message has
     * been displayed and no longer needs to be shown.
     */
    fun clearFlockErrorMsg() {
        _addFlockErrorMsg.update { EMPTY_STRING }
    }

    /**
     * Signals that the flock construction or modification is complete.
     * Figure out if it's a modification or a brand-new Flock.  Oh yeah,
     * don't forget to turn off that dialog!
     *
     * preconditions
     *  - [workingFlock] is complete
     *  - [originalFlock] contains the original (if we're modifying)
     *
     * side effects
     *  - [addFlockErrorMsg]    Could be changed with a valid error message
     *
     * @param   newFlockName    The name that the user wants to use for this flock.
     */
    fun addOrEditFlockComplete(newFlockName: String, ctx: Context) {

        // test to see if the flock info is usable
        if (newFlockName.isBlank()) {
            _addFlockErrorMsg.update {
                ctx.getString(R.string.no_name_for_new_flock)
            }
            return
        }
        if (workingFlock.roomIdSet.isEmpty() && workingFlock.zoneIdSet.isEmpty()) {
            _addFlockErrorMsg.update {
                ctx.getString(R.string.no_rooms_or_zones_for_new_flock)
            }
            return
        }

        // Find the rooms (from ALL of 'em) that match the ids in the working flock
        val roomSet = getAllRooms().filter {        // fixme: returns 0
            workingFlock.roomIdSet.contains(it.v2Id)
        }.toSet()

        // same for zones
        val zoneSet = getAllZones().filter {
            workingFlock.zoneIdSet.contains(it.v2Id)
        }.toSet()


        // So are we editing an existing flock or are we adding
        // a new flock?
        if (originalFlock != null) {
            // We're editing a flock.
            editFlock(
                origFlockId = originalFlock!!.id,
                newName = newFlockName,
                newRoomSet = roomSet,
                newZoneSet = zoneSet
                )

            // finally signal that we're no longer editing
            cancelAddOrEditFlock()
            return
        }

        addFlock(
            name = newFlockName,
            roomSet = roomSet,
            zoneSet = zoneSet
        )
        cancelAddOrEditFlock()
    }

    /**
     * The UI should call this while constructing a new [PhilipsHueFlockInfo]
     * or editing an existing flock.  It should call  each time the user
     * adds or removes a room or a zone.
     *
     * @param   added       True means the item was added.
     *                      False means it was removed.
     *
     * @param   room        A room is the item in question.  Will be null if
     *                      the item is a zone.
     *
     * @param   zone        The zone to be added or removed. Null if we're
     *                      working with a room.
     */
    fun toggleFlockList(
        added: Boolean,
        room: PhilipsHueRoomInfo?,
        zone: PhilipsHueZoneInfo?
    ) {
        // quick sanity check.  Only one (room or zone) should be non-null
        if ((room != null) && (zone != null)) {
            Log.e(TAG, "toggleFlockList() error!  Both room and zone are non-null! Aborting!")
            return
        }

        if (room != null) {
            if (added) {
                workingFlock = workingFlock.copy(roomIdSet = workingFlock.roomIdSet + room.v2Id)
            }
            else {
                workingFlock = workingFlock.copy(roomIdSet = workingFlock.roomIdSet - room.v2Id)
            }
        }

        else if (zone != null) {
            if (added) {
                workingFlock = workingFlock.copy(zoneIdSet = workingFlock.zoneIdSet + zone.v2Id)
            }
            else {
                workingFlock = workingFlock.copy(zoneIdSet = workingFlock.zoneIdSet - zone.v2Id)
            }
        }

        else {
            // should never happen: at least one of the room or zone should be valid
            Log.e(TAG, "toggleFlockList() - error: both room and zone are null!")
        }
    }

    /**
     * Use this to see if the viewmodel is currently in a state of adding
     * or editing a flock.
     *
     * @return      True iff we are EDITING (not adding, not doing neither) a flock.
     */
    fun isCurrentlyEditingFlock() : Boolean {
        return originalFlock != null
    }

    /**
     * Call this to tell if the view model is currently Adding a flock.
     *
     * @return      True iff ADDING a brand-new flock.  Editing or neither
     *              yields False.
     */
    fun isCurrentlyAddingFlock() : Boolean {
        if (isCurrentlyEditingFlock()) {
            return false        // editing, not ADDing
        }
        return showAddOrEditFlockDialog.value     // yet we're still showing a dialog--thus adding!
    }

    /**
     * Adds a [PhilipsHueFlockInfo] and all the associated stuff.
     *
     * @param   name        Human name for this flock.  Use blank (default)
     *                      for testing.
     *
     * @param   roomSet     The rooms controlled by this flock. Default
     *                      is null for testing.
     *
     * @param   zoneSet     The zones that are used by this flock.
     *                      Defaults to null (testing).
     */
    private fun addFlock(
        name: String = "",
        roomSet: Set<PhilipsHueRoomInfo>,
        zoneSet: Set<PhilipsHueZoneInfo>
    ) {

        // check params for correct input!
        if (name.isBlank()) {
            Log.v(TAG, "addFlock() - user tried a name that's blank - ignoring")
            return
        }

        if (roomSet.isEmpty() && zoneSet.isEmpty()) {
            Log.v(TAG, "addFlock() - user entered a flock with no rooms or zones - ignoring")
            return
        }

        // create the flock for the repository
        Log.d(TAG, "Creating new flock with ${roomSet.size} rooms and ${zoneSet.size} zones.")
        phRepository.addFlock(
            name = name,
            roomSet = roomSet,
            zoneSet = zoneSet,
            longTermStorage = true
        )
    }

    /**
     * Use this to modify an existing flock.
     *
     * @param   origFlockId     The id of the original flock.
     *
     * @param   newName         The name for this flock.  If it has not
     *                          changed, then this will be empty.
     *
     * @param   newRoomSet      The set of Rooms that this flock is now using.
     *
     * @param   newZoneSet      The zones this flock is now using.
     */
    fun editFlock(
        origFlockId: String,
        newName: String,
        newRoomSet: Set<PhilipsHueRoomInfo>,
        newZoneSet: Set<PhilipsHueZoneInfo>
    ) {
        // use the new name or the original name is newName is empty
        val name = newName.ifEmpty { originalFlock!!.name }

        // check for error conditions
        if (newRoomSet.isEmpty() && newZoneSet.isEmpty()) {
            Log.v(TAG, "editFlock() - user entered a flock with no rooms or zones - aborting!")
            return
        }

        // create the flock for the repository
        phRepository.editFlock(
            origFlockId = origFlockId,
            name = name,
            roomSet = newRoomSet,
            zoneSet = newZoneSet
        )

    } // editFlock()

    fun deleteFlock(flock: PhilipsHueFlockInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            phRepository.deleteFlock(flock)
        }
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
            // no bridges--nothing to do.  signal done with no problems.
            _philipsHueTestStatus.value = TestStatus.TEST_GOOD
            return
        }

        // check each bridge one by one to see if its info is current
        // and active.
        for (bridge in philipsHueBridgesCompose) {
            // does this bridge have an ip?
            val ip = bridge.ipAddress
            if (ip.isBlank()) {
                bridge.active = false
                continue
            }

            // check bridge is responding
            if (doesBridgeRespondToIp(bridge.ipAddress) == false) {
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
            if (doesBridgeAcceptToken(bridge.ipAddress, token) == false) {
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

/**
 * Holds data that the UI needs to display all the scenes for a room.
 */
data class SceneDataForRoom(
    val bridge: PhilipsHueBridgeInfo,
    val room: PhilipsHueRoomInfo,
    val scenes: List<PHv2Scene>
//    val lights: List<PhilipsHueLightInfo>  todo
)

// todo: combine this with SceneDataForRoom
data class SceneDataForZone(
    val bridge: PhilipsHueBridgeInfo,
    val zone: PhilipsHueZoneInfo,
    val scenes: List<PHv2Scene>
//    val lights: List<PhilipsHueLightInfo>  todo
)

/**
 * Scene info for the UI flocks.
 *
 * The scenes are maps: each room or zone will be mapped to a list of scenes
 * it operates with.
 */
data class SceneDataForFlock(
    val flock: PhilipsHueFlockInfo,
    val roomScenes: Map<PhilipsHueRoomInfo, List<PHv2Scene>>,
    val zoneScenes: Map<PhilipsHueZoneInfo, List<PHv2Scene>>
) {
    /**
     * Finds all the scenes that match (through their name).
     * Slow: O(n^n) but there shouldn't be that many scenes
     */
    fun getMatchingSceneNames() : Set<String> {
        // first start by creating a list of ALL the scenes
        val allScenes = mutableListOf<PHv2Scene>()
        for(roomSceneList in roomScenes) {
            allScenes.addAll(roomSceneList.value)
        }
        for(zoneSceneList in zoneScenes) {
            allScenes.addAll(zoneSceneList.value)
        }

        // now find only the items that appear more than once
        val dupes = mutableSetOf<PHv2Scene>()
        allScenes.forEachIndexed { i, scene ->
            // get all the elements after i + 1
            val scenesAfter = allScenes.drop(i + 1)
            // is scene in the scenesAfter list (by name only)?
            scenesAfter.forEach { afterScene ->
                if (afterScene.metadata.name == scene.metadata.name) {
                    // Yep, here's a dupe!  Add it
                    dupes.add(scene)
                }
            }
        }
        return dupes.map { it.metadata.name }.toSet()
    }

} // data class SceneDataForFlock

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
    STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED,

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