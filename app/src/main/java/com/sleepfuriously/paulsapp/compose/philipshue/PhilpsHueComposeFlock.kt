package com.sleepfuriously.paulsapp.compose.philipshue

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.DrawInfoDialogLine
import com.sleepfuriously.paulsapp.compose.MyYesNoDialog
import com.sleepfuriously.paulsapp.compose.SliderReportWhenFinished
import com.sleepfuriously.paulsapp.compose.SwitchWithLabel
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.EMPTY_STRING
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.yellowMain
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import kotlin.collections.forEach


/**
 * UI for a [PhilipsHueFlockInfo].  Similar to PhilipsHueComposeRoom.
 *
 * @param   flockName       The name to display
 *
 * @param   sceneName       The name of the current scene.  Use empty string
 *                          if no scene name should be displayed.
 *
 * @param   illumination    How much is this Flock currently illumniated.
 *                          0 = off all the way to 1 = full on.
 *
 * @param   lightSwitchOn   Is the switch for the light on (true) or off (false)
 *
 * @param   flockBrightnessChangedFunction   Function to call when the brightness
 *                          is changed by the user.  The parameter will be the
 *                          new illumination value.
 *
 * @param   flockOnOffChangedFunction        Function to call when the user
 *                          switches a Flock on or off.  The parameter is false
 *                          when the user wants the Flock off, and it'll be
 *                          true when the user wants to switch it on.
 *
 * @param   showScenesFunction      Function to call when the user wants to
 *                          display all the scenes applicable to this Flock.
 */
@Composable
fun DisplayPhilipsHueFlock(
    modifier: Modifier = Modifier,
    flockName: String,
    sceneName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    flockBrightnessChangedFunction: (newBrightness: Float) -> Unit,
    flockOnOffChangedFunction: (newOnOff: Boolean) -> Unit,
    showScenesFunction: () -> Unit
) {
    // variables for displaying the lightbulb image
    val lightImage = remember { getProperLightImage(illumination) }   // changes while hand is sliding
    val lightImageColor = remember { getLightColor(illumination) }

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
                text = stringResource(R.string.flock),
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
            text = AnnotatedString(flockName),
            onClick = {
                showScenesFunction.invoke()
            },
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(start = 16.dp)
        )

        // display the name of the current scene
        if (sceneName.isNotEmpty()) {
            Text(
                sceneName,
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                textAlign = TextAlign.End)
        }

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
                colors = SwitchDefaults.colors().copy(checkedTrackColor = yellowMain),
                onCheckedChange = { newSliderState ->
                    flockOnOffChangedFunction.invoke(newSliderState)
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
                flockBrightnessChangedFunction.invoke(finalValue)
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

/**
 * A nice separator between groups of things.  It also shows the name of the
 * group and holds a dotdotdot menu.
 */
@Composable
fun DrawFlocksSeparator(
    flockList: List<PhilipsHueFlockInfo>,
    viewmodel: PhilipsHueViewmodel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
    ) {
        DotDotDotFlockMenu(
            modifier = Modifier.align(Alignment.CenterEnd),
            viewmodel = viewmodel,
            flocks = flockList
        )
    }

}


@Composable
private fun DotDotDotFlockMenu(
    modifier : Modifier = Modifier,
    viewmodel: PhilipsHueViewmodel,
    flocks: List<PhilipsHueFlockInfo>
) {
    var isDropDownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var flockToDelete by remember { mutableStateOf<PhilipsHueFlockInfo?>(null) }

    // A second identical box is needed to make the drop-down
    // menu appear on the top right.  This also includes the
    // drop-down button which is drawn over the above Box.
    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {

        // We'll use an IconButton for extra commands related to
        // this flock
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
            // show flock info
            DropdownMenuItem(
                text = { Text(stringResource(R.string.show_info)) },
                onClick = {
                    isDropDownExpanded = false
                    showInfoDialog = true
                },
            )

            // add a flock
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_flock_confirm_button)) },
                onClick = {
                    isDropDownExpanded = false
                    viewmodel.beginAddFlockDialog()
                },
            )

            // edit a flock. choose which wisely
            flocks.forEach { flock ->
                DropdownMenuItem(
                    text = { Text("${stringResource(R.string.edit_flock)}: ${flock.name}") },
                    onClick = {
                        isDropDownExpanded = false
                        viewmodel.beginEditFlockDialog(flock)
                    }
                )
            }

            // delete a flock, but which?  A list for deleting each one...
            flocks.forEach { flock ->
                DropdownMenuItem(
                    text = { Text("${stringResource(R.string.delete_flock)}: ${flock.name}") },
                    onClick = {
                        isDropDownExpanded = false
                        flockToDelete = flock
                        showDeleteDialog = true
                    }
                )
            }
        } // DropdownMenu

        // delete confirmation dialog
        if (showDeleteDialog) {
            MyYesNoDialog(
                onDismiss = {
                    showDeleteDialog = false
                    flockToDelete = null
                },
                onConfirm = {
                    showDeleteDialog = false
                    viewmodel.deleteFlock(flockToDelete!!)
                    flockToDelete = null
                },
                titleText = stringResource(R.string.delete_flock_confirm_title),
                bodyText = stringResource(R.string.delete_flock_confirm_body),
            )
        }

        // Display all the info we know about this flock
        if (showInfoDialog) {
            ShowFlockInfoDialog(
                flockList = flocks,
                onClick = {
                    showInfoDialog = false
                })
        }
    }
}

