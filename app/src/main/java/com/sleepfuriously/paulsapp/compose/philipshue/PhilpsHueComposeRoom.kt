package com.sleepfuriously.paulsapp.compose.philipshue

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.SliderReportWhenFinished
import com.sleepfuriously.paulsapp.compose.isDarkTheme
import com.sleepfuriously.paulsapp.ui.theme.LocalTheme
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.darkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.lightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryLightCoolGray


/**
 * UI for a single room.
 *
 * @param   roomName        The name to display for this room
 *
 * @param   sceneName       The name of the current scene.  Use empty string
 *                          if no scene name should be displayed.
 *
 * @param   illumination    How much is this room currently illumniated.
 *                          0 = off all the way to 1 = full on.
 *
 * @param   lightSwitchOn   Is the switch for the light on (true) or off (false)
 *
 * @param   roomBrightnessChangedFunction   Function to call when the brightness
 *                          is changed by the user.  The parameter will be the
 *                          new illumination value.
 *
 * @param   roomOnOffChangedFunction        Function to call when the user
 *                          switches a room on or off.  The parameter is false
 *                          when the user wants the room off, and it'll be
 *                          true when the user wants to switch it on.
 *
 * @param   showScenesFunction      Function to call when the user wants to
 *                          display all the scenes applicable to this Room.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DisplayPhilipsHueRoom(
    modifier: Modifier = Modifier,
    roomName: String,
    sceneName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    roomBrightnessChangedFunction: (newBrightness: Float) -> Unit,
    roomOnOffChangedFunction: (newOnOff: Boolean) -> Unit,
    showScenesFunction: () -> Unit
) {
    Log.d(TAG, "DisplayPhilipsHueRoom() illumination = $illumination")

    // variables for displaying the lightbulb image
    val lightImage = getProperLightImage(illumination)      // changes while hand is sliding
    val lightImageColor = getLightColor(illumination)

    Log.d(TAG, "DisplayPhilipsHueRoom() lightImage = $lightImage")


    Column(modifier = modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(10.dp))
        .border(
            BorderStroke(2.dp, brush = SolidColor(LocalTheme.current.roomBorder)),
            RoundedCornerShape(12.dp)
        )
        .background(color = LocalTheme.current.roomBackground)

    ) {
        Row {
            Text(
                text = stringResource(R.string.room),
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
            text = AnnotatedString(roomName),
            onClick = {
                showScenesFunction.invoke()
            },
            style = LocalTextStyle.current.copy(
                color = LocalTheme.current.surfaceText
            ),
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
                colors = SwitchDefaults.colors().copy(
                    checkedTrackColor = LocalTheme.current.switchTrackColor,
                ),
                onCheckedChange = { newSliderState ->
                    roomOnOffChangedFunction.invoke(newSliderState)
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
                roomBrightnessChangedFunction.invoke(finalValue)
            },
            enabled = lightSwitchOn,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .height(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@NonSkippableComposable
@Composable
fun DrawLightBulb(imageId : Int, colorTint: Color) {
    Image(
        modifier = Modifier
            .width(110.dp),
        contentScale = ContentScale.Fit,
        painter = painterResource(imageId),
        colorFilter = ColorFilter.tint(colorTint),
        contentDescription = stringResource(id = R.string.lightbulb_content_desc)
    )
}

fun getProperLightImage(illumination: Float) : Int {
    val lightImage =
        if (illumination < 0.05f) {
            R.drawable.bare_bulb_white_04
        }
        else if (illumination < 0.4f) {
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

/**
 * Helper function, but it needs to be a composable to use [LocalTheme].
 */
@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun getLightColor(illumination: Float) : Color {
    // go from darkest to lightest
    if (illumination < 0.05f) {
        return LocalTheme.current.bulbColor4
    }
    if (illumination < 0.4f) {
        return LocalTheme.current.bulbColor3
    }
    if (illumination < 0.75) {
        return LocalTheme.current.bulbColor2
    }
    else
        return LocalTheme.current.bulbColor1
}

//---------------------------
//  constants
//---------------------------

private const val TAG = "PhilipsHueComposeRoom"