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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.model.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.lightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryLightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.yellowVeryLight

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
 */
@Composable
fun ShowMainScreenPhilipsHue(
    modifier: Modifier = Modifier,
    philipsHueViewmodel: PhilipsHueViewmodel,
    bridges: Set<PhilipsHueBridgeInfo>
) {
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

        DrawBridgeContents(
//            bridgeInfoSet = philipsHueViewmodel.philipsHueBridgesCompose,
            bridges = bridges,
            viewmodel = philipsHueViewmodel
        )
    }

    // display the fab on top of stuff
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        DrawMainPhilipsHueAddBridgeFab(
            modifier = Modifier
                .align(Alignment.TopEnd),    // only works if parent is Box
            viewmodel = philipsHueViewmodel,
        )
    }

    // todo: show messages (if any)

}

@Composable
private fun DrawBridgeContents(
    bridges: Set<PhilipsHueBridgeInfo>,
    viewmodel: PhilipsHueViewmodel
) {
    Log.d(TAG, "DrawBridgeContents() start")
    val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)

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
        bridges.forEach { bridgeInfo ->

            item(span = { GridItemSpan(this.maxLineSpan) }) {
                DrawBridgeSeparator(bridgeInfo, viewmodel)
            }

            // The first item (which has the name of the bridge)
            // will take the entire row of a grid.
            item(
                span = { GridItemSpan(this.maxLineSpan) }       // makes this item take entire row
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        enabled = bridgeInfo.active,
                        selected = bridgeInfo.connected,
                        onClick = { },
                    )
                    DrawBridgeTitle(
                        text = stringResource(
                            id = R.string.ph_bridge_name,
                            bridgeInfo.labelName
                        )
                    )
                }
            }

            if (bridgeInfo.rooms.size == 0) {
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
                Log.d(TAG, "DrawBridgeContents: updating display of ${bridgeInfo.rooms.size} rooms")
                bridgeInfo.rooms.forEach { room ->
                    Log.d(TAG, "DrawBridgeContents() - drawing room ${room.name}, on = ${room.on}, bri = ${room.brightness}")
                    item {
                        DisplayPhilipsHueRoom(
                            roomName = room.name,
                            illumination = room.brightness.toFloat() / MAX_BRIGHTNESS.toFloat(),
                            lightSwitchOn = room.on,
                            roomChangeCompleteFunction = { newIllumination, newSwitchOn ->
                                val intIllumination = (newIllumination * MAX_BRIGHTNESS).toInt()
                                Log.d(TAG, "calling viewmodel.roomBrightnessChanged(): new brightness = $intIllumination, on = $newSwitchOn")
                                viewmodel.changeRoomBrightness(
                                    (newIllumination * MAX_BRIGHTNESS).toInt(),
                                    newSwitchOn,
                                    room,
                                    bridgeInfo
                                )
                            },
                            showScenesFunction = {
                                viewmodel.showScenes(bridgeInfo, room)
                            }
                        )
                    }
                }
            }
        }
    }

}

/**
 * A nice separator between bridges.
 */
@Composable
private fun DrawBridgeSeparator(
    bridge: PhilipsHueBridgeInfo,
    viewmodel: PhilipsHueViewmodel
) {
    val modifier = remember {
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
        bridge = bridge,
    )
}

@Composable
private fun DotDotDotBridgeMenu(
    modifier : Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    bridge: PhilipsHueBridgeInfo,
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
                },
            )

            // disconnect or reconnect bridge (so we can receive updates)
            DropdownMenuItem(
                text = {
                    if (bridge.connected) {
                        Text(stringResource(R.string.disconnect_bridge))
                    }
                    else {
                        Text(stringResource(R.string.reconnect_bridge))
                    }
                },
                onClick = {
                    isDropDownExpanded = false
                    if (bridge.connected) {
                        viewmodel.disconnectBridge(bridge)
                    }
                    else {
                        viewmodel.connectBridge(bridge)
                    }
                },
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
                    viewmodel.deleteBridge(bridge.id)    // this spurns a new coroutine and returns immediately
                },
                titleText = stringResource(R.string.delete_bridge_confirm_title),
                bodyText = stringResource(R.string.delete_bridge_confirm_body),
            )
        }

        // Display all the info we know about this bridge
        if (showInfoDialog) {
            ShowBridgeInfoDialog(
                bridge,
                onClick = {
                    showInfoDialog = false
                })
        }
    }
}

