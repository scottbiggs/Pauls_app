package com.sleepfuriously.paulsapp

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.paulsapp.compose.philipshue.ManualBridgeSetup
import com.sleepfuriously.paulsapp.compose.philipshue.ShowMainScreenPhilipsHue
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesForRoom
import com.sleepfuriously.paulsapp.compose.SimpleFullScreenBoxMessage
import com.sleepfuriously.paulsapp.compose.philipshue.ShowAddOrEditFlockDialog
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesForFlock
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesForZone
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.viewmodels.MainViewmodel
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForFlock
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForRoom
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForZone
import com.sleepfuriously.paulsapp.viewmodels.TestStatus
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import com.sleepfuriously.paulsapp.viewmodels.MyViewModelInterface


/**
 * Paul's project
 *
 * Startup
 *
 *      - Start the UI in initializing state
 *
 *          - Each UI section will start its viewmodel
 *
 *              - The viewmodel wil initialize its model
 *
 *      - The models will update the viewmodels when its initialization
 *        is complete.  This will cause state changes.
 *
 *      - UI updates as states change
 */
class MainActivity : ComponentActivity() {

    /** access to the view model */
    private val mainViewmodel by viewModels<MainViewmodel>()

    /** All the Viewmodels used in the tabs */
    private val philipsHueViewmodel by viewModels<PhilipsHueViewmodel>()
//    val nestViewmodel by viewModels<NestViewmodel>()      // todo
//    val poolViewmodel by viewModels<PoolViewmodel>()
//    val securityViewmodel by viewModels<SecurityViewmodel>()


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw under system bars
        enableEdgeToEdge()

        // surest way to hide the action bar
        actionBar?.hide()

        /** Used for tabs.  Should be THE place to access the viewmodel. */
        val tabViewmodels = listOf(
            philipsHueViewmodel
            // todo: add all the tabbed components' viewmodels here!
        )

