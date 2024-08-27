package com.sleepfuriously.paulsapp.view

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.MainViewModel
import com.sleepfuriously.paulsapp.ui.theme.PaulsAppTheme

/**
 * The main screen that the user interacts with.  All the big
 * stuff goes here.
 */
class MainScreen {

    @Composable
    fun ShowMainScreen(
        modifier : Modifier = Modifier,
        viewModel: MainViewModel
    ) {

        if (viewModel.initializationComplete == false) {
            ShowInitialzingScreen()
        }


        Text("hi", modifier = modifier)
    }


    @Composable
    private fun ShowInitialzingScreen() {

        val config = LocalConfiguration.current
        val screenHeight = config.screenHeightDp.dp
        val screenWidth = config.screenWidthDp.dp

        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(80.dp)
                .background(color = Color(red = 0xb0, green = 0xc0, blue = 0xe0)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Initializing..."
            )
            CircularProgressIndicator()
        }
    }

    //----------------------------
    //  previews
    //----------------------------

    @Preview(showBackground = true)
    @Composable
    private fun ShowMainScreenPreview() {
        PaulsAppTheme {
            ShowMainScreen(viewModel = MainViewModel())
        }
    }

}


