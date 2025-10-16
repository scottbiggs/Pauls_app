package com.sleepfuriously.paulsapp.compose.philipshue

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.OverlappingRow
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueZoneInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel
import com.sleepfuriously.paulsapp.xyToRgbWithBrightness

/*
 * UI for displaying Scenes.
 */

/**
 * Displays a model dialog that holds all the scenes for a given room.
 *
 * @param   room            Room that is showing this list of scenes
 * @param   bridge          Bridge that owns all this stuff
 * @param   viewmodel       Call functions from here when the user wants to make changes
 * @param   onDismiss       Function to call when the user is done with this dialog
 */
@Composable
fun ShowScenesForRoom(
    bridge: PhilipsHueBridgeInfo,
    room: PhilipsHueRoomInfo,
    scenes: List<PHv2Scene>,
    viewmodel: PhilipsHueViewmodel,
    onDismiss: () -> Unit
) {

    AlertDialog(
        modifier = Modifier
            .border(
                BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.secondary)),
                RoundedCornerShape(22.dp)
            ),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.scenes_title, room.name))
        },
        text = {
            SelectionContainer {
                // This is the meat of the function.  All the data goes here.
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    columns = GridCells.Adaptive(MIN_PH_SCENE_WIDTH.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Log.d(TAG, "ShowScenesForRoom() - num scenes = ${scenes.size}")
                    scenes.forEach { scene ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            brush = SolidColor(MaterialTheme.colorScheme.secondaryContainer)
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable {
                                        viewmodel.sceneSelectedForRoom(bridge, room, scene)
                                    }
                            ) {
                                Text(scene.metadata.name)
                                OverlappingRow(overlapFactor = 0.6f) {
                                    // Find all the colors in this scene (don't count repeats, use a Set!)
                                    val sceneColorSet = mutableSetOf<MyColorXYBrightness>()
                                    scene.actions.forEach { action ->
                                        if (action.action.color != null) {
                                            var yY = 1.0
                                            if (action.action.dimming != null) {
                                                yY = action.action.dimming.brightness.toDouble()
                                            }
                                            sceneColorSet.add(MyColorXYBrightness(
                                                x = action.action.color.xy.x.toDouble(),
                                                y = action.action.color.xy.y.toDouble(),
                                                yY = yY
                                            ))
                                        }
                                    }

                                    // Draw a circle for each color in the set
                                    sceneColorSet.forEach { colorXY ->
                                        Log.d(TAG, "scene ${scene.metadata.name}, colorXY = $colorXY")
                                        val rgbTriple = xyToRgbWithBrightness(
                                            x = colorXY.x.toFloat(),
                                            y = colorXY.y.toFloat(),
                                            brightness = colorXY.yY.toInt()
                                        )
                                        val rgb = Color(rgbTriple.first, rgbTriple.second, rgbTriple.third)
                                        Log.d(TAG, "Drawing ColorCircle: rgb = [${rgb.red}, ${rgb.green}, ${rgb.blue}]")
                                        ColorCircle(color = rgb)
                                    }
                                    if (sceneColorSet.isEmpty()) {
                                        Text(
                                            stringResource(R.string.no_colored_lights_in_scene),
                                            modifier = Modifier
                                                .height(DEFAULT_SIZE_FOR_COLOR_CIRCLE.dp)
                                                .wrapContentSize()  // centers text vertically
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) { Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { }
    )
}

/**
 * Like [ShowScenesForRoom] but for zones instead.
 */
@Composable
fun ShowScenesForZone(
    bridge: PhilipsHueBridgeInfo,
    zone: PhilipsHueZoneInfo,
    scenes: List<PHv2Scene>,
    viewmodel: PhilipsHueViewmodel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier
            .border(
                BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.secondary)),
                RoundedCornerShape(22.dp)
            ),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.scenes_title, zone.name))
        },
        text = {
            SelectionContainer {
                // This is the meat of the function.  All the data goes here.
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    columns = GridCells.Adaptive(MIN_PH_SCENE_WIDTH.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Log.d(TAG, "ShowScenesForRoom() - num scenes = ${scenes.size}")
                    scenes.forEach { scene ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            brush = SolidColor(MaterialTheme.colorScheme.secondaryContainer)
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable {
                                        viewmodel.sceneSelectedForZone(bridge = bridge, zone = zone, scene = scene)
                                    }
                            ) {
                                Text(scene.metadata.name)
                                OverlappingRow(overlapFactor = 0.6f) {
                                    // Find all the colors in this scene (don't count repeats, use a Set!)
                                    val sceneColorSet = mutableSetOf<MyColorXYBrightness>()
                                    scene.actions.forEach { action ->
                                        if (action.action.color != null) {
                                            var yY = 1.0
                                            if (action.action.dimming != null) {
                                                yY = action.action.dimming.brightness.toDouble()
                                            }
                                            sceneColorSet.add(MyColorXYBrightness(
                                                x = action.action.color.xy.x.toDouble(),
                                                y = action.action.color.xy.y.toDouble(),
                                                yY = yY
                                            ))
                                        }
                                    }

                                    // Draw a circle for each color in the set
                                    sceneColorSet.forEach { colorXY ->
                                        Log.d(TAG, "scene ${scene.metadata.name}, colorXY = $colorXY")
                                        val rgbTriple = xyToRgbWithBrightness(
                                            x = colorXY.x.toFloat(),
                                            y = colorXY.y.toFloat(),
                                            brightness = colorXY.yY.toInt()
                                        )
                                        val rgb = Color(rgbTriple.first, rgbTriple.second, rgbTriple.third)
                                        Log.d(TAG, "Drawing ColorCircle: rgb = [${rgb.red}, ${rgb.green}, ${rgb.blue}]")
                                        ColorCircle(color = rgb)
                                    }
                                    if (sceneColorSet.isEmpty()) {
                                        Text(
                                            stringResource(R.string.no_colored_lights_in_scene),
                                            modifier = Modifier
                                                .height(DEFAULT_SIZE_FOR_COLOR_CIRCLE.dp)
                                                .wrapContentSize()  // centers text vertically
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) { Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = { }
    )
}

/**
 * Draws a circle of the given color and size.  If the color is very dark,
 * a white outline is drawn around it.
 *
 * @param   color       The color to draw this circle
 * @param   size        Diameter of this circle in dp.
 */
@Composable
fun ColorCircle(
    color: Color,
    size: Int = DEFAULT_SIZE_FOR_COLOR_CIRCLE,
) {
    // Create a border color.  It'll be black unless the color to show is
    // pretty dark.  In that case use white for the border.
    var borderColor = Color.Black
    if (color.luminance() < 0.04) {
        borderColor = Color.White
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .border(1.dp, borderColor, CircleShape)
            .padding(1.dp)
            .clip(CircleShape)
            .background(color)
    )
}


//----------------------------
//  classes
//----------------------------

data class MyColorXYBrightness(
    val x: Double,
    val y: Double,
    /** This is the Capital Y (I use yY to distinguish it). I believe Philips Hue multiplies this by 100 */
    val yY: Double
)

//----------------------------
//  constants
//----------------------------

private const val TAG = "PhilipsHueComposeScene"

private const val MIN_PH_SCENE_WIDTH = 130

private const val DEFAULT_SIZE_FOR_COLOR_CIRCLE = 32