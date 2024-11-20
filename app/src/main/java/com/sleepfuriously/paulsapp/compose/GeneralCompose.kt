package com.sleepfuriously.paulsapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

/**
 * This displays a TextField with a button that the user can click.
 * Pressing enter also activates that button (if [imeActivate] is true).
 *
 * And wait, that's not all!  You also get a button that clears the
 * text, just for ordering now!
 *
 * @param   modifier        Regular thing for how to draw this
 *
 * @param   defaultText     Text that will initially show when first drawn.
 *                          Defaults to "".
 *
 * @param   label           Label to put on this TextField. Defaults to "".
 *
 * @param   buttonLabel     Content description of the button (for blind people).
 *
 * @param   singleLine      Defaults to true (just one line)
 *
 * @param   keyboardType    What kind of keyboard to show.  Defaults to KeyboardType.Text,
 *                          the standard keyboard.
 *
 * @param   imeActivate     When True, the user will also cause the [onClick] function
 *                          to fire when they hit the enter key. Defaults = false.
 *
 * @param   onClick         Function to call when the button is clicked.  This function
 *                          takes the current value of the TextField's string as input.
 *                          Note that you need to prefix your function with double
 *                          colons (eg:  ::myFun) to make it recognizable to this param.
 */
@Composable
fun TextFieldAndButton(
    modifier: Modifier = Modifier,
    defaultText: String = "",
    label: String = "",
    buttonLabel: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeActivate: Boolean = false,
    onClick: (String) -> Unit
) {

    var textFieldText by remember { mutableStateOf(defaultText) }

    OutlinedTextField(
        modifier = modifier,
        value = textFieldText,
        label = { Text(label) },
        onValueChange = { textFieldText = it },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(
            // Define Done as our IME action (doesn't matter what it was, as long
            // as it's the same as in the keyboardActions below).
            imeAction = if (imeActivate) ImeAction.Done else ImeAction.None,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onDone = { onClick.invoke(textFieldText) }
        ),
        trailingIcon = {
            Row {
                IconButton(
                    onClick = {
                        textFieldText = ""
                    },
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = "clear")
                }

                FilledIconButton (onClick = { onClick.invoke(textFieldText) }) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = buttonLabel)
                }
            }
        },
    )
}

//--------------------------
//  constants
//--------------------------

private const val TAG = "GeneralCompose"