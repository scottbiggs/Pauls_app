package com.sleepfuriously.paulsapp.compose.philipshue

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.DrawInfoDialogLine
import com.sleepfuriously.paulsapp.compose.MyYesNoDialog
import com.sleepfuriously.paulsapp.compose.convertBrightnessFloatToInt
import com.sleepfuriously.paulsapp.compose.convertBrightnessIntToFloat
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.lightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.yellowDark
import com.sleepfuriously.paulsapp.ui.theme.yellowMain
import com.sleepfuriously.paulsapp.ui.theme.yellowVeryLight
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel

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
 * Displays the typical Philips Hue stuff: the contents and the
 * FAB for adding bridges.
 */
@Composable
fun ShowMainScreenPhilipsHue(
    modifier: Modifier = Modifier,
    philipsHueViewmodel: PhilipsHueViewmodel,
    bridges: List<PhilipsHueBridgeInfo>,
    flocks: List<PhilipsHueFlockInfo>
) {
    // the content
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeContentPadding()
    ) {
        DrawMainPhilipsHueAddBridgeFab(viewmodel = philipsHueViewmodel)

        DrawPhilipsHueContents(
            bridges = bridges,
            flocks = flocks,
            viewmodel = philipsHueViewmodel
        )
    }
}


/**
 * Displays the contents of the Philips Hue system.  This is the primary UI
 * the the PH, displaying the flocks and bridges in a lazy grid.
 */
