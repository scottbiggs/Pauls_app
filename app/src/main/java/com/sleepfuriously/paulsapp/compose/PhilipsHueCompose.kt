package com.sleepfuriously.paulsapp.compose

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.MainActivity
import com.sleepfuriously.paulsapp.viewmodels.MainViewmodel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.model.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueLightInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
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
    philipsHueViewModel: PhilipsHueViewmodel,
    viewModel: MainViewmodel,
    bridges: Set<PhilipsHueBridgeInfo>,
) {

    // setup tests
    val testRooms = mutableSetOf(
        PhilipsHueRoomInfo(
            "baby's room", true, mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )
        ),

        PhilipsHueRoomInfo(
            "2 times 3 time 4", true, mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )
        ),

        PhilipsHueRoomInfo(
            "living room", true, mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )
        ),
        PhilipsHueRoomInfo(
            "bedroom", true, mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )
        ),
        PhilipsHueRoomInfo(
            "kitchen", false, mutableSetOf(
                PhilipsHueLightInfo("1", name = "nightlight"),
                PhilipsHueLightInfo("2", name = "desk light"),
                PhilipsHueLightInfo("3", name = "backyard light"),
            )
        ),
    )

    val testBridges = setOf(
        PhilipsHueBridgeInfo("1", rooms = testRooms),
        PhilipsHueBridgeInfo("2", active = true),
        PhilipsHueBridgeInfo("3", active = true, rooms = testRooms),
        PhilipsHueBridgeInfo("4"),
        PhilipsHueBridgeInfo("245", active = true, rooms = testRooms),
    )

    val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)
    val activeBridges = philipsHueViewModel.bridgeUtils.getAllActiveBridges(bridges)

    // the content
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        // the top--a title and a fab (to add bridges)
        Row(
            verticalAlignment = Alignment.Top
        ) {

            Text(
                stringResource(id = R.string.ph_main_title),
                modifier = Modifier
                    .padding(top = 12.dp, start = 32.dp, bottom = 8.dp),
                fontSize = 32.sp,
                color = Color.White
            )

            // pushes the fab to the far right
            Spacer(modifier = Modifier.weight(1f))

            ShowMainPhilipsHueAddBridgeFab(
                philipsHueViewModel = philipsHueViewModel,
                viewModel = viewModel,
                numActiveBridges = activeBridges.size
            )

        }

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
            testBridges.forEach { bridge ->

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
                            fontSize = 24.sp,
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
                                fontSize = 18.sp,
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

        // todo: show messages (if any)

    }
}


/**
 * Walks the user through the initialization process of the philips
 * hue bridge
 */
@Composable
fun ManualBridgeSetup(
    parentActivity: MainActivity,
    philipsHueViewmodel: PhilipsHueViewmodel,
    initBridgeState: BridgeInitStates,
    modifier: Modifier = Modifier,
) {
    val config = LocalConfiguration.current
    val landscape = config.orientation == ORIENTATION_LANDSCAPE

    when (initBridgeState) {
        BridgeInitStates.NOT_INITIALIZING -> {
            // this should not happen.
            SimpleBoxMessage(
                modifier,
                "error: should not be in ManualBridgeSetup() with this bridge init state.",
                { parentActivity.finish() },
                stringResource(id = R.string.exit)
            )
        }

        // These states involve stage 1
        BridgeInitStates.STAGE_1_GET_IP,
        BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT,
        BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> {
            if (landscape) {
                ManualBridgeSetupStep1_landscape(philipsHueViewmodel, initBridgeState)
            } else {
                ManualBridgeSetupStep1_Portrait(philipsHueViewmodel, initBridgeState)
            }
        }

        // Stage 2 states
        BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON,
        BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE,
        BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE,
        BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED,
        BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE -> {
            ManualBridgeSetupStep2(philipsHueViewmodel, initBridgeState)
        }

        BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
            ManualBridgeSetupStep3(philipsHueViewmodel)
        }

    }
}

