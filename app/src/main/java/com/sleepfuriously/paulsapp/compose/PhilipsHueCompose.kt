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
            bridges.forEach { bridge ->

                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    DrawBridgeSeparator()
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
            numActiveBridges = philipsHueViewmodel.bridgeModel.getAllActiveBridges().size
        )
    }

    // todo: show messages (if any)

}

/**
 * A nice separator between bridges.
 */
@Composable
private fun DrawBridgeSeparator() {
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