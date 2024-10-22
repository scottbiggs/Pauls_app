package com.sleepfuriously.paulsapp

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    private val splashViewmodel by viewModels<SplashViewmodel>()


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel  = MainViewModel()

        // surest way to hide the action bar
        actionBar?.hide()

        // start initializations and splash screen
        splashViewmodel.checkIoT(this)
//        showSplashScreen()


        setContent {
            PaulsAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // create composables of flows in the splashviewmodel
                    val iotTestStatus by splashViewmodel.iotTestsStatus.collectAsStateWithLifecycle()
                    val wifiWorking by splashViewmodel.wifiWorking.collectAsStateWithLifecycle()
                    val bridgeIpSet by splashViewmodel.philipsHueIpSet.collectAsStateWithLifecycle()
                    val bridgeIpWorking by splashViewmodel.philipsHueBridgeIpWorking.collectAsStateWithLifecycle()
                    val bridgeTokenSet by splashViewmodel.philipsHueTokenSet.collectAsStateWithLifecycle()
                    val bridgeTokenWorks by splashViewmodel.philipsHueBridgeTokenWorks.collectAsStateWithLifecycle()
                    val philipsHueTestStatus by splashViewmodel.philipsHueTestStatus.collectAsStateWithLifecycle()

                    val initializingBridgeState by splashViewmodel.initializingBridgeState.collectAsStateWithLifecycle()


                    when (iotTestStatus) {
                        TestStatus.TESTING,
                        TestStatus.NOT_TESTED -> {

                            TestSetupScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel = viewModel,
                                wifiWorking = wifiWorking ?: false,
                                ipSet = bridgeIpSet,
                                ipWorking = bridgeIpWorking,
                                tokenSet = bridgeTokenSet,
                                tokenWorking = bridgeTokenWorks,
                                philipsHueTest = philipsHueTestStatus
                            )
                        }

                        TestStatus.TEST_GOOD -> {
                            ShowMainScreen(
                                modifier = Modifier.padding(innerPadding),
                                viewModel
                            )
                        }

                        TestStatus.TEST_BAD -> {
                            if (initializingBridgeState == BridgeInitStates.NOT_INITIALIZING) {
                                TestBad(
                                    splashViewmodel = splashViewmodel,
                                    wifiWorking = wifiWorking ?: false,
                                    ipSet = bridgeIpSet ?: false,
                                    ipWorking = bridgeIpWorking ?: false,
                                    tokenSet = bridgeTokenSet ?: false,
                                    tokenWorking = bridgeTokenWorks ?: false,
                                    philipsHueTest = philipsHueTestStatus
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


                    val ctx = LocalContext.current

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
        ipSet: Boolean?,
        ipWorking: Boolean?,
        tokenSet: Boolean?,
        tokenWorking: Boolean?,
        philipsHueTest: TestStatus
    ) {
        Log.d(TAG, "TestSetupScreen()")

        Column(modifier = modifier.fillMaxSize()) {
            Text("wifi = $wifiWorking")
            Text("ip of bridge = $ipSet")
            Text("ip of bridge working = $ipWorking")
            Text("token set = $tokenSet")
            Text("token working = $tokenWorking")
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
        ipSet: Boolean,
        ipWorking: Boolean,
        tokenSet: Boolean,
        tokenWorking: Boolean,
        philipsHueTest: TestStatus
    ) {
        // start with the wifi
        if (wifiWorking == false) {
            WifiNotWorking(modifier)
        }

        // Any problems with the philips hue stuff?
        else if (philipsHueTest != TestStatus.TEST_GOOD) {
            if (ipSet == false) {
                PhilipsHueIpNotSet(modifier, splashViewmodel)
            }
            else if (ipWorking == false) {
                PhilipsHueIpNotWorking(modifier, splashViewmodel)
            }
            else if (tokenSet == false) {
                PhilipsHueTokenNotSet(modifier, splashViewmodel)
            }
            else if (tokenWorking == false) {
                PhilipsHueTokenNotWorking(modifier, splashViewmodel)
            }
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

    @Composable
    private fun PhilipsHueIpNotSet(modifier: Modifier, splashViewModel: SplashViewmodel) {
        SimpleBoxMessage(
            modifier,
            stringResource(id = R.string.philips_hue_bridge_ip_not_set),
            {splashViewModel.beginInitializePhilipsHue(this)},
            stringResource(id = R.string.initialize_philips_hue)
        )
    }

    @Composable
    private fun PhilipsHueIpNotWorking(modifier: Modifier, splashViewModel: SplashViewmodel) {
        SimpleBoxMessage(
            modifier,
            stringResource(id = R.string.philips_hue_ip_not_working),
            {splashViewModel.beginInitializePhilipsHue(this)},
            stringResource(id = R.string.initialize_philips_hue)
        )
    }

    @Composable
    private fun PhilipsHueTokenNotSet(modifier: Modifier, splashViewModel: SplashViewmodel) {
        SimpleBoxMessage(
            modifier,
            stringResource(id = R.string.philips_hue_token_not_set),
            {splashViewModel.beginInitializePhilipsHue(this)},
            stringResource(id = R.string.initialize_philips_hue)
        )
    }

    @Composable
    private fun PhilipsHueTokenNotWorking(modifier: Modifier, splashViewModel: SplashViewmodel) {
        SimpleBoxMessage(
            modifier,
            stringResource(id = R.string.philips_hue_token_not_working),
            {splashViewModel.beginInitializePhilipsHue(this)},
            stringResource(id = R.string.initialize_philips_hue)
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

    //----------------------------
    //  previews
    //----------------------------

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
            //          (in our case, until isReady == false)
            setKeepOnScreenCondition {
                (splashViewmodel.iotTestsStatus.value == TestStatus.NOT_TESTED) ||
                        (splashViewmodel.iotTestsStatus.value == TestStatus.TESTING)
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
        splashViewModel: SplashViewmodel,
        initBridgeState: BridgeInitStates,
        modifier: Modifier = Modifier,
    ) {
        val config = LocalConfiguration.current
        val landscape = config.orientation == ORIENTATION_LANDSCAPE
        val screenHeight = config.screenHeightDp
        val screenWidth = config.screenWidthDp
        val ctx = LocalContext.current

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
            BridgeInitStates.STAGE_1 -> {
                var ipText by remember { mutableStateOf("") }

                if (landscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(id = R.string.stage_1)
                        )
                        Text(
                            stringResource(id = R.string.find_the_bridge_ip)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Image(
                                modifier = Modifier
//                                    .height((screenHeight - 120).dp)
                                    .weight(1f),
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_1),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_1_desc)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                modifier = Modifier
//                                    .height((screenHeight - 120).dp)
                                    .weight(1f),
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_2),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_2_desc)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Image(
                                modifier = Modifier
//                                    .height((screenHeight - 120).dp)
                                    .weight(1f),
                                contentScale = ContentScale.Fit,
                                painter = painterResource(id = R.drawable.bridge_ip_step_3),
                                contentDescription = stringResource(id = R.string.bridge_ip_step_3_desc)
                            )
                        }

                        OutlinedTextField(
                            value = ipText,
                            label = { Text(stringResource(id = R.string.enter_ip)) },
                            singleLine = true,
                            onValueChange = { ipText = it }
                        )
                        Button(onClick = { splashViewModel.setPhilipsHueIp(ctx, ipText) } ) {
                            Text(stringResource(id = R.string.ok))
                        }

                    }
                }
                else {
                    LazyColumn(

                    ) {

                    }
                }
            }
            BridgeInitStates.STAGE_2 -> {
            }
            BridgeInitStates.STAGE_3 -> {
            }
        }
    }
}




//----------------------------
//  constants
//----------------------------

private const val TAG = "MainActivity"