/**
 * Displays the dialog with all sorts of information about all our flocks.
 */
@Composable
private fun ShowFlockInfoDialog(
    flockList: List<PhilipsHueFlockInfo>,
    onClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClick,
        title = {
            Text(stringResource(R.string.show_flock_info_title))
        },
        text = {
            SelectionContainer {
                // This is the meat of the function.  All the data goes here.
                LazyColumn {

                    // number of flocks
                    item {
                        DrawInfoDialogLine(
                            title = stringResource(R.string.flock_size),
                            body = flockList.size.toString()
                        )
                    }

                    // for each flock...
                    for (flock in flockList) {
                        item { HorizontalDivider() }

                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.name),
                                body = flock.name)
                        }
                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.scene_name_prefix),
                                body = flock.currentSceneName
                            )
                        }
                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.on_title),
                                body = stringResource(if (flock.onOffState) R.string.light_on else R.string.light_off)
                            )
                        }
                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.brightness),
                                body = flock.brightness.toString()
                            )
                        }

                        // rooms
                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.bridge_info_rooms),
                                body = flock.roomSet.size.toString()
                            )
                        }
                        for (room in flock.roomSet) {
                            item {
                                DrawInfoDialogLine(
                                    title = "",
                                    body = "${room.name} (ip ${room.bridgeIpAddress}) : ${if (room.on) "on" else "off"}, ${room.brightness}"
                                )
                            }
                        }

                        // zones
                        item {
                            DrawInfoDialogLine(
                                title = stringResource(R.string.bridge_info_zones),
                                body = flock.zoneSet.size.toString()
                            )
                        }
                        for (zone in flock.zoneSet) {
                            item {
                                DrawInfoDialogLine(
                                    title = "",
                                    body = "${zone.name} (ip ${zone.bridgeIpAddress}) : ${if (zone.on) "on" else "off"}, ${zone.brightness}"
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
 * Call this when the user wants to build a brand new Flock or modify an
 * existing flock.  The viewmodel will know if this is a NEW flock or an
 * edit.
 *
 * @param   viewmodel       Access to the viewmodel.  This will provide the
 *                          lists of room and zones that the flock will be
 *                          built on.  It also handles the callbacks when
 *                          the user selects things.
 *
 * @param   errorMsg        When not empty display this error message.
 *
 * preconditions
 *  - the viewmodel's originalFlock variable will tell us if this is an
 *    add or an edit (null --> add)
 */
@Composable
fun ShowAddOrEditFlockDialog(
    viewmodel: PhilipsHueViewmodel,
    errorMsg: String,
    allRooms: Set<PhilipsHueRoomInfo>,
    allZones: Set<PhilipsHueZoneInfo>,

    /**
     * This function will be called any time the user toggles on/off a room or
     * a zone.
     *
     * params
     *  - whether the selection was turned on or off (true or false)
     *  - The [PhilipsHueRoomInfo] that was selected, or null if not applicable
     *  - The [PhilipsHueZoneInfo] that was modified (null if it's a room)
     */
    onToggled: (Boolean, PhilipsHueRoomInfo?, PhilipsHueZoneInfo?) -> Unit,

    /**
     * Function to call when the user has finally constructed a Flock.
     * The caller should already know what has been selected from calls
     * to [onToggled].
     *
     * Note:
     *  The function REALLY should do something so that this dialog is
     *  no longer called (viewmodel, hint hint).
     *
     * params
     *  - name for this Flock
     *
     * No return value
     */
    onOk: (String) -> Unit
) {
    /** the name the user is using for this flock */
    var name by remember {
        if (viewmodel.isCurrentlyEditingFlock()) {
            mutableStateOf(viewmodel.originalFlock!!.name)
        }
        else {
            mutableStateOf(EMPTY_STRING)
        }
    }

    val selectedRooms by rememberSaveable {
        if (viewmodel.isCurrentlyEditingFlock()) {
            // this is to recall the original selected rooms
            mutableStateOf(viewmodel.originalFlock!!.roomSet.toMutableSet())
        }
        else {
            mutableStateOf(mutableSetOf())
        }
    }

    val selectedZones by rememberSaveable {
        if (viewmodel.isCurrentlyEditingFlock()) {
            // recall the original selected zones
            mutableStateOf(viewmodel.originalFlock!!.zoneSet.toMutableSet())
        }
        else {
            mutableStateOf(mutableSetOf())
        }
    }

    AlertDialog(
        onDismissRequest = { },     // don't let the user inadvertantly tap away--they NEED to click cancel or OK
        title = {
            // title for add a flock
            if (viewmodel.isCurrentlyAddingFlock()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.add_flock_title)
                )
            }
            // title for edit a flock. includes the original flock
            else {
                Column {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        text = stringResource(R.string.add_flock_title)
                        )
                    Text("${stringResource(R.string.edit_flock_orig_name)}: ${viewmodel.originalFlock!!.name}")
                }
            }
        },
        text = {
            SelectionContainer {

                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Rooms
                        item {
                            Text(
                                text = stringResource(R.string.rooms),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        allRooms.forEach { room ->
                            item {
                                var checked by remember { mutableStateOf(selectedRooms.contains(room)) }
                                SwitchWithLabel(
                                    label = room.name,
                                    state = checked,
                                    textBefore = false,
                                    onStateChange = { turnedOn ->
                                        // call onToggled() with the room (zone is null)
                                        onToggled(turnedOn, room, null)
                                        checked = turnedOn
                                        if (turnedOn) { selectedRooms.add(room) }
                                        else { selectedRooms.remove(room) }
                                    }
                                )
                            }
                        }

                        // Zones
                        item {
                            Text(
                                stringResource(R.string.zones),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        allZones.forEach { zone ->
                            item {
                                var checked by remember { mutableStateOf(selectedZones.contains(zone)) }
                                SwitchWithLabel(
                                    label = zone.name,
                                    state = checked,
                                    textBefore = false,
                                    onStateChange = { turnedOn ->
                                        // call onToggled() with the current zone. room is null
                                        onToggled(turnedOn, null, zone)
                                        checked = turnedOn
                                        if (turnedOn) { selectedZones.add(zone) }
                                        else { selectedZones.remove(zone) }
                                    }
                                )
                            }
                        }
                    } // LazyColumn

                    HorizontalDivider(modifier = Modifier.fillMaxWidth())

                    OutlinedTextField(
                        value = name,
                        label = { Text(stringResource(R.string.add_flock_name_label)) },
                        onValueChange = { typedText ->
                            name = typedText
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            // Define Next as our IME action (doesn't matter what it was, as long
                            // as it's the same as in the keyboardActions below).
                            imeAction = ImeAction.None,
                            keyboardType = KeyboardType.Text,

                            ),
                    )
                } // Column

            }
        },
        confirmButton = {
            TextButton(onClick = { onOk(name) }) {
                Text(
                    text =
                        if (viewmodel.isCurrentlyEditingFlock()) {
                            stringResource(R.string.edit_flock_confirm_button)
                        }
                        else { stringResource(R.string.add_flock_confirm_button) },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewmodel.cancelAddOrEditFlock() },
            ) { Text(stringResource(R.string.cancel))}
        }
    )

    if (errorMsg.isNotEmpty()) {
        val ctx = LocalContext.current
        Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
        viewmodel.clearFlockErrorMsg()
    }
}

@Suppress("unused")
private const val TAG = "PhilipsHueComposeFlock"