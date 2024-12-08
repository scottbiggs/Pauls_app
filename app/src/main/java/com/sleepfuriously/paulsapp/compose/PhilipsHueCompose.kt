package com.sleepfuriously.paulsapp.compose

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.MainActivity
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.model.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.lightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryLightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.yellowVeryLight
import kotlin.math.roundToInt

/**
 * All the display stuff for the UI for the philips hue portion
 * of the app.
 *
 * This needs to communicate with the PhilipsHueViewmodel to work
 * correctly.
 */

//---------------------------------
//  compose functions
//---------------------------------

/**
 * Displays the philips hue stuff.
 *
 * @param   bridges     The PH bridges in the app.  They may be active
 *                      or inactive.
 */
@Composable
fun ShowMainScreenPhilipsHue(
    modifier: Modifier = Modifier,
    philipsHueViewmodel: PhilipsHueViewmodel,
    bridges: Set<PhilipsHueBridgeInfo>,
) {

    val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)
    val activeBridges = philipsHueViewmodel.bridgeModel.getAllActiveBridges()

    // the content
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        // the top--a title and a fab (to add bridges)
        Text(
            stringResource(id = R.string.ph_main_title),
            modifier = Modifier
                .padding(top = 4.dp, start = 32.dp, bottom = 8.dp),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            columns = GridCells.Adaptive(MIN_PH_ROOM_WIDTH.dp),
            verticalArrangement = Arrangement.Top,
            horizontalArrangement = Arrangement.Start
        ) {
            // Grouping the grids into bridges.  The first row will be the name
            // of the bridge.
            activeBridges.forEach { bridge ->

                // only draw bridges that are active
                if (bridge.active) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        // a gradient to separate the bridges
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                                .height(28.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            coolGray,
                                            Color(red = 0, green = 0, blue = 0, alpha = 0)
                                        )
                                    )
                                )
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
                            text = stringResource(id = R.string.ph_bridge_name, bridge.id),
                            style = MaterialTheme.typography.headlineSmall,
                            color = yellowVeryLight
                        )
                    }

                    if (bridge.rooms.size == 0) {
                        // no rooms to display
                        item(span = { GridItemSpan(this.maxLineSpan) }) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp),
                                text = noRoomsFound,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // yes, there are rooms to display
                        bridge.rooms.forEach { room ->
                            item {
                                DisplayPhilipsHueRoom(
                                    roomName = room.id,
                                    illumination = room.getAverageIllumination()
                                        .toFloat() / MAX_BRIGHTNESS.toFloat(),
                                    lightSwitchOn = room.on,
                                ) { newIllumination, switchOn ->
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

    // display the fab on top of stuff
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        ShowMainPhilipsHueAddBridgeFab(
            modifier = Modifier
                .padding(bottom = 18.dp)
                .align(Alignment.BottomEnd),    // only works if parent is Box
            viewmodel = philipsHueViewmodel,
            numActiveBridges = activeBridges.size
        )
    }

    // todo: show messages (if any)

}


@Composable
private fun ShowMainPhilipsHueAddBridgeFab(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    numActiveBridges: Int
) {
    // Add button to add a new bridge (if there are no active bridges, then
    // show the extended FAB)
    if (numActiveBridges == 0) {
        ExtendedFloatingActionButton(
            modifier = modifier
                .padding(top = 26.dp, end = 38.dp),
            onClick = { viewmodel.beginAddPhilipsHueBridge() },
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.add_button_content_desc)
                )
            },
            text = { Text(stringResource(id = R.string.ph_add_button)) }
        )
    } else {
        FloatingActionButton(
            modifier = modifier
                .padding(top = 16.dp, end = 38.dp),
            onClick = { viewmodel.beginAddPhilipsHueBridge() },
            elevation = FloatingActionButtonDefaults.elevation(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.add_button_content_desc)
            )
        }
    }
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
 *                                  illumination value and a boolean for if the
 *                                  switch is on/off.
 */
