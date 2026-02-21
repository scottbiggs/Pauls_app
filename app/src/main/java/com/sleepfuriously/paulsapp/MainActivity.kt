package com.sleepfuriously.paulsapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.paulsapp.compose.SimpleFullScreenBoxMessage
import com.sleepfuriously.paulsapp.compose.philipshue.ManualBridgeSetup
import com.sleepfuriously.paulsapp.compose.philipshue.ShowAddOrEditFlockDialog
import com.sleepfuriously.paulsapp.compose.philipshue.ShowMainScreenPhilipsHue
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesRoomsZones
import com.sleepfuriously.paulsapp.compose.philipshue.ShowScenesForFlock
import com.sleepfuriously.paulsapp.compose.pool.ShowMainPool
import com.sleepfuriously.paulsapp.compose.sprinkler.ShowMainSprinkler
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.ui.theme.LocalTheme
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.dallasLightColorScheme
import com.sleepfuriously.paulsapp.ui.theme.darkColorScheme
import com.sleepfuriously.paulsapp.viewmodels.ActiveSystem
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.viewmodels.ClimateViewmodel
import com.sleepfuriously.paulsapp.viewmodels.MainViewmodel
import com.sleepfuriously.paulsapp.viewmodels.MyViewModelInterface
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.viewmodels.PoolViewmodel
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForFlock
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForRoom
import com.sleepfuriously.paulsapp.viewmodels.SceneDataForZone
import com.sleepfuriously.paulsapp.viewmodels.SecurityViewmodel
import com.sleepfuriously.paulsapp.viewmodels.SprinklerViewmodel
import com.sleepfuriously.paulsapp.viewmodels.TestStatus
import dev.romainguy.kotlin.math.Float2


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
    private val climateViewModel by viewModels<ClimateViewmodel>()
    val securityViewmodel by viewModels<SecurityViewmodel>()
    val sprinklerViewmodel by viewModels<SprinklerViewmodel>()
    val poolViewmodel by viewModels<PoolViewmodel>()


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
            philipsHueViewmodel,
            poolViewmodel,
            climateViewModel,
            sprinklerViewmodel,
            securityViewmodel
        )

        setContent {

            // start initializations
            mainViewmodel.initialize(this)
            while (mainViewmodel.wifiWorking.value == null) {
                Thread.sleep(100)
            }
            // if no internet, exit immediately.
            if (mainViewmodel.wifiWorking.value == false) {
                Toast.makeText(
                    this,
                    stringResource(R.string.wifi_not_working),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }

            philipsHueViewmodel.initialize()

            // This is the special stuff--required to get the LocalTheme going.
//            val themeColors = if (isSystemInDarkTheme()) defaultDarkColorScheme else defaultLightColorScheme
            val themeColors = if (isSystemInDarkTheme()) darkColorScheme else dallasLightColorScheme
            CompositionLocalProvider(LocalTheme provides themeColors) {

                PaulsAppTheme {

                    //
                    // flow data from viewmodels
                    //

                    // create data to receive state flows from the philips hue viewmodel
                    val wifiWorking by mainViewmodel.wifiWorking.collectAsStateWithLifecycle()
                    val mainViewmodelInitializing by mainViewmodel.intializing.collectAsStateWithLifecycle()

                    val phTestingState by philipsHueViewmodel.phTestingState.collectAsStateWithLifecycle()
                    val phTestingErrorMsg by philipsHueViewmodel.phTestingErrorMsg.collectAsStateWithLifecycle()
                    val addNewBridgeState by philipsHueViewmodel.addNewBridgeState.collectAsStateWithLifecycle()
                    val philipsHueFinishNow by philipsHueViewmodel.crashNow.collectAsStateWithLifecycle()
                    val showWaitSpinner by philipsHueViewmodel.waitingForResponse.collectAsStateWithLifecycle()

                    val philipsHueBridges = philipsHueViewmodel.philipsHueBridgesCompose
                    val philipsHueInitializing = philipsHueViewmodel.philipsHueInitializing

                    val roomSceneData =
                        philipsHueViewmodel.sceneDisplayStuffForRoom.collectAsStateWithLifecycle()
                    val zoneSceneData =
                        philipsHueViewmodel.sceneDisplayStuffForZone.collectAsStateWithLifecycle()
                    val flockSceneData =
                        philipsHueViewmodel.sceneDisplayStuffForFlock.collectAsStateWithLifecycle()

                    val flocks = philipsHueViewmodel.flocks.collectAsStateWithLifecycle()
                    val addFlock =
                        philipsHueViewmodel.showAddOrEditFlockDialog.collectAsStateWithLifecycle()
                    val addFlockError =
                        philipsHueViewmodel.addFlockErrorMsg.collectAsStateWithLifecycle()

                    val currentTab = mainViewmodel.activeTab.collectAsStateWithLifecycle()
                    val currentSystem = mainViewmodel.activeSystem.collectAsStateWithLifecycle()

                    // Before anything, do we need to exit?
                    if (philipsHueFinishNow) {
                        Log.d(TAG, "Finishing!!!")
                        finish()
                    }


                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                        //------------------------
                        //  What to display?  Depends on what's happening.
                        //
                        if (mainViewmodelInitializing ||
                            (phTestingState == TestStatus.TESTING) ||
                            philipsHueInitializing
                        ) {
                            Log.d(TAG, "showing TestSetupScreen")
                            Log.d(TAG, "   mainViewmodelInitializing = $mainViewmodelInitializing")
                            Log.d(TAG, "   phTestingState = $phTestingState")
                            Log.d(TAG, "   philipsHueIntializing = $philipsHueInitializing")

                            ShowInitializingScreen(Modifier.padding(innerPadding))
                        } else if (wifiWorking == false) {
                            TestBad(
                                wifiWorking = false,
                                errorMsg = stringResource(R.string.wifi_not_working)
                            )
                        } else if (phTestingState == TestStatus.TEST_BAD) {
                            TestBad(
                                wifiWorking = wifiWorking ?: false,
                                errorMsg = phTestingErrorMsg
                            )
                        } else if (addNewBridgeState != BridgeInitStates.NOT_INITIALIZING) {
                            Log.d(TAG, "in onCreate(), showWaitSpinner = $showWaitSpinner")
                            ManualBridgeSetup(
                                parentActivity = this,
                                philipsHueViewmodel = philipsHueViewmodel,
                                waitingForResults = showWaitSpinner,
                                initBridgeState = addNewBridgeState
                            )
                        } else {
                            // show the tabs
//                            ShowViewmodelTabs(
//                                modifier = Modifier.padding(innerPadding),
//                                mainViewmodel = mainViewmodel,
//                                viewmodelTabs = tabViewmodels,
//                                currentTab = currentTab.value,
//                                philipsHueBridges = philipsHueBridges,
//                                roomSceneData = roomSceneData.value,
//                                zoneSceneData = zoneSceneData.value,
//                                flockSceneData = flockSceneData.value,
//                                philipsHueFlocks = flocks.value,
//                                showAddFlock = addFlock.value,
//                                addFlockErrorMsg = addFlockError.value
//                            )

                            // Show the appropiate thing for the current tab state.
                            when (currentSystem.value) {
                                ActiveSystem.MainScreen -> ShowMainScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    viewmodelList = listOf(
                                        philipsHueViewmodel,
                                        poolViewmodel,
                                        climateViewModel,
                                        sprinklerViewmodel,
                                        securityViewmodel
                                    )
                                )
                                ActiveSystem.PhilipsHue -> TODO()
                                ActiveSystem.Pool -> TODO()
                                ActiveSystem.Sprinkler -> TODO()
                                ActiveSystem.Error -> TODO()
                            }

                        }

                        // finally show the version number
                        ShowVersion(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }

    } // onCreate()


    //----------------------------
    //  composables
    //----------------------------

    /**
     * Draws the main screen of this app.  This resembles the Launcher app
     * for most android devices.  The different systems of this app will
     * be icons laid out on the screen (user-defined).
     */
    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    fun ShowMainScreen(
        modifier: Modifier = Modifier,
        /** All the viewmodels to display */
        viewmodelList: List<MyViewModelInterface>
    ) {

        val ctx = LocalContext.current

        DrawLogo()

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            Log.d(TAG, "ShowMainScreen() dimensions are $maxWidth x $maxHeight")

            val drawPositions = getDrawPositions(
                viewmodelList,
                maxWidth.value,
                maxHeight.value
            )

            viewmodelList.forEachIndexed { i, viewmodel ->

                Log.d(TAG, "drawing ${viewmodel.getTitle(ctx)} at ${drawPositions[i].x.dp}, ${drawPositions[i].y.dp}")
//                Spacer(
//                    modifier = modifier
//                        .width(drawPositions[i].x.dp)
//                        .height(drawPositions[i].y.dp)
//                )
                Text(
                    modifier = modifier.padding(
                        start = drawPositions[i].x.dp,
                        top = drawPositions[i].y.dp
                    ),
                    text = viewmodel.getTitle(ctx)
                )
                Image(
                    modifier = modifier
                        .padding(
                            start = drawPositions[i].x.dp,
                            top = drawPositions[i].y.dp
                        )
                        .size(maxWidth / 15),
                    imageVector = viewmodel.getSelectedIcon(),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(LocalTheme.current.icon1),
                )
            }
        }
    }

    /**
     * Helper function to calculate draw positions for all the viewmodel icons.
     * Accesses the viewmodels and asks for their position.  If the viewmodel doesn't
     * know its position, a random one is assigned.
     *
     * @param   viewmodelList       List of the viewmodels that we're dealing with.
     *
     * @param   width, height       The dimensions of the view area
     */
    fun getDrawPositions(
        viewmodelList: List<MyViewModelInterface>,
        width: Float,
        height: Float
    ) : List<Float2> {

        // these are the actual ranges of the draw area
        val startX = ICON_PADDING
        val endX = width.toInt() - ICON_PADDING
        val startY = ICON_PADDING
        val endY = height.toInt() - ICON_PADDING

        val newPositions = mutableListOf<Float2>()
        viewmodelList.forEach { viewmodel ->
            var pos = viewmodel.getIconPos()
            if (pos == null) {
                pos = Float2(
                    x = (startX .. endX).random().toFloat(),
                    y = (startY .. endY).random().toFloat()
                )
            }
            else {
                // turn the percent into actual coords
                pos = Float2(
                    x = (endX - startX) * pos.x,
                    y = (endY - startY) * pos.y
                )
            }
            newPositions.add(pos)
        }

        return newPositions
    }

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

                    // The Philips Hue light system.
                    //
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

                    is SecurityViewmodel -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("Security Place-holder", fontSize = 26.sp) }
                    }

                    is ClimateViewmodel -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("Climate Control Place-holder", fontSize = 26.sp) }
                    }

                    // The Sprinkler system.  This one is pretty easy.
                    //
                    is SprinklerViewmodel -> {
                        ShowMainSprinkler(
                            sprinklerViewmodel = viewmodelTabs[index] as SprinklerViewmodel,
                            acceptJavaScript = true
                        )
                    }

                    is PoolViewmodel -> {
                        ShowMainPool(
                            poolViewmodel = viewmodelTabs[index] as PoolViewmodel,
                            acceptJavaScript = true
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

    /**
     * Display this while initializing the various systems.
     * I'm doing this until I crack down and to the splash screen the right way.
     */
    @Composable
    fun ShowInitializingScreen(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
                ) {
                CircularProgressIndicator(
                    color = LocalTheme.current.progressIndicatorColor,
                )
                Text(stringResource(R.string.initializing), fontSize = 20.sp)
            }
        }
    }

    /**
     * The main composable for displaying the Philips Hue portion of this app.
     * This differentiates between the different STATES of the philps hue
     * display.
     */
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
            ShowScenesRoomsZones(
                bridge = roomSceneData.bridge,
                room = roomSceneData.room,
                scenes = roomSceneData.scenes,
                onSetSceneForRoom = philipsHueViewmodel::setSceneSelectedForRoom,
                onDismiss = philipsHueViewmodel::stopShowingScenes,
                zone = null,
                onSetSceneForZone = null
            )
        }

        else if (zoneSceneData != null) {
            ShowScenesRoomsZones(
                bridge = zoneSceneData.bridge,
                room = null,
                onSetSceneForRoom = null,
                zone = zoneSceneData.zone,
                scenes = zoneSceneData.scenes,
                onSetSceneForZone = philipsHueViewmodel::setSceneSelectedForZone,
                onDismiss = philipsHueViewmodel::stopShowingScenes
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
     * Begins the process of manually initializing the IoT devices.
     */
    @Composable
    private fun TestBad(
        modifier : Modifier = Modifier,
        wifiWorking: Boolean,
        errorMsg: String = ""
    ) {
        Log.d(TAG, "TestBad()  errorMsg = $errorMsg")
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


    @Composable
    private fun ShowVersion(modifier: Modifier) {
        val ctx = LocalContext.current

        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Text("v${getVersionName(ctx)}", color = coolGray)
        }
    }

    /**
     * Displays a background logo that covers the entire screen
     */
    @Composable
    fun DrawLogo() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.dallas_cowboy_star2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.1f
            )
        }
    }
}

//----------------------------
//  constants
//----------------------------

private const val TAG = "MainActivity"

private const val ICON_PADDING = 140