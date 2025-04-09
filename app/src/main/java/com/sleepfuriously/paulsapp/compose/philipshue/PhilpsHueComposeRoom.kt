package com.sleepfuriously.paulsapp.compose.philipshue

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.SliderReportWhenFinished
import com.sleepfuriously.paulsapp.ui.theme.coolGray
import com.sleepfuriously.paulsapp.ui.theme.lightCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryDarkCoolGray
import com.sleepfuriously.paulsapp.ui.theme.veryLightCoolGray


/**
 * UI for a single room.
 *
 * @param   roomName        The name to display for this room
 *
 * @param   illumination    How much is this room currently illumniated.
 *                          0 = off all the way to 1 = full on.
 *
 * @param   roomChangeCompleteFunction  Function to call when the illumination is
 *                                  changed and completed by the user.  It takes the new
 *                                  illumination value and a boolean for if the
 *                                  switch is on/off.
 */
@Composable
fun DisplayPhilipsHueRoom(
    modifier: Modifier = Modifier,
    roomName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    roomChangeCompleteFunction: (newIllumination: Float, newSwitchOn: Boolean) -> Unit,
    showScenesFunction: () -> Unit
) {
    // variables for displaying the lightbulb image
    val lightImage = getProperLightImage(illumination)    // changes while hand is sliding
    val lightImageColor = getLightColor(illumination)

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
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(start = 16.dp)
        )

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
                    roomChangeCompleteFunction.invoke(illumination, newSliderState)
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
                roomChangeCompleteFunction.invoke(finalValue, lightSwitchOn)
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

fun getLightColor(illumination: Float) : Color {
    val color =
        if (illumination < 0.05f) {
            coolGray
        }
        else if (illumination < 0.4f) {
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

//---------------------------
//  constants
//---------------------------

private const val TAG = "PhilipsHueComposeRoom"