@Composable
private fun DisplayPhilipsHueRoom(
    modifier: Modifier = Modifier,
    roomName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    roomChangedFunction: (newIllumination: Float, switchOn: Boolean) -> Unit,
) {

    // variables for the widgets
    var sliderPosition by remember { mutableFloatStateOf(illumination) }
    var roomLightsSwitchOn by remember { mutableStateOf(lightSwitchOn) }

    // variables for displaying the lightbulb
    var lightImage by remember { mutableStateOf(getProperLightImage(sliderPosition)) }
    var lightColor by remember { mutableStateOf(getLightColor(sliderPosition)) }

    Log.d(TAG, "begin DisplayPhilipsHueRoom:  lightImage = $lightImage, lightColor = $lightColor")

    Column(modifier = modifier
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
            modifier = modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )

        Row (
            modifier = modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Switch(
                modifier = modifier
                    .padding(start = 8.dp, bottom = 8.dp)
                    .rotate(-90f),
                checked = roomLightsSwitchOn,
                onCheckedChange = { newSliderState ->
                    roomLightsSwitchOn = newSliderState
                    if (roomLightsSwitchOn) {
                        lightImage = getProperLightImage(sliderPosition)
                        lightColor = getLightColor(sliderPosition)
                    }
                    else {
                        lightImage = getProperLightImage(0f)
                        lightColor = getLightColor(0f)
                    }
                    roomChangedFunction.invoke(sliderPosition, roomLightsSwitchOn)
                }
            )

            // This pushes the switch to the far left and the lightbulb to
            // the far right.
            Spacer(modifier = modifier.weight(1f))

            DrawLightBulb(lightImage, lightColor)
        }

        Slider(
            value = sliderPosition,
            enabled = roomLightsSwitchOn,
            onValueChange = {
                sliderPosition = it
                lightImage = getProperLightImage(sliderPosition)
                lightColor = getLightColor(sliderPosition)
                roomChangedFunction.invoke(sliderPosition, roomLightsSwitchOn)
            },
            modifier = modifier
                .padding(vertical = 4.dp, horizontal = 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(veryDarkCoolGray)
        )
        Text(
            text = stringResource(id = R.string.brightness, (sliderPosition * 100).roundToInt()),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun DrawLightBulb(imageId : Int, colorTint: Color) {
    Image(
        modifier = Modifier
            .width(140.dp),
        contentScale = ContentScale.Fit,
        painter = painterResource(imageId),
        colorFilter = ColorFilter.tint(colorTint),
        contentDescription = stringResource(id = R.string.lightbulb_content_desc)
    )
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

private fun getLightColor(illumination: Float) : Color {
    val color =
        if (illumination < 0.25f) {
            coolGray
        }
        else if (illumination < 0.5f) {
            lightCoolGray
        }
        else if (illumination < 0.75f) {
            veryLightCoolGray
        }
        else {
            Color.White
        }
    return color
}


//---------------------------------
//  bridge init
//---------------------------------

/**
 * Walks the user through the initialization process of the philips
 * hue bridge
 */
@Composable
fun ManualBridgeSetup(
    modifier: Modifier = Modifier,
    parentActivity: MainActivity,
    philipsHueViewmodel: PhilipsHueViewmodel,
    waitingForResults: Boolean,
    initBridgeState: BridgeInitStates
) {
    val config = LocalConfiguration.current
    val landscape = config.orientation == ORIENTATION_LANDSCAPE

    Log.d(TAG, "ManualBridgeSetup() waitingForResults = $waitingForResults")
    if (waitingForResults) {
        ManualInitWaiting(philipsHueViewmodel, modifier)
    }
    else {
        when (initBridgeState) {
            BridgeInitStates.NOT_INITIALIZING -> {
                // this should not happen.
                SimpleFullScreenBoxMessage(
                    backgroundModifier = modifier,
                    msgText = "error: should not be in ManualBridgeSetup() with this bridge init state.",
                    onClick = { parentActivity.finish() },
                    buttonText = stringResource(id = R.string.exit)
                )
            }

            // These states involve stage 1
            BridgeInitStates.STAGE_1_GET_IP,
            BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT,
            BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> {
                if (landscape) {
                    ManualBridgeSetupStep1_landscape(modifier, philipsHueViewmodel, initBridgeState)
                } else {
                    ManualBridgeSetupStep1_Portrait(
                        modifier,
                        philipsHueViewmodel,
                        initBridgeState
                    )
                }
            }

            // Stage 2 states
            BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON,
            BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,
            BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE,
            BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED,
            BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE -> {
                ManualBridgeSetupStep2(modifier, philipsHueViewmodel, initBridgeState)
            }

            BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE,
            BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
                ManualBridgeSetupStep3(modifier, philipsHueViewmodel, initBridgeState)
            }

        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManualBridgeSetupStep1_landscape(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {

    val ctx = LocalContext.current

    BackHandler {
        viewmodel.bridgeInitGoBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(id = R.string.find_the_bridge_ip),
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            // 1st column
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Image(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    painter = painterResource(id = R.drawable.bridge_ip_step_1),
                    contentDescription = stringResource(id = R.string.bridge_ip_step_1_desc)
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    text = stringResource(id = R.string.bridge_ip_step_1)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))

            // 2nd column
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Image(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    painter = painterResource(id = R.drawable.bridge_ip_step_2),
                    contentDescription = stringResource(id = R.string.bridge_ip_step_2_desc)
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    text = stringResource(id = R.string.bridge_ip_step_2)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))

            // 3rd column
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Image(
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .weight(1f),
                    painter = painterResource(id = R.drawable.bridge_ip_step_3),
                    contentDescription = stringResource(id = R.string.bridge_ip_step_3_desc)
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = R.string.bridge_ip_step_3),
                    style = MaterialTheme.typography.bodyMedium
                )

                TextFieldAndButton(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally),
                    label = stringResource(R.string.enter_ip),
                    onClick = viewmodel::addPhilipsHueBridgeIp,
                    buttonLabel = stringResource(R.string.enter_ip),
                    imeActivate = true,
                    defaultText = viewmodel.getNewBridgeIp(),
                    keyboardType = KeyboardType.Decimal
                )
            } // 3rd column

        } // Row of the 3 images and text (textfield and button in 3rd)

    } // main column

    // display any error messages
    if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_bad_ip_format),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }
    if (state == BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP) {
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_no_bridge_at_ip, viewmodel.newBridge?.ip ?: ""),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }

} // ManualBridgeSetupStep1_landscape