@Composable
private fun DrawPhilipsHueContents(
    bridges: List<PhilipsHueBridgeInfo>,
    flocks: List<PhilipsHueFlockInfo>,
    viewmodel: PhilipsHueViewmodel
) {
    val noRoomsFound = stringResource(id = R.string.no_rooms_for_bridge)

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        state = viewmodel.lazyColumnState,
        columns = GridCells.Adaptive(MIN_PH_ROOM_WIDTH.dp),
        verticalArrangement = Arrangement.Top,
        horizontalArrangement = Arrangement.Start
    ) {

        //-------------
        //  flocks
        //-------------

        item(span = { GridItemSpan(this.maxLineSpan) }) {
            DrawFlocksSeparator(
                flockList = flocks,
                viewmodel = viewmodel
            )
        }

        flocks.forEach { flock ->
            item {
                DisplayPhilipsHueFlock(
                    flockName = flock.name,
                    sceneName = flock.currentSceneName,
                    illumination = convertBrightnessIntToFloat(flock.brightness),
                    lightSwitchOn = flock.onOffState,
                    flockOnOffChangedFunction = { newOnOff ->
                        viewmodel.sendFlockOnOffToBridges(
                            newOnOffState = newOnOff,
                            changedFlock = flock
                        )
                    },
                    flockBrightnessChangedFunction = { newBrightness ->
                        viewmodel.sendFlockBrightness(
                            newBrightness = convertBrightnessFloatToInt(newBrightness),
                            changedFlock = flock
                        )
                    },
                    showScenesFunction = {
                        viewmodel.showScenesForFlock(flock)
                    }
                )
            }
        }

        // Each bridge will consist of:
        //  - separator
        //  - radio button indicating connected status
        //    (are we connected to the bridge via sse)
        //  - title (name of the bridge)
        //  - drop-down menu:
        //      - bridge info
        //      - delete bridge
        //  - grid of all the rooms on the bridge
        bridges.forEach { bridgeInfo ->

            // Bridge separator
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                DrawBridgeSeparator(bridgeInfo, viewmodel)
            }

            // The name of the bridge.  This will take the entire row of a
            // grid and includes a radio button that toggles the server-sent
            // events on/off.
            item(
                span = { GridItemSpan(this.maxLineSpan) }       // makes this item take entire row
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        enabled = bridgeInfo.active,
                        selected = bridgeInfo.connected,
                        colors = RadioButtonDefaults.colors().copy(selectedColor = yellowMain, unselectedColor = yellowDark),
                        onClick = {
                            if (bridgeInfo.connected) {
                                viewmodel.disconnectBridge(bridgeInfo)
                            } else {
                                viewmodel.connectBridge(bridgeInfo)
                            }
                        },
                    )
                    DrawBridgeTitle(
                        text = stringResource(
                            id = R.string.ph_bridge_name,
                            bridgeInfo.humanName
                        ),
                        bridgeInfo.connected
                    )
                }
            }

            //--------
            //  rooms
            //--------

            if (bridgeInfo.rooms.isEmpty()) {
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
                bridgeInfo.rooms.forEach { room ->
                    item {
                        DisplayPhilipsHueRoom(
                            roomName = room.name,
                            sceneName = room.currentSceneName,
                            illumination = convertBrightnessIntToFloat(room.brightness),
                            lightSwitchOn = room.on,

                            roomBrightnessChangedFunction = { newIllumination ->
                                val intIllumination = (newIllumination * MAX_BRIGHTNESS).toInt()
                                viewmodel.sendRoomBrightness(
                                    newBrightness = intIllumination,
                                    changedRoom = room
                                )
                            },

                            roomOnOffChangedFunction = { newOnOff ->
                                viewmodel.sendRoomOnOff(
                                    newOnOffState = newOnOff,
                                    changedRoom = room
                                )
                            },

                            showScenesFunction = {
                                viewmodel.showScenesForRoom(bridgeInfo, room)
                            }
                        )
                    }
                }
            }

            //--------
            // zones
            //--------

            bridgeInfo.zones.forEach { zone ->
                item {
                    DisplayPhilipsHueZone(
                        zoneName = zone.name,
                        sceneName = zone.currentSceneName,
                        illumination = zone.brightness.toFloat() / MAX_BRIGHTNESS.toFloat(),
                        lightSwitchOn = zone.on,
                        zoneBrightnessChangedFunction = { newIllumination ->
                            val intIllumination = (newIllumination * MAX_BRIGHTNESS).toInt()
                            viewmodel.sendZoneBrightness(
                                newBrightness = intIllumination,
                                changedZone = zone
                            )
                        },
                        zoneOnOffChangedFunction = { newOffStatus ->
                            viewmodel.sendZoneOnOff(
                                newOnOffState = newOffStatus,
                                changedZone = zone
                            )
                        },
                        showScenesFunction = {
                            viewmodel.showScenesForZone(bridgeInfo, zone)
                        }
                    )
                }
            }

        }
    }
}

