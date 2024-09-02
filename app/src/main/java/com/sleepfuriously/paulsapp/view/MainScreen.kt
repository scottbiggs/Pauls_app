package com.sleepfuriously.paulsapp.view

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.MainViewModel
import com.sleepfuriously.paulsapp.PhilipsHueBridgeInit
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme

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


    AnimatedVisibility(
        visible = viewModel.bridgeInit == PhilipsHueBridgeInit.INITIALIZING,
        enter = fadeIn(animationSpec = tween(2000)),

        // exit by sliding left and fading
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth ->
                -fullWidth
            },
            animationSpec = tween(1000)
        ) + fadeOut(targetAlpha = 0f, animationSpec = tween(1000))
    ) {
        SplashScreen()
    }

    when (viewModel.bridgeInit) {
        PhilipsHueBridgeInit.INITIALIZING -> {
        }

        PhilipsHueBridgeInit.INITIALIZED -> {
            // todo
            Toast.makeText(LocalContext.current, stringResource(R.string.bridge_found), Toast.LENGTH_LONG).show()
        }

        PhilipsHueBridgeInit.INITIALIZATION_TIMEOUT -> {
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
private fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(80.dp)
            .background(color = Color(red = 0xb0, green = 0xc0, blue = 0xe0)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.initializing)
        )
        CircularProgressIndicator()
    }
}


//----------------------------
//  previews
//----------------------------

@Preview(
    name = "tablet",
    device = "spec:shape=Normal,width=1240,height=640,unit=dp,dpi=480",
    showSystemUi = true,
    showBackground = true
)
@Composable
private fun ShowMainScreenPreview() {
    PaulsAppTheme {
        ShowMainScreen(viewModel = MainViewModel())
    }
}

//----------------------------
//  constants
//----------------------------

private const val TAG = "MainScreen"