@Composable
private fun ManualBridgeSetupStep1_Portrait(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates,
) {
    val ctx = LocalContext.current

    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp
    val screenWidth = config.screenWidthDp

    BackHandler {
        viewmodel.bridgeInitGoBack()
    }

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
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(id = R.string.find_the_bridge_ip),
            style = MaterialTheme.typography.headlineMedium,
        )
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            state = rememberLazyListState(initialFirstVisibleItemIndex = 0) // alwasy start at the beginning
        ) {
            item {
                Column(
                    modifier
                        .fillMaxHeight()
                        .width(columnWidth.dp)
                        .weight(1f)
                ) {
                    Image(
                        modifier = Modifier.weight(1f),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_1),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_1_desc)
                    )
                    Text(
                        modifier = modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_1)
                    )
                }
            }
            item {
                Spacer(modifier = modifier.width(24.dp))
            }

            item {
                Column(
                    modifier
                        .fillMaxHeight()
                        .width(columnWidth.dp)
                        .weight(1f)
                ) {
                    Image(
                        modifier = Modifier.weight(1f),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_2),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_2_desc)
                    )
                    Text(
                        modifier = modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_2)
                    )
                }
            }
            item {
                Spacer(modifier = modifier.width(24.dp))
            }

            item {
                Column(
                    modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .width(columnWidth.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Image(
                        modifier = Modifier.weight(1f),
                        contentScale = ContentScale.Fit,
                        painter = painterResource(id = R.drawable.bridge_ip_step_3),
                        contentDescription = stringResource(id = R.string.bridge_ip_step_3_desc)
                    )
                    Text(
                        modifier = modifier
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.bridge_ip_step_3)
                    )

                    TextFieldAndButton(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        label = stringResource(R.string.enter_ip),
                        onClick = viewmodel::addPhilipsHueBridgeIp,
                        buttonLabel = stringResource(R.string.enter_ip),
                        imeActivate = true,
                        defaultText = viewmodel.getNewBridgeIp(),
                        keyboardType = KeyboardType.Decimal
                    )
                }

            }
        } // LazyRow

    } // master Column

    // display any error messages
    if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_bad_ip_format),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }
    if (state == BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP) {
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_no_bridge_at_ip, viewmodel.newBridge?.ip ?: ""),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }

} // ManualBridgeSetupStep1_Portrait


/**
 * This part of the manual bridge setup is where the user needs to hit the
 * big button on the bridge.  Once they do, the user needs to hit the
 * ok button on this screen.
 */
