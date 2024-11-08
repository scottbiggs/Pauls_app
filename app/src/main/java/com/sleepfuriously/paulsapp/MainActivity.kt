package com.sleepfuriously.paulsapp

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.paulsapp.model.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme
import com.sleepfuriously.paulsapp.ui.theme.almostBlack
import com.sleepfuriously.paulsapp.ui.theme.almostBlackLighter
import com.sleepfuriously.paulsapp.ui.theme.almostBlackMuchLighter
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.darkBlueLight
import com.sleepfuriously.paulsapp.ui.theme.darkBlueMain
import com.sleepfuriously.paulsapp.ui.theme.darkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.lightBlueLight
import com.sleepfuriously.paulsapp.ui.theme.lightBlueMain
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryLightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.yellowMain
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
    private lateinit var viewModel: MainViewModel

    /** accessor for splash screen viewmodel */
    private lateinit var splashViewmodel: SplashViewmodel


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw under system bars
        enableEdgeToEdge()

        viewModel  = MainViewModel()
        splashViewmodel = SplashViewmodel(this)

        // surest way to hide the action bar
        actionBar?.hide()

        // start initializations and splash screen
        splashViewmodel.checkIoT(this)
        showSplashScreen()

        setContent {
            PaulsAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // create composables of flows in the splashviewmodel
                    val wifiWorking by splashViewmodel.wifiWorking.collectAsStateWithLifecycle()
                    val iotTesting by splashViewmodel.iotTesting.collectAsStateWithLifecycle()
                    val philipsHueTestStatus by splashViewmodel.philipsHueTestStatus.collectAsStateWithLifecycle()

                    /** yup, this is pretty important--all the bridges */
                    val philipsHueBridges by splashViewmodel.philipsHueBridges.collectAsStateWithLifecycle()


                    if (iotTesting) {
                        TestSetupScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel = viewModel,
                            wifiWorking = wifiWorking ?: false,
                            philipsHueTest = philipsHueTestStatus,
                            philipsHueBridges = philipsHueBridges
                        )
                    }
                    else {
                        FourPanes(
                            0.3f,
                            splashViewmodel = splashViewmodel,
                            viewModel = viewModel,
                            bridges = philipsHueBridges
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
        splashViewmodel: SplashViewmodel,
        viewModel: MainViewModel,
        modifier : Modifier = Modifier,
        bridges: Set<PhilipsHueBridgeInfo>
    ) {
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
                        ShowMainScreenPhilipsHue(modifier, splashViewmodel, viewModel, bridges)
                    }
                    Box(modifier = niceBorderModifier
                        .weight(rightWeight)
                    ) {
                        Text("top right", modifier = Modifier.align(Alignment.Center), color = Color.White)
                    }
                }

                Row(modifier = Modifier
                    .weight(bottomWeight)
                ) {
                    Box(modifier = niceBorderModifier
                        .weight(leftWeight)
                    ) {
                        Text("bottom left", modifier = Modifier.align(Alignment.Center), color = Color.White)
                    }
                    Box(modifier = niceBorderModifier
                        .weight(rightWeight)
                    ) {
                        Text("bottom right", modifier = Modifier.align(Alignment.Center), color = Color.White)
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
     * Displays the philips hue stuff.
     *
     * @param   activeBridges       The bridges that are currently active in the house
     *                              that the app is running.
     */
    @Composable
    private fun ShowMainScreenPhilipsHue(
        modifier: Modifier = Modifier,
        splashViewModel: SplashViewmodel,
        viewModel: MainViewModel,
        bridges: Set<PhilipsHueBridgeInfo>,
    ) {

        val ctx = LocalContext.current

        // setup tests
        val testRooms = mutableSetOf(
            PhilipsHueRoomInfo("baby's room", mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )),

            PhilipsHueRoomInfo("2 times 3 time 4", mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )),

            PhilipsHueRoomInfo("living room", mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )),
            PhilipsHueRoomInfo("bedroom", mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )),
            PhilipsHueRoomInfo("kitchen", mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )),
        )

        val testBridges = setOf(
            PhilipsHueBridgeInfo("1", rooms = testRooms),
            PhilipsHueBridgeInfo("2", active = true),
            PhilipsHueBridgeInfo("3", active = true, rooms = testRooms),
            PhilipsHueBridgeInfo("4")
        )

        val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)


        Column(modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
        ) {

            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                columns = GridCells.Adaptive(200.dp),
                verticalArrangement = Arrangement.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                // Grouping the grids into bridges.  The first row will be the name
                // of the bridge.
                testBridges.forEach { bridge ->

                    // only draw bridges that are active
                    if (bridge.active) {
                        item( span = { GridItemSpan(this.maxLineSpan) } ) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(top = 12.dp),
                                color = darkCoolGray,
                                thickness = 5.dp
                            )
                        }

                        // The first item (which has the name of the bridge)
                        // will take the entire row of a grid.
                        item(
                            span = { GridItemSpan(this.maxLineSpan) }       // makes this item take entire row
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 2.dp, bottom = 8.dp),
                                text = "bridge: ${bridge.id}",
                                fontSize = 24.sp,
                                color = veryLightCoolGray
                            )
                        }

                        if (bridge.rooms.size == 0) {
                            // no rooms to display
                            item(span = { GridItemSpan(this.maxLineSpan) }) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                    text = noRoomsFound,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else {
                            // yes, there are rooms to display
                            bridge.rooms.forEach { room ->
                                item {
                                    DisplayPhilipsHueRoom(
                                        roomName = room.id,
                                        illumination = room.getAverageIllumination()
                                            .toFloat() / MAX_BRIGHTNESS.toFloat()
                                    ) {
                                        // todo call the illumination change in the viewmodel
                                        //  and make it display (var by remember in a slider?)
                                    }
                                }
                            }
                        }
                    }

                }
            }

        }


        // todo: show messages (if any)

        // and finally the fab
        ShowMainPhilipsHueAddBridgeFab(
            splashViewModel = splashViewModel,
            viewModel = viewModel,
            numActiveBridges = bridges.size     // fixme: this counts ALL bridges, not just active ones!
        )
    }

    /**
     * UI for a single room.
     *
     * @param   roomName        The name to display for this room
     *
     * @param   illumination    How much is this room currently illumniated.
     *                          0 = off all the way to 1 = full on.
     *
     * @param   roomChangedFunction     Function to call when the illumination is
     *                                  changed by the user.  It takes the new
     *                                  illumination value.
     */
    @Composable
    private fun DisplayPhilipsHueRoom(
        roomName: String,
        illumination: Float,
        roomChangedFunction: (newIllumination: Float) -> Unit
    ) {

        var sliderPosition by remember { mutableFloatStateOf(illumination) }
        var lightImage = getProperLightImage(sliderPosition)

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.secondary)),
                RoundedCornerShape(12.dp)
            )

        ) {
            Text(
                text = roomName,
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )

            Image(
                modifier = Modifier
                    .width(140.dp)
//                    .padding(end = 10.dp)
                    .align(Alignment.End),
                contentScale = ContentScale.Fit,
                painter = painterResource(id = lightImage),
                contentDescription = stringResource(id = R.string.lightbulb_content_desc)
            )

            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    lightImage = getProperLightImage(sliderPosition)
                    roomChangedFunction.invoke(sliderPosition)
                },
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 18.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(veryDarkCoolGray)
            )
            Text(
                text = stringResource(id = R.string.brightness, sliderPosition),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }

    private fun getProperLightImage(illumination: Float) : Int {
        val lightImage =
            if (illumination < 0.25f) {
                R.drawable.bare_bulb_white_04
            }
            else if (illumination < 0.5f) {
                R.drawable.bare_bulb_white_03
            }
            else if (illumination < 0.75f) {
                R.drawable.bare_bulb_white_02
            }
            else {
                R.drawable.bare_bulb_white_01
            }
        return lightImage
    }

    @Composable
    private fun ShowMainPhilipsHueAddBridgeFab(
        modifier: Modifier = Modifier,
        splashViewModel: SplashViewmodel,
        viewModel: MainViewModel,
        numActiveBridges: Int
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(38.dp)
        ) {
            // Add button to add a new bridge (if there are no active bridges, then
            // show the extended FAB)
            if (numActiveBridges == 0) {
                ExtendedFloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    onClick = { splashViewModel.beginAddPhilipsHueBridge() },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.add_button_content_desc)
                        )
                    },
                    text = { Text(stringResource(id = R.string.ph_add_button)) }
                )
            }
            else {
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd),
                    onClick = { splashViewModel.beginAddPhilipsHueBridge() },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.add_button_content_desc)
                    )
                }
            }
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
        viewModel: MainViewModel,
        wifiWorking: Boolean?,
        philipsHueTest: TestStatus,
        philipsHueBridges: Set<PhilipsHueBridgeInfo>,
    ) {
        Log.d(TAG, "TestSetupScreen()")

        Column(modifier = modifier.fillMaxSize()) {
            Text("wifi = $wifiWorking")
            philipsHueBridges.forEach() { bridge ->
                Text("   bridge ${bridge.id}: ip = ${bridge.ip}, token = ${bridge.token}, lastUsed = ${bridge.lastUsed}, active = ${bridge.active}")
            }
            Text("philips hue tests complete = $philipsHueTest")
        }

    }


    /**
     * Begins the process of manually initializing the IoT devices.
     */
    @Composable
    private fun TestBad(
        modifier : Modifier = Modifier,
        splashViewmodel: SplashViewmodel,
        wifiWorking: Boolean,
    ) {
        // start with the wifi
        if (wifiWorking == false) {
            WifiNotWorking(modifier)
        }

        else {
            // This should not happen.  But if it does, a message will appear
            // with an exit button.
            SimpleBoxMessage(
                modifier,
                "error: unhandled case in TestBad()",
                { finish() },
                stringResource(id = R.string.exit)
            )
        }
    }

    @Composable
    private fun WifiNotWorking(modifier: Modifier = Modifier) {
        SimpleBoxMessage(modifier,
            stringResource(id = R.string.wifi_not_working),
            { finish() },
            stringResource(id = R.string.exit)
        )
    }


    /**
     * @param   msgText        Message to display
     * @param   onClick     Function to run when the button is clicked.
     */
    @Composable
    fun SimpleBoxMessage(
        modifier: Modifier = Modifier,
        msgText: String = "error",
        onClick: () -> Unit,
        buttonText: String
        ) {
        Box(modifier = modifier
            .fillMaxSize()
            .padding(80.dp)
            .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    text = msgText,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = modifier
                        .wrapContentHeight()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onClick
                ) {
                    Text(buttonText)
                }
            }
        }
    }


    /**
     * Initializing the Philips Hue bridge, part 1.
     * The user simply has to fill in the IP of the bridge.
     * Information on how to do it is also displayed.
     */
    @Composable
    private fun PhilipsHueBridgeIpManualInit() {
        var ipText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.secondary)
        ) {

            Text(
                stringResource(id = R.string.enter_ip_hint),
                color = MaterialTheme.colorScheme.onSecondary
            )
            OutlinedTextField(
                value = ipText,
                label = { Text(stringResource(id = R.string.enter_ip)) },
                singleLine = true,
                onValueChange = { ipText = it }
            )
        }

    }


    /**
     * This requires the user to hit the big button on the Philips
     * Hue bridge and THEN hit the go button on their device.  This
     * starts the sequence of the bridge sending back the secret
     * name for this app to use.
     */
    @Composable
    private fun DisplayManualInitPart2() {

    }


    /**
     * Handles displaying the splash screen.  Needs to be
     * called BEFORE setContent() in [onCreate].
     *
     * preconditions:
     *      splashScreenViewmodel       ready to use
     */
    private fun showSplashScreen() {
        // Needs to be called before setContent() or setContentView().
        // apply{} is optional--use to do any additional work.
        installSplashScreen().apply {
            // example: this will check the value every frame and keep showing
            //          the splash screen as long as the total value is true
            setKeepOnScreenCondition {
                splashViewmodel.iotTesting.value
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

    /**
     * Walks the user through the initialization process of the philips
     * hue bridge
     */
    @Composable
    private fun ManualBridgeSetup(
        viewmodel: SplashViewmodel,
        initBridgeState: BridgeInitStates,
        modifier: Modifier = Modifier,
    ) {
        val config = LocalConfiguration.current
        val landscape = config.orientation == ORIENTATION_LANDSCAPE
        val screenHeight = config.screenHeightDp
        val screenWidth = config.screenWidthDp

        when (initBridgeState) {
            BridgeInitStates.NOT_INITIALIZING -> {
                // this should not happen.
                SimpleBoxMessage(
                    modifier,
                    "error: should not be in ManualBridgeSetup() with this bridge init state.",
                    { finish() },
                    stringResource(id = R.string.exit)
                )
            }
            BridgeInitStates.STAGE_1_GET_IP,
            BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT,
            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> {
                if (landscape) {
                    ManualBridgeSetupStep1_Landscape(viewmodel, initBridgeState)
                }
                else {
                    ManualBridgeSetupStep1_Portrait(viewmodel, initBridgeState)
                }
            }
            BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON,
            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE -> {
                // todo
            }
            BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
                // todo
            }
        }
    }

    @Composable
    private fun ManualBridgeSetupStep1_Landscape(
        splashViewModel: SplashViewmodel,
        state: BridgeInitStates
    ) {

        val ctx = LocalContext.current
        var ipText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(id = R.string.find_the_bridge_ip),
                fontSize = 28.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    Image(
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_1),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_1_desc)
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_1)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    Image(
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_2),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_2_desc)
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_2)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    Image(
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_3),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_3_desc)
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_3)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        value = ipText,
                        label = { Text(stringResource(id = R.string.enter_ip)) },
                        singleLine = true,
                        onValueChange = { ipText = it },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Decimal
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { splashViewModel.AddPhilipsHueBridgeIp(ipText) }
                        )
                    )
                    Button(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                            .width(120.dp),
                        onClick = { splashViewModel.AddPhilipsHueBridgeIp(ipText) } ) {
                        Text(stringResource(id = R.string.next))
                    }
                }
            }

        }

        // display any error messages
        if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
            Toast.makeText(ctx, stringResource(R.string.new_bridge_stage_1_error_bad_ip_format), Toast.LENGTH_LONG).show()
            splashViewmodel.bridgeAddErrorMsgIsDisplayed()
        }
        if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
            Toast.makeText(ctx, stringResource(R.string.new_bridge_stage_1_error_bad_ip_format), Toast.LENGTH_LONG).show()
            splashViewmodel.bridgeAddErrorMsgIsDisplayed()
        }

    }

    @Composable
    private fun ManualBridgeSetupStep1_Portrait(
        splashViewModel: SplashViewmodel,
        state: BridgeInitStates
    ) {
        val ctx = LocalContext.current
        var ipText by remember { mutableStateOf("") }

        val config = LocalConfiguration.current
        val screenHeight = config.screenHeightDp
        val screenWidth = config.screenWidthDp

        //----------------
        // We want each column to be just small enough on the screen so that
        // a bit of the next column peeks through.  This will let the user
        // know that they can scroll right to see more.
        //
        // We also want everything to fit vertically.  That is the height
        // of that column (including text and buttons) plus the title at
        // the top is less than the height of the device.
        //
        // To do this, we simply calculate the needed width (easy)
        //
        val columnWidth = screenWidth.toFloat() * 0.85


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(id = R.string.find_the_bridge_ip),
                fontSize = 28.sp
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                state = rememberLazyListState(initialFirstVisibleItemIndex = 0) // alwasy start at the beginning
            ) {
                item {
                    LazyColumn(
                        Modifier
                            .fillMaxHeight()
                            .width(columnWidth.dp)
                            .weight(1f)
                    ) {
                        item {
                            Image(
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_1),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_1_desc)
                            )
                        }
                        item {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                                text = stringResource(id = R.string.bridge_ip_step_1)
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                item {
                    LazyColumn(
                        Modifier
                            .fillMaxHeight()
                            .width(columnWidth.dp)
                            .weight(1f)
                    ) {
                        item {
                            Image(
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_2),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_2_desc)
                            )
                        }
                        item {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                                text = stringResource(id = R.string.bridge_ip_step_2)
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                item {

                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .width(columnWidth.dp)
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Image(
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_3),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_3_desc)
                            )
                        }
                        item {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                                text = stringResource(id = R.string.bridge_ip_step_3)
                            )
                        }
                        item {
                            Row {
                                OutlinedTextField(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .align(Alignment.CenterVertically),
                                    value = ipText,
                                    label = { Text(stringResource(id = R.string.enter_ip)) },
                                    singleLine = true,
                                    onValueChange = { ipText = it },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next,
                                        keyboardType = KeyboardType.Decimal
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { splashViewModel.AddPhilipsHueBridgeIp(ipText) }
                                    )
                                )

                                Button(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(top = 8.dp, start = 12.dp)
                                        .width(120.dp),
                                    onClick = { splashViewModel.AddPhilipsHueBridgeIp(ipText) }) {
                                    Text(stringResource(id = R.string.next))
                                }
                            }
                        }
                    }

                }
            }

        }

        // display any error messages
        if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
            Toast.makeText(ctx, stringResource(R.string.new_bridge_stage_1_error_bad_ip_format), Toast.LENGTH_LONG).show()
            splashViewmodel.bridgeAddErrorMsgIsDisplayed()
        }
        if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
            Toast.makeText(ctx, stringResource(R.string.new_bridge_stage_1_error_bad_ip_format), Toast.LENGTH_LONG).show()
            splashViewmodel.bridgeAddErrorMsgIsDisplayed()
        }

    }

    //----------------------------
    //  previews
    //----------------------------
