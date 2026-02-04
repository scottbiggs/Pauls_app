package com.sleepfuriously.paulsapp.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.MyApplication
import com.sleepfuriously.paulsapp.R
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.LayoutDirection
import com.sleepfuriously.paulsapp.compose.philipshue.MAX_BRIGHTNESS
import com.sleepfuriously.paulsapp.model.philipshue.json.PHv2LightColorGamut
import com.sleepfuriously.paulsapp.ui.theme.LocalTheme

/**
 * This file holds compose functions that are generally useful
 */

//--------------------------
//  public compose functions
//--------------------------

/**
 * Displays a message in a box that pretty much takes most of the screen.
 * I plan to use this instead of a dialog.
 *
 * @param   msgText        Message to display
 * @param   onClick     Function to run when the button is clicked.
 */
@Composable
fun SimpleFullScreenBoxMessage(
    @SuppressLint("ModifierParameter")
    backgroundModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    msgText: String = "error",
    onClick: () -> Unit,
    buttonText: String
) {
    Box(modifier = backgroundModifier
        .fillMaxSize()
        .padding(80.dp)
        .background(LocalTheme.current.surfaceColored),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                textAlign = TextAlign.Center,
                text = msgText,
                color = LocalTheme.current.surfaceColoredText,
                modifier = textModifier
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
 *                          Note that you may need to prefix your function with double
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
            // Define Next as our IME action (doesn't matter what it was, as long
            // as it's the same as in the keyboardActions below).
            imeAction = if (imeActivate) ImeAction.Next else ImeAction.None,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(
            onNext = { onClick.invoke(textFieldText) }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = buttonLabel)
                }
            }
        },
    )
}

/**
 * Use this to show a confirm/dismiss dialog.
 *
 * @param   onDismiss       Function to call when this dialog is dismissed by any way
 *
 * @param   onConfirm       Function to call when confirmed
 *
 * @param   titleText       Text for the title.
 *
 * @param   bodyText        Text for the body of the message.
 *
 * @param   confirmText     Text to put in the confirmation button.  Default is "confirm".
 *
 * @param   dismissText     Text to put in the dismiss button.  Default is "dismiss".
 */
@Composable
fun MyYesNoDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    titleText: String,
    bodyText: String,
    dismissText: String = stringResource(R.string.dismiss),
    confirmText: String = stringResource(R.string.confirm),
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(titleText)
        },
        text = { Text(bodyText) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) { Text(confirmText, color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text(dismissText, color = MaterialTheme.colorScheme.primary) }
        }
    )
}

/**
 * This is a slider that is able to respond to outside changes in its value,
 * yet only report changes once the slide is complete (user's finger leaves
 * the slider).
 *
 * @param   sliderInputValue        The value that the slider should display
 *                                  while not in the middle of a slide.  As
 *                                  this changes this function will recompose,
 *                                  updating the display appropriately.
 *
 * @param   setSliderValueFunction  Function to call when a slide is complete.
 *                                  The value of the completed slide  in the
 *                                  range [0..1] is input to the function.
 *
 * @param   enabled                 Same as [Slider].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderReportWhenFinished(
    sliderInputValue: Float,
    setSliderValueFunction: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val slidingPosition = remember { mutableFloatStateOf(0f) }
    val sliding = remember { mutableStateOf(false) }

    Slider(
        value = if (sliding.value) slidingPosition.floatValue else sliderInputValue,
        enabled = enabled,
        colors = SliderDefaults.colors().copy(
            thumbColor = MaterialTheme.colorScheme.secondary,
            activeTrackColor = MaterialTheme.colorScheme.secondary,     // Before the mark
            inactiveTrackColor = MaterialTheme.colorScheme.outline,     // AFTER (to the right) of the mark
//            activeTickColor = MaterialTheme.colorScheme.primary       // not used as I'm not using tick marks
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        shape = RoundedCornerShape(18.dp),
                        color = if (enabled)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.outlineVariant
                    )
                    .size(18.dp)
                    .background(
                        color = if (enabled)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(18.dp)
                    )
            )
        },
        onValueChange = {
            slidingPosition.floatValue = it
            sliding.value = true
        },
        onValueChangeFinished = {
            Log.v(TAG, "Slider finished. value = ${slidingPosition.floatValue}")
            setSliderValueFunction(slidingPosition.floatValue)
            sliding.value = false
        },
        modifier = modifier
            //
            // this stuff makes the rounded outline
            //
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(10.dp)
            )
//            .background(MaterialTheme.colorScheme.onTertiary)
            .height(40.dp)
            .padding(top = 2.dp, start = 2.dp)  // centers the thumb within the slider area
    )
}

/**
 * Measuring policy for making overlapping rows.
 *  from:  https://proandroiddev.com/custom-layouts-with-jetpack-compose-bc1bdf10f5fd
 *
 *  overlappingFactor decides how much of each child composable will be
 *  visible before next one overlaps it 1.0 means completely visible,
 *  0.7 means 70%, 0.5 means 50% visible and so on..
 */