@Composable
private fun ManualBridgeSetupStep2(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {

    BackHandler {
        viewmodel.bridgeInitGoBack()
    }

    val ctx = LocalContext.current

    val bridgeErrorMsg = when (state) {
        BridgeInitStates.NOT_INITIALIZING -> ""
        BridgeInitStates.STAGE_1_GET_IP -> ""
        BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT -> ""
        BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> ""
        BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON -> ""
        BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE -> stringResource(
            R.string.registering_bridge_button_not_pressed,
            viewmodel.newBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE -> stringResource(
            R.string.registering_bridge_unsuccessful,
            viewmodel.newBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE -> stringResource(R.string.registering_bridge_cannot_parse_response)
        BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED -> stringResource(
            R.string.registering_bridge_button_not_pressed,
            viewmodel.newBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE -> stringResource(R.string.bridge_ip_step_3_problem_adding_bridge)
        BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> ""
    }

    if (bridgeErrorMsg.isBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()      // takes the insets into account (nav bars, etc)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.connect_to_ph_bridge),
                style = MaterialTheme.typography.headlineSmall,
            )

            Image(
                modifier = Modifier.weight(1f),     // image will fill remaining height
                contentScale = ContentScale.Fit,
                painter = painterResource(id = R.drawable.press_bridge_button),
                contentDescription = stringResource(id = R.string.press_bridge_button_desc)
            )

            // success message
            val ipStr = viewmodel.newBridge?.ip
            Text(
                stringResource(R.string.connect_bridge_ip_success, ipStr ?: "error"),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
            )

            // continue message
            Text(
                stringResource(R.string.press_bridge_button),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
            )


            Button(
                onClick = {
                    viewmodel.bridgeButtonPushed()
                },
                modifier = Modifier
                    .padding(bottom = 24.dp, top = 8.dp)
                    .width(width = 100.dp)
                    .height(50.dp)
            ) {
                Text(
                    stringResource(R.string.next),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } // Column
    } // if (bridgeErrorMsg.isBlank()

    else {
        // else display the error message
        SimpleFullScreenBoxMessage(
            textModifier = Modifier.padding(horizontal = 84.dp),
            msgText = bridgeErrorMsg,
            onClick = {
                viewmodel.bridgeAddErrorMsgIsDisplayed()
            },
            buttonText = stringResource(R.string.ok)
        )
    }
} // ManualBridgeSetupStep2


/**
 * UI part 3 of the manual bridge setup.  This is the result after the user
 * has hit the button on the bridge.
 */
@Composable
private fun ManualBridgeSetupStep3(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {

    BackHandler {
        viewmodel.bridgeInitGoBack()
    }

    if (state == BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE) {
        SimpleFullScreenBoxMessage(
            backgroundModifier = modifier,
            onClick = {
                viewmodel.bridgeAddAllGoodAndDone()
            },
            msgText = stringResource(id = R.string.new_bridge_success),
            buttonText = stringResource(R.string.ok)
        )
    }

    else if (state == BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE) {
        SimpleFullScreenBoxMessage(
            backgroundModifier = modifier,
            onClick = {
                viewmodel.bridgeAddAllGoodAndDone()
            },
            msgText = stringResource(id = R.string.bridge_ip_step_3_problem_adding_bridge),
            buttonText = stringResource(R.string.ok)
        )
    }

    else {
        // this state should never call this function
        SimpleFullScreenBoxMessage(
            backgroundModifier = modifier,
            onClick = {
                viewmodel.bridgeInitGoBack()
            },
            msgText = stringResource(R.string.bridge_ip_step_3_bad_state_error),
            buttonText = stringResource(R.string.back)
        )
    }

//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .safeContentPadding()      // takes the insets into account (nav bars, etc)
//            .padding(8.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = stringResource(id = R.string.new_bridge_success),
//            style = MaterialTheme.typography.headlineMedium,
//        )
//
//        Button(
//            onClick = {
//                viewmodel.bridgeAddAllGoodAndDone()
//            }
//        ) { Text(stringResource(R.string.ok)) }
//
//    }

}

@Composable
private fun ManualInitWaiting(
    viewmodel: PhilipsHueViewmodel,
    modifier: Modifier = Modifier
) {

    BackHandler {
        viewmodel.bridgeCancelTest()
    }

    Box (
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.wait_while_checking_bridge_ip))
        }
    }
}

//---------------------------------
//  previews
//---------------------------------

/*
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 1600, heightDp = 800
)
@Composable
private fun ManualBridgeSetupStep2_landscapePreview() {

    val ctx = LocalContext.current

    Box(
        modifier = Modifier
            .width(MIN_PH_ROOM_WIDTH.dp)
            .height((MIN_PH_ROOM_WIDTH * 1.5).dp)
    ) {
        ManualBridgeSetupStep2(
            PhilipsHueViewmodel(),
            BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON
        )
    }
}
*/


/*
@Preview
@Composable
private fun ManualInitWaitingPreview() {
    ManualInitWaiting(PhilipsHueViewmodel())
}
*/

/*
    @Preview (uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    private fun DisplayPhilipsHueRoomPreview() {
        Box(
            modifier = Modifier
                .width(MIN_PH_ROOM_WIDTH.dp)
                .height((MIN_PH_ROOM_WIDTH * 1.5).dp)
        ) {
            DisplayPhilipsHueRoom(
                roomName = "bedroom",
                illumination = 0.5f,
                lightSwitchOn = true
            ) { _, _ ->
                // not used in preview!
            }
        }
    }
*/


//---------------------------------
//  constants
//---------------------------------

private const val TAG = "PhilipsHueCompose"

/** The grid of rooms in the PH section, each room must be at least this wide */
private const val MIN_PH_ROOM_WIDTH = 200