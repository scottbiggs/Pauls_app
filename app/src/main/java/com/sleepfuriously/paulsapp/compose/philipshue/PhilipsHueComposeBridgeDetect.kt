package com.sleepfuriously.paulsapp.compose.philipshue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.model.philipshue.data.PhilipsHueBridgeInfo
import com.sleepfuriously.paulsapp.viewmodels.PhilipsHueViewmodel

/**
 * Call this function while waitinf for the auto-bridge detect functionality
 * is running.  It'll display some sort of spinner
 */
@Composable
fun DetectingBridgesSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("waiting for detection to complete -- fix me!")
    }
}

/**
 * Shows the user the discovered bridges, their current bridges, and lets the
 * user figure out what to do with them.
 *
 * @param   knownBridges        List of the known bridges (known to this app)
 *
 * @param   discoveredBridges   The bridges that were recently discovered on
 *                              this network.  May include bridges from the
 *                              knownBridges param.
 */
@Composable
fun DiscoveredBridgeSelector(
    modifier : Modifier = Modifier,
    knownBridges: List<PhilipsHueBridgeInfo>,
    discoveredBridges: List<String>,
    viewmodel: PhilipsHueViewmodel
) {

    var bridgesToAdd by remember { mutableStateOf<List<String>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                style = MaterialTheme.typography.headlineLarge,
                text = stringResource(R.string.bridge_auto_detect_title),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            //
            // no bridges found through discovery
            //
            if (discoveredBridges.isEmpty()) {
                Text(
                    style = MaterialTheme.typography.headlineLarge,
                    text = stringResource(R.string.bridge_auto_detect_none_found),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            } // discovered bridges

            //
            // Show the discovered bridges and known bridges
            //
            else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //
                    // discovered bridges
                    //
                    Column {
                        discoveredBridges.forEach { discoveredBridge ->
                            Text("ip = $discoveredBridge", modifier.padding(top = 10.dp))
                        }
                    }

                    //
                    // known bridges
                    //
                    Column {
                        knownBridges.forEach { knownBridge ->
                            Text(
                                "known = ${knownBridge.humanName}, ip = ${knownBridge.ipAddress}",
                                modifier.padding(top = 10.dp)
                            )
                        }
                    }
                } // known bridges

            } // else (show discovered bridges and known bridges)


            //
            // action buttons
            //

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //
                // cancel
                //
                Button(
                    onClick = { viewmodel.cancelDetectBridges() }
                ) {
                    Text(stringResource(R.string.cancel))
                }

                //
                // confirm
                //
                Button(
                    onClick = { viewmodel.addDetectedBridges(bridgesToAdd) }
                ) {

                }
            }

        } // Column
    } // Box

}