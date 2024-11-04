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
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme


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
//    private val splashViewmodel by viewModels<SplashViewmodel>()
    private lateinit var splashViewmodel: SplashViewmodel


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel  = MainViewModel()
        splashViewmodel = SplashViewmodel(this)

        // surest way to hide the action bar
        actionBar?.hide()

        // start initializations and splash screen
        splashViewmodel.checkIoT(this)
//        showSplashScreen()

        setContent {
            PaulsAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val ctx = LocalContext.current

                    // create composables of flows in the splashviewmodel
                    val wifiWorking by splashViewmodel.wifiWorking.collectAsStateWithLifecycle()
                    val iotTesting by splashViewmodel.iotTesting.collectAsStateWithLifecycle()
//                    val iotTestStatus by splashViewmodel.iotTestsStatus.collectAsStateWithLifecycle()
                    val philipsHueTestStatus by splashViewmodel.philipsHueTestStatus.collectAsStateWithLifecycle()

                    /** used while initializing a specific bridge */
                    val initializingBridgeState by splashViewmodel.addNewBridgeState.collectAsStateWithLifecycle()

                    /** yup, this is pretty important--all the bridges */
                    val philipsHueBridges by splashViewmodel.philipsHueBridges.collectAsStateWithLifecycle()

                    //--------------
                    //  Philips Hue displays
                    //

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
                        ShowMainScreen(viewModel = viewModel)
                    }


/*  this is no longer needed

                    when (iotTestStatus) {
                        TestStatus.TESTING,
                        TestStatus.NOT_TESTED -> {
                            Log.d(TAG, "iotTestStatus is ${iotTestStatus.name}")
                            TestSetupScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel,
                                wifiWorking = wifiWorking ?: false,
                                philipsHueTest = philipsHueTestStatus,
                                philipsHueBridges = philipsHueBridges,
                            )
                        }

                        TestStatus.TEST_GOOD -> {
                            Log.d(TAG, "iotTestStatus is ${iotTestStatus.name}")
                            ShowMainScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel
                            )
                        }

                        TestStatus.TEST_BAD -> {
                            Log.d(TAG, "iotTestStatus is ${iotTestStatus.name}")
                            if (initializingBridgeState == BridgeInitStates.NOT_INITIALIZING) {
                                TestBad(
                                    splashViewmodel = splashViewmodel,
                                    wifiWorking = wifiWorking ?: false
                                )
                            }
                            else {
                                ManualBridgeSetup(
                                    splashViewmodel,
                                    initializingBridgeState
                                )
                            }
                        }

                    }
*/

                    // for testing setBridgeToken
//                    val token = splashViewmodel.bridgeToken.collectAsState()
//
//                    Column(modifier = Modifier.padding(innerPadding)) {
//                        Text("token = ${token.value}")
//                        Button(onClick = {
//                            splashViewmodel.getBridgeTokenTest(ctx)
//                        }) {
//                            Text("get token")
//                        }
//
//                        Spacer(modifier = Modifier.height(30.dp))
//
//                        Button(onClick = {
//                            splashViewmodel.setBridgeTokenTest(ctx, "foo forever!")
//                        }) {
//                            Text("set token")
//                        }
//                    }

                    // for testing isWifiWorking()
//                    val wifiState = splashViewmodel.wifiWorking.collectAsState()
//                    val txt = when (wifiState.value) {
//                        null -> "not tested yet"
//                        true -> "yes"
//                        false -> "no"
//                    }
//
//                    Column(modifier = Modifier.padding(innerPadding)) {
//
//                        Text(txt)
//
//                        Button(onClick = {
//                            splashViewmodel.isWifiWorking(ctx)
//                        }) {
//                            Text("is wifi working?")
//                        }
//
//                    }
                    // end testing
                }
            }
        }

    } // onCreate()


    //----------------------------
    //  composables
    //----------------------------

    /**
     * The main screen that the user interacts with.  All the big
     * stuff goes here.
     */
    @Composable
    fun ShowMainScreen(
        modifier : Modifier = Modifier,
        viewModel: MainViewModel
    ) {
        Log.d(TAG, "ShowMainScreen()")

        // useful to know!
        val config = LocalConfiguration.current
        val screenHeight = config.screenHeightDp.dp
        val screenWidth = config.screenWidthDp.dp
        val landscape = config.orientation == ORIENTATION_LANDSCAPE

        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Text("todo", fontSize = 30.sp)
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