package com.sleepfuriously.paulsapp.compose

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    philipsHueViewmodel: PhilipsHueViewmodel
//    bridges: Set<PhilipsHueBridgeInfo>,
) {
    Log.v(TAG, "ShowMainScreenPhilipsHue() begin. num bridges = ${philipsHueViewmodel.philipsHueBridgesCompose.size}")

    val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)

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
            // Each bridge will consist of:
            //  - separator
            //  - title (name of the bridge)
            //  - drop-down menu:
            //      - bridge info
            //      - delete bridge
            //  - grid of all the rooms on the bridge
            philipsHueViewmodel.philipsHueBridgesCompose.forEach { bridge ->

                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    DrawBridgeSeparator(bridge.id, philipsHueViewmodel)
                }


                // The first item (which has the name of the bridge)
                // will take the entire row of a grid.
                item(
                    span = { GridItemSpan(this.maxLineSpan) }       // makes this item take entire row
                ) {
                    DrawBridgeTitle(text = stringResource(id = R.string.ph_bridge_name, bridge.id))
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
        )
    }

    // todo: show messages (if any)

}

/**
 * A nice separator between bridges.
 */
@Composable
private fun DrawBridgeSeparator(
    bridgeId: String,
//    bridges: Set<PhilipsHueBridgeInfo>,
    viewmodel: PhilipsHueViewmodel
) {
    var modifier = remember {
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .height(28.dp)
    }
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        coolGray,
                        Color(red = 0, green = 0, blue = 0, alpha = 0)
                    )
                )
            )
    )

    DotDotDotBridgeMenu(
        modifier = modifier,
        viewmodel = viewmodel,
        bridgeId = bridgeId,
    )
}

@Composable
private fun DotDotDotBridgeMenu(
    modifier : Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    bridgeId: String,
) {
    val ctx = LocalContext.current
    var isDropDownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // A second identical box is needed to make the drop-down
    // menu appear on the top right.  This also includes the
    // drop-down button which is drawn over the above Box.
    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {

        // We'll use an IconButton for extra commands related to
        // this bridge
        IconButton(
            modifier = Modifier
                .padding(vertical = 2.dp, horizontal = 8.dp)
                .align(Alignment.CenterEnd),
            onClick = { isDropDownExpanded = !isDropDownExpanded }
        ) {
            // on the far right, draw an icon
            Image(
                contentScale = ContentScale.Fit,
                painter = painterResource(R.drawable.baseline_more_vert_24),
                contentDescription = stringResource(id = R.string.drop_down_menu_desc),
            )
        }

        DropdownMenu(
            modifier = Modifier
                .align(Alignment.TopEnd),
            expanded = isDropDownExpanded,
            onDismissRequest = {
                isDropDownExpanded = false
            }
        ) {
            // show bridge info
            DropdownMenuItem(
                text = { Text(stringResource(R.string.show_bridge_info)) },
                onClick = {
                    isDropDownExpanded = false
                    showInfoDialog = true
                }
            )

            // delete bridge
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_bridge)) },
                onClick = {
                    isDropDownExpanded = false
                    showDeleteDialog = true
                    Log.d(TAG, "selected info menu item - showInfoDialog = $showInfoDialog")
                }
            )

            // add test bridge
            DropdownMenuItem(
                text = { Text("add test bridge") },
                onClick = {
                    isDropDownExpanded = false
                    viewmodel.addBridgeTest()
                    Toast.makeText(ctx, "Just added a fake bridge!", Toast.LENGTH_LONG).show()
                }
            )
        }

        // delete confirmation dialog
        if (showDeleteDialog) {
            MyYesNoDialog(
                onDismiss = {
                    showDeleteDialog = false
                },
                onConfirm = {
                    showDeleteDialog = false
                    viewmodel.deleteBridge(bridgeId)    // this spurns a new coroutine and returns immediately
                },
                titleText = stringResource(R.string.delete_bridge_confirm_title),
                bodyText = stringResource(R.string.delete_bridge_confirm_body),
            )
        }

        // Display all the info we know about this bridge
        if (showInfoDialog) {
            Log.d(TAG, "showing info dialog - showInfoDialog = $showInfoDialog")
            ShowBridgeInfoDialog(
                bridgeId,
                viewmodel,
                onClick = {
                    showInfoDialog = false
                    Log.d(TAG, "click! - showInfoDialog = $showInfoDialog")
                })
        }
    }
}

@Composable
private fun ShowBridgeInfoDialog(
    bridgeId: String,
    viewmodel: PhilipsHueViewmodel,
    onClick: () -> Unit
) {

    val bridge = remember { viewmodel.getBridgeInfo(bridgeId) }

    AlertDialog(
        onDismissRequest = onClick,
        title = {
            Text(stringResource(R.string.show_bridge_info_title))
        },
        text = {
            SelectionContainer {
                // This is the meat of the function.  All the data goes here.
                LazyColumn {
                    // id
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.id),
                            bridge?.id ?: stringResource(R.string.no_bridge_id)
                        )
                    }

                    // ip
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.ip),
                            bridge?.ip ?: stringResource(R.string.no_bridge_id)
                        )
                    }

                    // active
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.active),
                            bridge?.active?.toString() ?: stringResource(R.string.not_applicable)
                        )
                    }

                    // token
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.token),
                            bridge?.token ?: stringResource(R.string.not_applicable)
                        )
                    }

                    // last used
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.bridge_last_used),
                            bridge?.lastUsed?.toString() ?: stringResource(R.string.not_applicable)
                        )
                    }

                    // rooms
                    if (bridge?.rooms != null) {
                        item {
                            DrawBridgeInfoLine(
                                stringResource(R.string.bridge_info_room),
                                bridge.rooms.size.toString()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClick
            ) { Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { }
    )

}

/**
 * Draws the given two texts in a Row suitable for displaying in the Bridge
 * Info screen.
 *
 * @param   title       The first part.  It'll be bigger and bolder.
 *                      Just a word or two.
 *
 * @param   body        The right part.  Should be the main data.
 *
 * @param   width       Width of the title section.  Default should be good for
 *                      a nice sized word.
 */
@Composable
private fun DrawBridgeInfoLine(
    title: String,
    body: String,
    width: Int = 90
) {
    Row {
        Text(
            text = title,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(width.dp)
                .alignByBaseline()
                .padding(end = 8.dp)
        )
        Text(
            text = body,
            modifier = Modifier
                .alignByBaseline()
        )
    }

}

@Composable
private fun DrawBridgeTitle(text: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 2.dp, bottom = 8.dp),
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = yellowVeryLight
    )
}

@Composable
private fun ShowMainPhilipsHueAddBridgeFab(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel
) {
    // Add button to add a new bridge (if there are no active bridges, then
    // show the extended FAB)
    if (viewmodel.philipsHueBridgesCompose.isEmpty()) {
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