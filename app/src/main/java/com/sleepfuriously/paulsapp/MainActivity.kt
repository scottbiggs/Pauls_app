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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sleepfuriously.paulsapp.compose.philipshue.ManualBridgeSetup
import com.sleepfuriously.paulsapp.compose.philipshue.ShowMainScreenPhilipsHue
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesForRoom
import com.sleepfuriously.paulsapp.compose.SimpleFullScreenBoxMessage
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme
import com.sleepfuriously.paulsapp.ui.theme.almostBlack
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.viewmodels.SceneData
import com.sleepfuriously.paulsapp.viewmodels.TestStatus
import kotlin.math.roundToInt


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

    //----------------------------
    //  additional properties
    //----------------------------

    /** access to the view model */
//    private lateinit var viewmodel: MainViewmodel

    /** accessor for philps hue viewmodel */
    private lateinit var philipsHueViewmodel: PhilipsHueViewmodel


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw under system bars
        enableEdgeToEdge()

        // surest way to hide the action bar
        actionBar?.hide()


        setContent {

            philipsHueViewmodel = viewModel<PhilipsHueViewmodel>(this)
            // start initializations and splash screen
            philipsHueViewmodel.checkIoT(this)
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

//                val philipsHueBridges = philipsHueViewmodel.philipsHueBridgeModelsCompose
                val philipsHueBridges = philipsHueViewmodel.philipsHueBridgesCompose

                val roomSceneData = philipsHueViewmodel.sceneDisplayStuff.collectAsStateWithLifecycle()

                // Before anything, do we need to exit?
                if (philipsHueFinishNow) {
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
                        Log.d(TAG, "about to show FourPanes()! iotTesting = $iotTestingState, addNewBridgeState = $addNewBridgeState")
                        FourPanes(
                            0.3f,
                            philipsHueViewmodel = philipsHueViewmodel,
                            philipsHueBridges = philipsHueBridges,
                            roomSceneData = roomSceneData.value
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
     * The primary display.  Breaks the screen into four arrange-able panes.
     *
     * @param   minPercent      Tells the most that a pane can shrink.  For example
     *                          0.1 means that the pane can get to 10% its original
     *                          size.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun FourPanes(
        minPercent : Float,
        philipsHueViewmodel: PhilipsHueViewmodel,
        roomSceneData:  SceneData?,
//        philipsHueBridges: List<PhilipsHueBridgeModel>,
        philipsHueBridges: List<PhilipsHueBridgeInfo>,
        modifier : Modifier = Modifier,
    ) {

        Log.d(TAG, "FourPanes() start.  num bridges = ${philipsHueViewmodel.philipsHueBridgesCompose.size}")
        Log.d(TAG, "roomSceneData = $roomSceneData")

        //-------------
        // these are the offsets from the center of the drawing area
        //
        var offsetPixelsX by remember { mutableFloatStateOf(0f) }
        var offsetPixelsY by remember { mutableFloatStateOf(0f) }

        //-------------
        // weights
        //
        var leftWeight by remember { mutableFloatStateOf(1f) }
        var rightWeight by remember { mutableFloatStateOf(1f) }
        var topWeight by remember { mutableFloatStateOf(1f) }
        var bottomWeight by remember { mutableFloatStateOf(1f) }



        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(color = almostBlack)
//                .safeGesturesPadding()     // takes the insets into account (nav bars, etc)
//                .safeContentPadding()      // takes the insets into account (nav bars, etc)
                .safeDrawingPadding()
        ) {
            // Get the pix and dp sizes of the screen.
            // Don't know if I need to do by remember or simply an assignment.
            val screenWidthDp by remember { mutableStateOf(maxWidth) }
            val screenHeightDp by remember { mutableStateOf(maxHeight) }

            Log.d(TAG, "screen dp = ($screenWidthDp, $screenHeightDp)")

            val screenWidthPx = constraints.maxWidth
            val screenHeightPx = constraints.maxHeight

            Log.d(TAG, "screen px = ($screenWidthPx, $screenHeightPx)")

            // set the min and max values of the offset
            val minScreenWidthPx by remember { mutableFloatStateOf(screenWidthPx * -(1f - minPercent) / 2f) }
            val maxScreenWidthPx by remember { mutableFloatStateOf(screenWidthPx * (1f - minPercent) / 2f) }
            val minScreenHeightPx by remember { mutableFloatStateOf(screenHeightPx * -(1f - minPercent) / 2f) }
            val maxScreenHeightPx by remember { mutableFloatStateOf(screenHeightPx * (1f - minPercent) / 2f) }

            Log.d(TAG, "$minScreenWidthPx < screenWidthPx < $maxScreenWidthPx")

            // set the weights
            leftWeight = 0.5f + (offsetPixelsX / screenWidthPx)
            rightWeight = 1f - leftWeight

            topWeight = 0.5f + (offsetPixelsY / screenHeightPx)
            bottomWeight = 1f - topWeight

            // create the stroke for the border around each of the panes
            val niceBorderModifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.primary)),
                    RoundedCornerShape(12.dp)
                )

            //
            // Now to draw the four panes.  This is done by a column
            // of two rows.
            //
            Column {
                Row(modifier = Modifier
                    .weight(topWeight)
                ) {
                    Box(modifier = niceBorderModifier
                        .weight(leftWeight)
                    ) {

                        //---------------------
                        //  Philips Hue
                        //
                        if (roomSceneData == null) {
                            // show regular PH stuff
                            ShowMainScreenPhilipsHue(
                                modifier = modifier,
                                philipsHueViewmodel = philipsHueViewmodel,
                                bridges = philipsHueBridges
                            )
                        }
                        else {
                            // show room/scene specific info
                            ShowScenesForRoom(
                                bridge = roomSceneData.bridge,
                                room = roomSceneData.room,
                                scenes = roomSceneData.scenes,
                                viewmodel = philipsHueViewmodel,
                                onDismiss = { philipsHueViewmodel.dontShowScenes() }
                            )
                        }
                    }
                    Box(modifier = niceBorderModifier
                        .weight(rightWeight)
                    ) {
                        //---------------------
                        //  Nest
                        //
                        Column {
                            Text(
                                stringResource(id = R.string.nest_main_title),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 32.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.headlineLarge,      // the proper way to change text sizes
                                color = Color.White
                            )
                            // todo: show nest
                        }
                    }
                }

                Row(modifier = Modifier
                    .weight(bottomWeight)
                ) {
                    Box(modifier = niceBorderModifier
                        .weight(leftWeight)
                    ) {
                        //---------------------
                        //  Pool
                        //
                        Column {
                            Text(
                                stringResource(id = R.string.pool_main_title),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 32.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                            // todo: show pool
                        }
                    }
                    Box(modifier = niceBorderModifier
                        .weight(rightWeight)
                    ) {
                        //---------------------
                        //  Security
                        //
                        Column {
                            Text(
                                stringResource(id = R.string.security_main_title),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, start = 32.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White
                            )
                            // todo: show security

                            // the version number
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                Alignment.BottomEnd
                            ) {
                                // display version number in bottom right corner
                                Text(
                                    text = getVersionName(this@MainActivity),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(end = 4.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

            }

            //---------------
            // the cursor
            //
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(70.dp)
                        .width(70.dp)
                        .offset {
                            IntOffset(
                                offsetPixelsX.roundToInt(),
                                offsetPixelsY.roundToInt()
                            )
                        }
                        .draggable2D(
                            state = rememberDraggable2DState { delta ->

                                // all this math is to keep the stuff within the boundaries
                                val tmpOffsetX = offsetPixelsX + delta.x
                                offsetPixelsX = if (tmpOffsetX < minScreenWidthPx) {
                                    minScreenWidthPx
                                } else if (tmpOffsetX > maxScreenWidthPx) {
                                    maxScreenWidthPx
                                } else {
                                    tmpOffsetX
                                }

                                val tmpOffsetY = offsetPixelsY + delta.y
                                offsetPixelsY = if (tmpOffsetY < minScreenHeightPx) {
                                    minScreenHeightPx
                                } else if (tmpOffsetY > maxScreenHeightPx) {
                                    maxScreenHeightPx
                                } else {
                                    tmpOffsetY
                                }

                            }
                        ),

                    painter = painterResource(id = R.drawable.four_dots),
                    contentDescription = stringResource(id = R.string.cross_handle_content_desc),
//                alpha = 0f      // makes the image invisible!
                )
            }

        }

    } // FourPanes()


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


    //----------------------------
    //  previews
    //----------------------------


}

//----------------------------
//  constants
//----------------------------

private const val TAG = "MainActivity"

