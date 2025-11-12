package com.sleepfuriously.paulsapp.compose.philipshue

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.DrawInfoDialogLine
import com.sleepfuriously.paulsapp.compose.MyYesNoDialog
import com.sleepfuriously.paulsapp.compose.SliderReportWhenFinished
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueFlockInfo
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel


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
                text = { Text(stringResource(R.string.add_flock)) },
                onClick = {
                    isDropDownExpanded = false
                    viewmodel.addFlock()
                },
            )

            // delete a flock
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_flock)) },
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
                    viewmodel.deleteFlock()    // this spurns a new coroutine and returns immediately
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