/**
 * A nice separator between bridges.  It also shows the name of the
 * bridge and how many rooms are in it. todo: make optional
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
    ) {
        DotDotDotBridgeMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            viewmodel = viewmodel,
            bridge = bridge,
        )
    }
}

@Composable
private fun DotDotDotBridgeMenu(
    modifier : Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    bridge: PhilipsHueBridgeInfo,
) {
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
                text = { Text(stringResource(R.string.show_info)) },
                onClick = {
                    isDropDownExpanded = false
                    showInfoDialog = true
                },
            )

            // refresh the bridge, but only if its connected
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ph_reset)) },
                onClick = {
                    isDropDownExpanded = false
                    viewmodel.resetBridge(bridge)
                },
                enabled = bridge.connected
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
                    viewmodel.deleteBridge(bridge.v2Id)    // this spurns a new coroutine and returns immediately
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

/**
 * Displays the dialog with all sorts of information about the bridge.
 */
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
                    // human name
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.name),
                            bridge.humanName
                        )
                    }

                    // id
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.id),
                            bridge.v2Id
                        )
                    }

                    // ip
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.ip),
                            bridge.ipAddress
                        )
                    }

                    // active
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.active),
                            bridge.active.toString()
                        )
                    }

                    // connected
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.connected),
                            bridge.connected.toString()
                        )
                    }

                    // token
                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.token),
                            bridge.token
                        )
                    }

                    // rooms
                    item {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }

                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.bridge_info_rooms),
                            bridge.rooms.size.toString()
                        )
                    }
                    bridge.rooms.forEach { room ->
                        item {
                            DrawInfoDialogLine(
                                stringResource(R.string.bridge_info_room),
                                room.name
                            )
                        }

                        item {
                            DrawInfoDialogLine(
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
                                DrawInfoDialogLine(
                                    light.name,
                                    "$onStr, brightness = $brightness",
                                    width = 180
                                )
                            }
                        }
                    }

                    // zones
                    item {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    }

                    item {
                        DrawInfoDialogLine(
                            stringResource(R.string.bridge_info_zones),
                            body = bridge.zones.size.toString()
                        )
                    }
                    bridge.zones.forEach { zoneInfo ->
                        item {
                            DrawInfoDialogLine(
                                stringResource(R.string.bridge_info_zone),
                                zoneInfo.name
                            )
                        }

                        item {
                            DrawInfoDialogLine(
                                stringResource(R.string.bridge_info_lights),
                                zoneInfo.lights.size.toString(),
                                width = 150
                            )
                        }

                        // fixme: no lights!!!
                        zoneInfo.lights.forEach { light ->
                            val onStr =
                                if (light.state.on) ctx.getString(R.string.light_on) else ctx.getString(
                                    R.string.light_off
                                )
                            val brightness = light.state.bri
                            item {
                                DrawInfoDialogLine(
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
 * Draws the title of each bridge.  Its formatting depends on the connected status.
 */
@Composable
private fun DrawBridgeTitle(
    text: String,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .fillMaxWidth(),
        text = text,
        style = if (connected) {
            MaterialTheme.typography.headlineSmall
        }
        else {
            MaterialTheme.typography.headlineSmall.copy(
                textDecoration = TextDecoration.LineThrough
            )
        },
        color = if (connected) { yellowVeryLight }
                else { lightCoolGray }
    )
}

@Composable
private fun DrawMainPhilipsHueAddBridgeFab(
    modifier: Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel
) {
    Box(modifier = modifier.fillMaxWidth()) {

    // Add button to add a new bridge (if there are no active bridges, then
    // show the extended FAB)
    if (viewmodel.philipsHueBridgesCompose.isEmpty()) {

        // No bridges so make the add button more pronounced.  Same for detect bridges button.

        Row {
            ExtendedFloatingActionButton(
                modifier = modifier
                    .padding(end = 32.dp),
                onClick = { viewmodel.beginAddPhilipsHueBridge() },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.ph_add_button_content_desc)
                    )
                },
                text = { Text(stringResource(id = R.string.ph_add_button)) }
            )

            ExtendedFloatingActionButton(
                onClick = { viewmodel.beginDetectPhilipsHueBridges() },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.ph_detect_bridges_button_content_desc)
                    )
                },
                text = { Text(stringResource(id = R.string.ph_detect_bridges_button)) }
            )
        }
    } else {
        Row {
            FloatingActionButton(
                modifier = modifier
                    .padding(end = 32.dp),
                onClick = { viewmodel.beginAddPhilipsHueBridge() },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(id = R.string.ph_add_button_content_desc)
                )
            }

            // button for detecting bridges
            FloatingActionButton(
                onClick = { viewmodel.beginDetectPhilipsHueBridges() },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AddHome,
                    contentDescription = stringResource(id = R.string.ph_add_button_content_desc)
                )
            }
        }
    }
        }
}


//---------------------------------
//  previews
//---------------------------------

//---------------------------------
//  constants
//---------------------------------

private const val TAG = "PhilipsHueCompose"

/** The grid of rooms in the PH section, each room must be at least this wide */
const val MIN_PH_ROOM_WIDTH = 130

/** the maximum light that a light can put out */
const val MAX_BRIGHTNESS = 100

/** min light that a light can emit */
const val MIN_BRIGHTNESS = 0