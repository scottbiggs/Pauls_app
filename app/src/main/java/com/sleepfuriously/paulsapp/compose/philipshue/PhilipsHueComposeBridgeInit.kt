package com.sleepfuriously.paulsapp.compose.philipshue

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.MainActivity
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.SimpleFullScreenBoxMessage
import com.sleepfuriously.paulsapp.compose.TextFieldAndButton
import com.sleepfuriously.paulsapp.viewmodels.BridgeInitStates
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel

/**
 * Holds Composables used when initializing a Philips Hue bridge
 */

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
        ManualInitWaiting(modifier)
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
            BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED,
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
            stringResource(
                R.string.new_bridge_stage_1_error_no_bridge_at_ip,
                viewmodel.workingNewBridge?.ip ?: ""
            ),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }
    if (state == BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED) {
        Toast.makeText(
            ctx,
            stringResource(
                R.string.new_bridge_stage_1_error_already_initialized,
                viewmodel.workingNewBridge?.ip ?: ""
            ),
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
            stringResource(
                R.string.new_bridge_stage_1_error_no_bridge_at_ip,
                viewmodel.workingNewBridge?.ip ?: ""
            ),
            Toast.LENGTH_LONG
        ).show()
        viewmodel.bridgeAddErrorMsgIsDisplayed()
    }
    if (state == BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP) {
        Toast.makeText(
            ctx,
            stringResource(
                R.string.new_bridge_stage_1_error_no_bridge_at_ip,
                viewmodel.workingNewBridge?.ip ?: ""
            ),
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

    val bridgeErrorMsg = when (state) {
        BridgeInitStates.NOT_INITIALIZING -> ""
        BridgeInitStates.STAGE_1_GET_IP -> ""
        BridgeInitStates.STAGE_1_ERROR__BAD_IP_FORMAT -> ""
        BridgeInitStates.STAGE_1_ERROR__NO_BRIDGE_AT_IP -> ""
        BridgeInitStates.STAGE_1_ERROR__BRIDGE_ALREADY_INITIALIZED -> ""
        BridgeInitStates.STAGE_2_PRESS_BRIDGE_BUTTON -> ""
        BridgeInitStates.STAGE_2_ERROR__NO_TOKEN_FROM_BRIDGE -> stringResource(
            R.string.registering_bridge_button_not_pressed,
            viewmodel.workingNewBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_2_ERROR__UNSUCCESSFUL_RESPONSE -> stringResource(
            R.string.registering_bridge_unsuccessful,
            viewmodel.workingNewBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_2_ERROR__CANNOT_PARSE_RESPONSE -> stringResource(R.string.registering_bridge_cannot_parse_response)
        BridgeInitStates.STAGE_2_ERROR__BUTTON_NOT_PUSHED -> stringResource(
            R.string.registering_bridge_button_not_pressed,
            viewmodel.workingNewBridge?.ip ?: "null"
        )

        BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE -> stringResource(R.string.bridge_ip_step_3_problem_adding_bridge)
        BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> ""
    }

    if (bridgeErrorMsg.isBlank()) {
        Column(
            modifier = modifier
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
            val ipStr = viewmodel.workingNewBridge?.ip
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

    when (state) {
        BridgeInitStates.STAGE_3_ALL_GOOD_AND_DONE -> {
            SimpleFullScreenBoxMessage(
                backgroundModifier = modifier,
                onClick = {
                    viewmodel.bridgeAddAllGoodAndDone()
                },
                msgText = stringResource(id = R.string.new_bridge_success),
                buttonText = stringResource(R.string.ok)
            )
        }
        BridgeInitStates.STAGE_3_ERROR_CANNOT_ADD_BRIDGE -> {
            SimpleFullScreenBoxMessage(
                backgroundModifier = modifier,
                onClick = {
                    viewmodel.bridgeAddAllGoodAndDone()
                },
                msgText = stringResource(id = R.string.bridge_ip_step_3_problem_adding_bridge),
                buttonText = stringResource(R.string.ok)
            )
        }
        else -> {
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
    }

}

/**
 * Display while the app is waiting for the bridge to respond.
 */
@Composable
private fun ManualInitWaiting(
    modifier: Modifier = Modifier
) {

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

private const val TAG = "PhilipsHueBridgeInit"