/*
    @Preview(
        name = "ManualBridgeSetupStep1 Landscape (night)",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
        device = Devices.PIXEL_TABLET
        )
    @Composable
    private fun ManualBridgeSetupStep1LandscapePreview() {
        val tmpViewmodel = SplashViewmodel()
        ManualBridgeSetupStep1_Landscape(splashViewModel = tmpViewmodel)
    }
*/
    @Preview(
        name = "ManualBridgeSetupStep1 Portrait Normal (night)",
        uiMode = Configuration.UI_MODE_NIGHT_YES,
        device = Devices.PIXEL_TABLET
    )
    @Composable
    private fun ManualBridgeSetupStep1PortriatPreviewNormal() {
        val ctx = LocalContext.current
        val tmpViewmodel = SplashViewmodel(ctx)
        ManualBridgeSetupStep1_Portrait(
            splashViewModel = tmpViewmodel,
            state = BridgeInitStates.STAGE_1_GET_IP
        )
    }


    /*
        @Preview(name = "init part 1", uiMode = Configuration.UI_MODE_NIGHT_YES)
        @Composable
        private fun DisplayManualInitPart1Preview() {
            PhilipsHueBridgeIpManualInit()
        }

        @Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
        @Composable
        private fun SplashScreenContentsPreview() {
            SplashScreenContents()
        }
    */
    /*
        @Preview(
            name = "tablet",
            device = "spec:shape=Normal,width=1240,height=640,unit=dp,dpi=480",
            showSystemUi = true,
            showBackground = true
        )
        @Composable
        private fun ShowMainScreenPreview() {
            PaulsAppTheme {
                ShowMainScreen(viewModel = viewModel)
            }
        }
    */

}




//----------------------------
//  constants
//----------------------------

private const val TAG = "MainActivity"