        setContent {

            // start initializations and splash screen
            philipsHueViewmodel.checkIoT(this)      // fixme: this needs to be moved!!!
//                    showSplashScreen()

            PaulsAppTheme {

                // create data to receive state flows from the philips hue viewmodel
                val wifiWorking by philipsHueViewmodel.wifiWorking.collectAsStateWithLifecycle()
                val iotTestingState by philipsHueViewmodel.iotTestingState.collectAsStateWithLifecycle()
                val iotTestingErrorMsg by philipsHueViewmodel.iotTestingErrorMsg.collectAsStateWithLifecycle()
                val philipsHueTestStatus by philipsHueViewmodel.philipsHueTestStatus.collectAsStateWithLifecycle()
                val addNewBridgeState by philipsHueViewmodel.addNewBridgeState.collectAsStateWithLifecycle()
                val philipsHueFinishNow by philipsHueViewmodel.crashNow.collectAsStateWithLifecycle()
                val showWaitSpinner by philipsHueViewmodel.waitingForResponse.collectAsStateWithLifecycle()

                val philipsHueBridges = philipsHueViewmodel.philipsHueBridgesCompose

                val roomSceneData = philipsHueViewmodel.sceneDisplayStuffForRoom.collectAsStateWithLifecycle()
                val zoneSceneData = philipsHueViewmodel.sceneDisplayStuffForZone.collectAsStateWithLifecycle()
                val flockSceneData = philipsHueViewmodel.sceneDisplayStuffForFlock.collectAsStateWithLifecycle()

                val flocks = philipsHueViewmodel.flocks.collectAsStateWithLifecycle()
                val addFlock = philipsHueViewmodel.showAddOrEditFlockDialog.collectAsStateWithLifecycle()
                val addFlockError = philipsHueViewmodel.addFlockErrorMsg.collectAsStateWithLifecycle()

                val currentTab = mainViewmodel.activeTab.collectAsStateWithLifecycle()

                // Before anything, do we need to exit?
                if (philipsHueFinishNow) {
                    Log.d(TAG, "Finishing!!!")
                    finish()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    //------------------------
                    //  What to display?  Depends on what's happening.
                    //
                    //  - going through the initial testing cycle
                    //      -> testing screen
                    //  - addNewBridgeState is something other than NOT_INITIALIZING
                    //      -> we need to handle the initialization
                    //  - normal state
                    //      -> FourPanes
                    if (iotTestingState == TestStatus.TESTING) {
                        TestSetupScreen(
                            modifier = Modifier.padding(innerPadding),
                            wifiWorking = wifiWorking ?: false,
                            philipsHueTest = philipsHueTestStatus,
                            viewmodel = philipsHueViewmodel
                        )
                    }

                    else if (wifiWorking == false) {
                        TestBad(
                            wifiWorking = false,
                            errorMsg = stringResource(R.string.wifi_not_working)
                        )
                    }

                    else if (iotTestingState == TestStatus.TEST_BAD) {
                        TestBad(
                            wifiWorking = wifiWorking ?: false,
                            errorMsg = iotTestingErrorMsg
                        )
                    }

                    else if (addNewBridgeState != BridgeInitStates.NOT_INITIALIZING) {
                        Log.d(TAG, "in onCreate(), showWaitSpinner = $showWaitSpinner")
                        ManualBridgeSetup(
                            parentActivity = this,
                            philipsHueViewmodel = philipsHueViewmodel,
                            waitingForResults = showWaitSpinner,
                            initBridgeState = addNewBridgeState)
                    }
                    else {
                        // show the tabs
                        ShowViewmodelTabs(
                            mainViewmodel = mainViewmodel,
                            viewmodelTabs = tabViewmodels,
                            currentTab = currentTab.value,
                            philipsHueBridges = philipsHueBridges,
                            roomSceneData = roomSceneData.value,
                            zoneSceneData = zoneSceneData.value,
                            flockSceneData = flockSceneData.value,
                            philipsHueFlocks = flocks.value,
                            showAddFlock = addFlock.value,
                            addFlockErrorMsg = addFlockError.value
                        )
                    }

                }
            }
        }

    } // onCreate()


    //----------------------------
    //  composables
    //----------------------------

    /**
     * Main display.  Shows the tabs and their contents.
     *
     * @param   mainViewmodel       The ViewModel for the main screen.
     *
     * @param   viewmodelTabs       List of the Viewmodels that are used for tabs.
     *
     * @param   currentTab          Index to the list of the tab to show.
     */
    @OptIn(ExperimentalFoundationApi::class)    // needed for rememberPagerState
    @Composable
    fun ShowViewmodelTabs(
        modifier: Modifier = Modifier,
        mainViewmodel: MainViewmodel,
        viewmodelTabs: List<MyViewModelInterface>,
        currentTab: Int,
        roomSceneData: SceneDataForRoom?,
        zoneSceneData: SceneDataForZone?,
        flockSceneData: SceneDataForFlock?,
        philipsHueBridges: List<PhilipsHueBridgeInfo>,
        philipsHueFlocks: List<PhilipsHueFlockInfo>,
        showAddFlock: Boolean,
        addFlockErrorMsg: String,
    ) {
        val ctx = LocalContext.current
        val pagerState = rememberPagerState {
            viewmodelTabs.size
        }

        // is triggered when a tab is hit (not swipes)
        LaunchedEffect(currentTab) {
            // tell the pager that we changed pages (and which page we changed to)
            pagerState.animateScrollToPage(currentTab)
        }

        // And another LaunchedEffect for the swiper to tell the Tabs to change
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            if (pagerState.isScrollInProgress == false) {
                // Don't do anything WHILE a scroll is in progress.  Wait for the
                // animation to finish and THEN set the tab.
                mainViewmodel.changeTab(pagerState.currentPage)
            }
        }

        // now for some Composable stuff
        Column(modifier = modifier.fillMaxSize()) {
            // Start with the TabRow at the top
            TabRow(selectedTabIndex = currentTab) {
                viewmodelTabs.forEachIndexed { index, thisTabViewmodel ->
                    Tab(
                        selected = index == currentTab,
                        onClick = {
                            mainViewmodel.changeTab(index)
                        },
                        text = { Text(thisTabViewmodel.getTitle(ctx)) },
                        icon = {
                            Icon(
                                imageVector = if (index == currentTab) {
                                    thisTabViewmodel.getSelectedIcon()
                                } else {
                                    thisTabViewmodel.getUnselectedIcon()
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }


            // Provides the ability to swipe between tabs
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)     // makes sure that the pager gets everything below the TabRow
            ) { index ->    // index to the current page! Cool!

                when (viewmodelTabs[index]) {
                    is PhilipsHueViewmodel -> {
                        ShowPhilipsHue(
                                philipsHueViewmodel = viewmodelTabs[index] as PhilipsHueViewmodel,
                            philipsHueBridges = philipsHueBridges,
                            roomSceneData = roomSceneData,
                            zoneSceneData = zoneSceneData,
                            flockSceneData = flockSceneData,
                            philipsHueFlocks = philipsHueFlocks,
                            showAddFlock = showAddFlock,
                            addFlockErrorMsg = addFlockErrorMsg
                        )
                    }

                    //
                    // todo: add more tabs as I do those parts of the app
                    //

                    // Couldn't figure out the viewmodel type!!!
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("error: What the hell viewmodel is this?")
                        }
                    }
                }
            }


        }
    }

    @Composable
    fun ShowPhilipsHue(
        philipsHueViewmodel: PhilipsHueViewmodel,
        roomSceneData: SceneDataForRoom?,
        zoneSceneData: SceneDataForZone?,
        flockSceneData: SceneDataForFlock?,
        philipsHueBridges: List<PhilipsHueBridgeInfo>,
        philipsHueFlocks: List<PhilipsHueFlockInfo>,
        showAddFlock: Boolean,
        addFlockErrorMsg: String,
        modifier : Modifier = Modifier,
    ) {
        val ctx = LocalContext.current

        if (roomSceneData != null) {
            // show room/scene specific info
            ShowScenesForRoom(
                bridge = roomSceneData.bridge,
                room = roomSceneData.room,
                scenes = roomSceneData.scenes,
                viewmodel = philipsHueViewmodel,
                onDismiss = { philipsHueViewmodel.stopShowingScenes() }
            )
        }

        else if (zoneSceneData != null) {
            // show zone/scene info
            ShowScenesForZone(
                bridge = zoneSceneData.bridge,
                zone = zoneSceneData.zone,
                scenes = zoneSceneData.scenes,
                viewmodel = philipsHueViewmodel,
                onDismiss = { philipsHueViewmodel.stopShowingScenes() }
            )
        }

        else if (flockSceneData != null) {
            // show flock scene info
            ShowScenesForFlock(
                flockSceneData = flockSceneData,
                viewmodel = philipsHueViewmodel,
                onDismiss = { philipsHueViewmodel.stopShowingScenes() }
            )
        }

        else if (showAddFlock) {
            ShowAddOrEditFlockDialog(
                errorMsg = addFlockErrorMsg,
                viewmodel = philipsHueViewmodel,
                allRooms = philipsHueViewmodel.getAllRooms(),
                allZones = philipsHueViewmodel.getAllZones(),
                onToggled = { turnedOn, room, zone ->
                    philipsHueViewmodel.toggleFlockList(turnedOn, room, zone)
                },
                onOk = { flockName ->
                    philipsHueViewmodel.addOrEditFlockComplete(flockName, ctx)
                }
            )
        }

        else {
            // show regular PH stuff
            ShowMainScreenPhilipsHue(
                modifier = modifier,
                philipsHueViewmodel = philipsHueViewmodel,
                bridges = philipsHueBridges,
                flocks = philipsHueFlocks
            )
        }
    }


    /**
     * Displays the content of the splash screen.  Animations should be
     * done at the caller level.
     */
    @Composable
    private fun SplashScreenContents(modifier: Modifier = Modifier) {

        Log.d(TAG, "SplashScreenContents()")

        Box(modifier = modifier) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(50.dp)
                    .background(color = MaterialTheme.colorScheme.tertiary),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.initializing),
                    color = MaterialTheme.colorScheme.onTertiary
                )
                CircularProgressIndicator()
            }
        }
    }

    /**
     * This screen handles gathering inputs to complete setup for the user.
     */
    @Composable
    fun TestSetupScreen(
        modifier : Modifier = Modifier,
        wifiWorking: Boolean?,
        philipsHueTest: TestStatus,
        viewmodel: PhilipsHueViewmodel,
    ) {
        Log.d(TAG, "TestSetupScreen()")

        val ctx = LocalContext.current

        Column(modifier = modifier.fillMaxSize()) {

            // wifi
            Text("wifi = $wifiWorking")

            // list known bridges
            Text("here the known bridges:")
//            viewmodel.philipsHueBridgeModelsCompose.forEach { bridgeModel ->
//                bridgeModel.bridge.value?.let { bridge ->
//                    Text("   bridge ${bridge.id}: ip = ${bridge.ipAddress}, token = ${bridge.token}, active = ${bridge.active}")
//                }
//            }
            viewmodel.philipsHueBridgesCompose.forEach { bridge ->
                Text("   bridge ${bridge.v2Id}: ip = ${bridge.ipAddress}, token = ${bridge.token}, active = ${bridge.active}")
            }


            Text("push to test synchronous PUT to bridge")
            Button(
                onClick = {
                    Toast.makeText(ctx, "get this part working, scott!", Toast.LENGTH_LONG).show()
//                    philipsHueViewmodel.testPutToBridge()
            }) {
                Text("go")
            }

            Text("philips hue tests completeness = $philipsHueTest")
        }

    }


    /**
     * Begins the process of manually initializing the IoT devices.
     */
    @Composable
    private fun TestBad(
        modifier : Modifier = Modifier,
        wifiWorking: Boolean,
        errorMsg: String = ""
    ) {
        // start with the wifi
        if (wifiWorking == false) {
            TestErrorMessage(modifier, errorMsg)
        }

        else {
            SimpleFullScreenBoxMessage(
                backgroundModifier = modifier,
                msgText = errorMsg,
                buttonText = stringResource(id = R.string.exit),
                onClick = { finish() }
            )
        }
    }

    @Composable
    private fun TestErrorMessage(modifier: Modifier = Modifier, errMsg: String) {
        SimpleFullScreenBoxMessage(
            backgroundModifier = modifier,
            msgText = errMsg,
            buttonText = stringResource(id = R.string.exit),
            onClick = { finish() }
        )
    }


    /**
     * Handles displaying the splash screen.  Needs to be
     * called BEFORE setContent() in [onCreate].
     *
     * preconditions:
     *      philipsHueViewmodel       ready to use
     */
    private fun showSplashScreen() {
        // Needs to be called before setContent() or setContentView().
        // apply{} is optional--use to do any additional work.
        installSplashScreen().apply {
            // example: this will check the value every frame and keep showing
            //          the splash screen as long as the total value is true
            setKeepOnScreenCondition {
                philipsHueViewmodel.iotTestingState.value == TestStatus.TESTING
            }

            // set the exit animation
            setOnExitAnimationListener { screen ->
                val zoomX = ObjectAnimator.ofFloat(
                    screen.iconView,
                    View.SCALE_X,
                    0.7f,       // start will be the final value in logo_animator.xml
                    8.0f                // what we'll finish with (expand to entire width of screen)
                )
                zoomX.interpolator = OvershootInterpolator(0f)    // provides a bump when animating (2f is default)
                zoomX.duration = 750L      // duration of exit animation
                zoomX.doOnEnd { screen.remove() }   // when done, remove this screen and go on to next

                // same for y value
                val zoomY = ObjectAnimator.ofFloat(
                    screen.iconView,
                    View.SCALE_Y,
                    0.7f,
                    0.0f                // go to nothing
                )
                zoomY.interpolator = OvershootInterpolator(0f)
                zoomY.duration = 750L
                zoomY.doOnEnd { screen.remove() }

                // finally start the animations
                zoomX.start()
                zoomY.start()
            }
        }

    }

}

//----------------------------
//  constants
//----------------------------

private const val TAG = "MainActivity"

