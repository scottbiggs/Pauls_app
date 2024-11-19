package com.sleepfuriously.paulsapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * This file holds compose functions that are generally useful
 */

//--------------------------
//  public compose functions
//--------------------------

/**
 * @param   msgText        Message to display
 * @param   onClick     Function to run when the button is clicked.
 */
@Composable
fun SimpleBoxMessage(
    modifier: Modifier = Modifier,
    msgText: String = "error",
    onClick: () -> Unit,
    buttonText: String
) {
    Box(modifier = modifier
        .fillMaxSize()
        .padding(80.dp)
        .background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = msgText,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = modifier
                    .wrapContentHeight()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onClick
            ) {
                Text(buttonText)
            }
        }
    }
}

//--------------------------
//  constants
//--------------------------

private const val TAG = "GeneralCompose"