package com.sleepfuriously.paulsapp.compose

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.model.philipshue.PhilipsHueRoomInfo
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2Scene
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel

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
    Log.d(TAG, "ShowScenesForRoom(): num scenes = ${scenes.size}")
    AlertDialog(
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
                        .padding(horizontal = 4.dp),
                    columns = GridCells.Adaptive(MIN_PH_SCENE_WIDTH.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Log.d(TAG, "ShowScenesForRoom() - num scenes = ${scenes.size}")
                    scenes.forEach { scene ->
                        item {
                            Column {
                                Text(scene.metadata.name)
                                Button(
                                    onClick = {
                                        viewmodel.sceneSelected(bridge, room, scene)
                                    }
                                ) {
                                    Text("select")
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


private const val TAG = "PhilipsHueComposeScene"

private const val MIN_PH_SCENE_WIDTH = 130