@Composable
private fun ShowBridgeInfoDialog(
    bridge: PhilipsHueBridgeInfo,
    onClick: () -> Unit
) {

    val ctx = LocalContext.current

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
                            bridge.id
                        )
                    }

                    // ip
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.ip),
                            bridge.ip
                        )
                    }

                    // active
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.active),
                            bridge.active.toString()
                        )
                    }

                    // connected
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.connected),
                            bridge.connected.toString()
                        )
                    }

                    // token
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.token),
                            bridge.token
                        )
                    }

                    // rooms
                    item {
                        DrawBridgeInfoLine(
                            stringResource(R.string.bridge_info_rooms),
                            bridge.rooms.size.toString()
                        )
                    }
                    bridge.rooms.forEach { room ->
                        item {
                            DrawBridgeInfoLine(
                                stringResource(R.string.bridge_info_room),
                                room.id
                            )
                        }

                        item {
                            DrawBridgeInfoLine(
                                stringResource(R.string.bridge_info_lights),
                                room.lights.size.toString(),
                                width = 150
                            )
                        }

                        room.lights.forEach { light ->
                            val onStr =
                                if (light.state.on) ctx.getString(R.string.light_on) else ctx.getString(
                                    R.string.light_off
                                )
                            val brightness = light.state.bri
                            item {
                                DrawBridgeInfoLine(
                                    light.name,
                                    "$onStr, brightness = $brightness",
                                    width = 180
                                )
                            }
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
    width: Int = 120
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
private fun DrawBridgeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = yellowVeryLight
    )
}

@Composable
private fun DrawMainPhilipsHueAddBridgeFab(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel
) {
    // Add button to add a new bridge (if there are no active bridges, then
    // show the extended FAB)
    if (viewmodel.philipsHueBridgesCompose.isEmpty()) {
        ExtendedFloatingActionButton(
            modifier = modifier
                .padding(top = 8.dp, end = 38.dp),
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
                .padding(top = 8.dp, end = 38.dp),
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
 * @param   roomChangeCompleteFunction  Function to call when the illumination is
 *                                  changed and completed by the user.  It takes the new
 *                                  illumination value and a boolean for if the
 *                                  switch is on/off.
 */
@Composable
private fun DisplayPhilipsHueRoom(
    modifier: Modifier = Modifier,
    roomName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    roomChangeCompleteFunction: (newIllumination: Float, newSwitchOn: Boolean) -> Unit,
    showScenesFunction: () -> Unit
) {
    // variables for displaying the lightbulb image
    val lightImage = getProperLightImage(illumination)    // changes while hand is sliding
    val lightImageColor = getLightColor(illumination)

    Column(modifier = modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(10.dp))
        .border(
            BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.secondary)),
            RoundedCornerShape(12.dp)
        )

    ) {
        Row {
            Text(
                text = stringResource(R.string.room),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 4.dp, start = 8.dp)
            )
            IconButton(
                modifier = Modifier
                    .size(22.dp)
                    .padding(start = 4.dp),
                onClick = showScenesFunction
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.scenes_butt),
                )
            }

        }

        ClickableText(
            text = AnnotatedString(roomName),
            onClick = {
                showScenesFunction.invoke()
            },
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(start = 16.dp)
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
                checked = lightSwitchOn,
                onCheckedChange = { newSliderState ->
                    roomChangeCompleteFunction.invoke(illumination, newSliderState)
                }
            )

            // This pushes the switch to the far left and the lightbulb to
            // the far right.
            Spacer(modifier = Modifier.weight(1f))

            DrawLightBulb(lightImage, lightImageColor)
        }

        SliderReportWhenFinished(
            sliderInputValue = illumination,
            setSliderValueFunction = { finalValue ->
                roomChangeCompleteFunction.invoke(finalValue, lightSwitchOn)
            },
            enabled = lightSwitchOn,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(veryDarkCoolGray)
                .height(20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DrawLightBulb(imageId : Int, colorTint: Color) {
    Image(
        modifier = Modifier
            .width(110.dp),
        contentScale = ContentScale.Fit,
        painter = painterResource(imageId),
        colorFilter = ColorFilter.tint(colorTint),
        contentDescription = stringResource(id = R.string.lightbulb_content_desc)
    )
}

private fun getProperLightImage(illumination: Float) : Int {
    val lightImage =
        if (illumination < 0.05f) {
            R.drawable.bare_bulb_white_04
        }
        else if (illumination < 0.4f) {
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
        if (illumination < 0.05f) {
            coolGray
        }
        else if (illumination < 0.4f) {
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

//---------------------------------
//  constants
//---------------------------------

private const val TAG = "PhilipsHueCompose"

/** The grid of rooms in the PH section, each room must be at least this wide */
private const val MIN_PH_ROOM_WIDTH = 130