@Composable
private fun ManualBridgeSetupStep1_landscape(
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {

    val ctx = LocalContext.current
    var ipText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
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
                        onNext = { viewmodel.addPhilipsHueBridgeIp(ipText) }
                    )
                )
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .width(120.dp),
                    onClick = { viewmodel.addPhilipsHueBridgeIp(ipText) }) {
                    Text(stringResource(id = R.string.next))
                }
            }
        }

    }

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
            stringResource(R.string.new_bridge_stage_1_error_no_bridge_at_ip, ipText),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }

}

@Composable
private fun ManualBridgeSetupStep1_Portrait(
    viewmodel: PhilipsHueViewmodel,
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
                                    onNext = { viewmodel.addPhilipsHueBridgeIp(ipText) }
                                )
                            )

                            Button(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(top = 8.dp, start = 12.dp)
                                    .width(120.dp),
                                onClick = { viewmodel.addPhilipsHueBridgeIp(ipText) }) {
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
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_bad_ip_format),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }
    if (state == BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT) {
        Toast.makeText(
            ctx,
            stringResource(R.string.new_bridge_stage_1_error_bad_ip_format),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }

}

@Composable
private fun ManualBridgeSetupStep2(
    viewmodel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {
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

        BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.connect_to_ph_bridge),
            fontSize = 28.sp,
        )

        Image(
            modifier = Modifier.weight(1f),     // image will fill remaining height
            contentScale = ContentScale.Fit,
            painter = painterResource(id = R.drawable.press_bridge_button),
            contentDescription = stringResource(id = R.string.press_bridge_button_desc)
        )

        val ipStr = viewmodel.newBridge?.ip
        Text(
            stringResource(R.string.connect_bridge_ip_success, ipStr ?: "error"),
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
            fontSize = 24.sp
        )
        Text(
            stringResource(R.string.press_bridge_button),
            modifier = Modifier.fillMaxWidth(0.5f),
            textAlign = TextAlign.Center,
            fontSize = 24.sp
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
                stringResource(R.string.ok),
                fontSize = 18.sp
            )
        }
    } // Column

    if (bridgeErrorMsg.isBlank() == false) {
        SimpleBoxMessage(
            msgText = bridgeErrorMsg,
            onClick = {
                viewmodel.bridgeAddErrorMsgIsDisplayed()
            },
            buttonText = stringResource(R.string.ok)
        )
    }
}


@Composable
private fun ManualBridgeSetupStep2_portait(
    philipsHueViewModel: PhilipsHueViewmodel,
    state: BridgeInitStates
) {
    // todo
}


@Composable
private fun ManualBridgeSetupStep3(philipsHueViewModel: PhilipsHueViewmodel) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()      // takes the insets into account (nav bars, etc)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.new_bridge_success),
            fontSize = 28.sp,
        )

        Button(
            onClick = {
                philipsHueViewModel.bridgeAddAllGoodAndDone()
            }
        ) { Text(stringResource(R.string.ok)) }

    }

}


@Composable
private fun ShowMainPhilipsHueAddBridgeFab(
    modifier: Modifier = Modifier,
    philipsHueViewModel: PhilipsHueViewmodel,
    viewModel: MainViewmodel,
    numActiveBridges: Int
) {
    // Add button to add a new bridge (if there are no active bridges, then
    // show the extended FAB)
    if (numActiveBridges == 0) {
        ExtendedFloatingActionButton(
            modifier = modifier
                .padding(top = 26.dp, end = 38.dp),
            onClick = { philipsHueViewModel.beginAddPhilipsHueBridge() },
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
            onClick = { philipsHueViewModel.beginAddPhilipsHueBridge() },
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

        Row (
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Switch(
                modifier = Modifier
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
            Spacer(modifier = Modifier.weight(1f))

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
            modifier = Modifier
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
//  previews
//---------------------------------

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