package com.sleepfuriously.paulsapp.compose.philipshue

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.SliderReportWhenFinished
import com.sleepfuriously.paulsapp.compose.isDarkTheme


/**
 * UI for a single zone (similar to [DisplayPhilipsHueRoom]).
 *
 * @param   zoneName        The name to display for this zone
 *
 * @param   illumination    How much is this zone currently illumniated.
 *                          0 = off all the way to 1 = full on.
 *
 * @param   zoneBrightnessChangedFunction  Function to call when the brightness is
 *                                  changed and completed by the user.  It takes the new
 *                                  illumination value [0..1].
 *
 * @param   zoneOnOffChangedFunction    Function to call when the
 *                                  zone is turned on or off.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DisplayPhilipsHueZone(
    modifier: Modifier = Modifier,
    zoneName: String,
    sceneName: String,
    illumination: Float,
    lightSwitchOn: Boolean,
    zoneBrightnessChangedFunction: (newIllumination: Float) -> Unit,
    zoneOnOffChangedFunction: (newOnOffStatus: Boolean) -> Unit,
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
            BorderStroke(2.dp, brush = SolidColor(MaterialTheme.colorScheme.onTertiaryContainer)),
            RoundedCornerShape(12.dp)
        )

    ) {
        Row {
            Text(
                text = stringResource(R.string.zone),
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
            text = AnnotatedString(zoneName),
            onClick = {
                showScenesFunction.invoke()
            },
            style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .padding(start = 16.dp)
        )


        // display the name of the current scene
        if (sceneName.isNotEmpty()) {
            Text(sceneName, modifier = Modifier.fillMaxWidth().padding(end = 8.dp), textAlign = TextAlign.End)
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
                    checkedTrackColor = MaterialTheme.colorScheme.secondary
                ),
                onCheckedChange = { newSliderState ->
                    zoneOnOffChangedFunction.invoke(newSliderState)
                }
            )

            // This pushes the switch to the far left and the lightbulb to
            // the far right.
            Spacer(modifier = Modifier.weight(1f))

            DrawLightBulb(lightImage, lightImageColor)
        }

        SliderReportWhenFinished(
            sliderInputValue = illumination,
            setSliderValueFunction = { sliderFinalValue ->
                zoneBrightnessChangedFunction.invoke(sliderFinalValue)
            },
            enabled = lightSwitchOn,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .height(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