fun overlappingRowMeasurePolicy(overlapFactor: Float) = MeasurePolicy { measurables, constraints ->
    val placeables = measurables.map { measurable ->
        measurable.measure(constraints)
    }

    // height is the tallest of all the placeables
    val height = placeables.maxOf { it.height }
    // width is all the placeables within their overlap
    val width = (placeables.subList(1, placeables.size).sumOf { it.width } * overlapFactor + placeables[0].width).toInt()

    layout(width, height) {
        // Placement block with placement logic. Analogous to onLayout() in
        // the view world.
        var xPos = 0
        for (placeable in placeables) {
            placeable.placeRelative(xPos, 0, 0f)
            xPos += (placeable.width * overlapFactor).toInt()
        }
    }
}

/**
 * Row that will actually overlap the items.  Taken from:
 *      https://proandroiddev.com/custom-layouts-with-jetpack-compose-bc1bdf10f5fd
 *
 * Uses [overlappingRowMeasurePolicy] to get the measuring right as original measuring
 * does everything possible to avoid overlapping.
 */
@Composable
fun OverlappingRow(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.1, to = 1.0) overlapFactor: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val measurePolicy = overlappingRowMeasurePolicy(overlapFactor)
    Layout(
        measurePolicy = measurePolicy,
        content = content,
        modifier = modifier
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
fun DrawInfoDialogLine(
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

/**
 * Draws a switch with a label attached.
 *
 * @param   label       The text to go with the switch
 *
 * @param   state       The current (start) state of this switch.
 *
 * @param   textBefore  True - the text should be before the switch.
 *                      False - text appears after the switch.
 *
 * @param   onStateChange   Function to run whenever the user changes
 *                          the switch.
 *
 * from:  https://stackoverflow.com/a/73076422/624814
 */
@Composable
fun SwitchWithLabel(
    label: String,
    state: Boolean,
    textBefore: Boolean = true,
    onStateChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                // This is for removing ripple when Row is clicked
                indication = null,
                role = Role.Switch,
                onClick = {
                    onStateChange(!state)
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {

        if (textBefore) {
            // draw the text to the left of the switch
            Text(text = label)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Switch(
                checked = state,
                colors = SwitchDefaults.colors().copy(checkedTrackColor = MaterialTheme.colorScheme.secondary),
                onCheckedChange = {
                    onStateChange(it)
                }
            )
        }
        else {
            // draw text after (to the right) of the switch
            Switch(
                checked = state,
                colors = SwitchDefaults.colors().copy(checkedTrackColor = MaterialTheme.colorScheme.secondary),
                onCheckedChange = {
                    onStateChange(it)
                }
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(text = label)
        }
    }
}

/**
 * Converts the Int version of brightness (ranging from 0 to [MAX_BRIGHTNESS])
 * to the float version (from 0.0 to 1.0).
 */
fun convertBrightnessIntToFloat(intBrightness: Int) : Float {
    return intBrightness.toFloat() / MAX_BRIGHTNESS.toFloat()
}

/**
 * Converts a brightness from the Float version which ranges from [0.0 .. 1.0]
 * to the Int version, ranging from [0 .. MAX_BRIGHTNESS].  Currently
 * [MAX_BRIGHTNESS] is 100.
 */
fun convertBrightnessFloatToInt(floatBrightness: Float) : Int {
    return (floatBrightness * MAX_BRIGHTNESS.toFloat()).toInt()
}

/**
 * Returns TRUE iff we are currently in night mode.  False for all other modes.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun isDarkTheme(): Boolean {
    val nightMode = MyApplication.appContext.resources.configuration.colorMode
        .and(Configuration.UI_MODE_NIGHT_MASK)
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}

//--------------------------
//  constants
//--------------------------

/** x component of CIE White in D65 */
const val CIE_WHITE_X = 0.31271
/** y component of CIE White in D65 */
const val CIE_WHITE_Y = 0.32902
/** Capital Y (called YY) component of CIE White in D65 */
const val CIE_WHITE_YY = 1.0

/** x component of CIE Red in D65 */
const val CIE_RED_X = 0.64
/** y component of CIE Red in D65 */
const val CIE_RED_Y = 0.33
/** Capital Y (called YY) component of CIE Red in D65 */
const val CIE_RED_YY = 0.2126

/** x component of CIE Green in D65 */
const val CIE_GREEN_X = 0.3
/** y component of CIE White in D65 */
const val CIE_GREEN_Y = 0.6
/** Capital Y (called YY) component of CIE White in D65 */
const val CIE_GREEN_YY = 0.0722

/** x component of CIE Blue in D65 */
const val CIE_BLUE_X = 0.15
/** y component of CIE Blue in D65 */
const val CIE_BLUE_Y = 0.06
/** Capital Y (called YY) component of CIE Blue in D65 */
const val CIE_BLUE_YY = 0.0722

/** defines gamut triangle for the monitor */
val monitorGamut =
    PHv2LightColorGamut(
        red = Pair(0.68, 0.32),
        green = Pair(0.265, 0.69),
        blue = Pair(0.15, 0.06)
    )



private const val TAG = "GeneralCompose"