package com.sleepfuriously.paulsapp

import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
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
    private val splashViewModel by viewModels<SplashViewModel>()


    //----------------------------
    //  functions
    //----------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel  = MainViewModel()
        viewModel.initialize(this)

        // surest way to hide the action bar
        actionBar?.hide()

        //--------------
        // splash screen stuff
        //--------------

        showSplashScreen()


        setContent {
            PaulsAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (viewModel.displayStates == MainActivityDisplayStates.APP_STARTUP) {

                        AnimateSplashScreen(viewModel, Modifier.padding(innerPadding))
                    }
                    else {
                        ShowMainScreen(
                            modifier = Modifier.padding(innerPadding),
                            viewModel
                        )
                    }
                }
            }
        }
    }


    //----------------------------
    //  composables
    //----------------------------

    /**
     * Displays the splash screen with a bit of animation.
     * When the timed out, will signal the view model and stop
     * displaying the splash (with animation of course!).
     *
     * Note: animations are done here.  The actual splash screen
     * are done in [SplashScreenContents].
     */
    @Composable
    private fun AnimateSplashScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {

        val animVisibleState = remember {
            MutableTransitionState(viewModel.displayStates
                    == MainActivityDisplayStates.APP_STARTUP)
        }

        Column {
            AnimatedVisibility(
//            visible = viewModel.displayStates == MainActivityDisplayStates.APP_STARTUP,
                visibleState = animVisibleState,
                enter = fadeIn(animationSpec = tween(4000)),

                // exit by sliding left and fading
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth ->
                        -fullWidth
                    },
                    animationSpec = tween(4000)
                ) // + fadeOut(targetAlpha = 0f, animationSpec = tween(1000))
            ) {
                SplashScreenContents()
            }
        }


//        SplashScreenContents(modifier)
    }

    /**
     * The main screen that the user interacts with.  All the big
     * stuff goes here.
     */
    @Composable
    fun ShowMainScreen(
        modifier : Modifier = Modifier,
        viewModel: MainViewModel
    ) {
        // useful to know!
        val config = LocalConfiguration.current
        val screenHeight = config.screenHeightDp.dp
        val screenWidth = config.screenWidthDp.dp
        val landscape = config.orientation == ORIENTATION_LANDSCAPE

        var splashScreenDone by remember { mutableStateOf(true) }

        if (viewModel.bridgeInit == PhilipsHueBridgeInit.BRIDGE_INITIALIZING) {
            splashScreenDone = false
        }

        when (viewModel.bridgeInit) {

            PhilipsHueBridgeInit.BRIDGE_UNINITIALIZED -> {
                DisplayInitPHBridgeButton(viewModel, modifier)
            }

            PhilipsHueBridgeInit.BRIDGE_INITIALIZING -> {
            }

            PhilipsHueBridgeInit.BRIDGE_INITIALIZED -> {
                // todo
                Toast.makeText(LocalContext.current, stringResource(R.string.bridge_found), Toast.LENGTH_LONG).show()

                if (splashScreenDone) {
                    Column(modifier = Modifier.fillMaxSize(), Arrangement.Center) {
                        Text(
                            "Hi scott!  it's looking good so far.",
                            modifier = Modifier
                                .align(alignment = Alignment.CenterHorizontally),
                        )
                    }
                }
            }

            PhilipsHueBridgeInit.BRIDGE_INITIALIZATION_TIMEOUT -> {
                // todo
                Text(stringResource(R.string.cannot_find_bridge))
            }

            PhilipsHueBridgeInit.ERROR -> {
                // todo
                Text(stringResource(R.string.error_init_bridge))
            }
        }

    }

    /**
     * Displays the content of the splash screen.  Animations should be
     * done at the caller level.
     */
    @Composable
    private fun SplashScreenContents(modifier: Modifier = Modifier) {

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
     * Simply shows the button that starts the initialization process.
     * This will change the state such that a screen will show the user
     * what to do.
     */
    @Composable
    private fun DisplayInitPHBridgeButton(
        viewModel: MainViewModel,
        modifier: Modifier = Modifier
    ) {

        Box(modifier = modifier.fillMaxSize()) {

            Button(onClick = {
                viewModel.startManualBridgeInit()
            }) {
                Text(stringResource(id = R.string.start_ph_bridge_init_button))
            }
        }
    }

    /**
     * Initializing the Philips Hue bridge, part 1.
     * The user simply has to fill in the IP of the bridge.
     * Information on how to do it is also displayed.
     */
    @Composable
    private fun DisplayManualInitPart1() {
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
        DisplayManualInitPart1()
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
     */
    private fun showSplashScreen() {
        // Needs to be called before setContent() or setContentView().
        // apply{} is optional--use to do any additional work.
        installSplashScreen().apply {
            // example: this will check the value every frame and keep showing
            //          the splash screen as long as the total value is true
            //          (in our case, until isReady == false)
            setKeepOnScreenCondition {
                !splashViewModel.isReady.value
            }

            // my own experiments on exiting the screen
            setOnExitAnimationListener { screen ->

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
