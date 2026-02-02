package com.sleepfuriously.paulsapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sleepfuriously.paulsapp.ui.theme.coolGray

private val DarkColorScheme = darkColorScheme(
    primary = lightBlueMain,
    onPrimary = almostBlack,
    primaryContainer = darkBlueDark,
    onPrimaryContainer = veryLightCoolGray,

    inversePrimary = darkBlueMain,

    secondary = yellowMain,
    onSecondary = almostBlack,
    secondaryContainer = yellowLight,
    onSecondaryContainer = almostBlack,

    tertiary = brownMed,
    onTertiary = veryDarkCoolGray,
    tertiaryContainer = brownDark,
    onTertiaryContainer = veryLightCoolGray,

    /** this is a medium gray */
    outline = coolGray,
    /** a lighter (or MORE of the scheme) gray */
    outlineVariant = lightCoolGray,

    error = lightMyRed
//    background = almostBlack,
//    onBackground = veryLightCoolGray,
//
//    surface = almostBlackLighter,
//    onSurface = veryLightCoolGray,
)

private val LightColorScheme = lightColorScheme(
    primary = lightBlueVeryDark,
    onPrimary = veryLightCoolGray,
    primaryContainer = darkBlueDark,
    onPrimaryContainer = veryLightCoolGray,

    inversePrimary = lightBlueLight,

    secondary = palmGreen,
    onSecondary = almostBlack,
    secondaryContainer = palmGreenVeryVeryLight,
    onSecondaryContainer = almostBlack,

    tertiary = brownDark,
    onTertiary = veryLightCoolGray,
    tertiaryContainer = brownLight,
    onTertiaryContainer = almostBlack,

    /** this is a medium gray */
    outline = lightCoolGray,
    /** a lighter (or MORE of the scheme) gray */
    outlineVariant = coolGray

//    background = veryLightCoolGray,
//    onBackground = almostBlack,
//
//    surface = lightCoolGray,
//    onSurface = almostBlack,
)

private val LightColorSchemeCyan = lightColorScheme(
    primary = cyanDark,
    primaryContainer = cyanDark,

    inversePrimary = cyanLight,

    secondary = violet,
    onSecondary = onViolet,
    secondaryContainer = lightViolet,
    onSecondaryContainer = almostBlack,

    tertiary = marineGreen,
    onTertiary = onMarineGreen,
    tertiaryContainer = lightMarineGreen,
    onTertiaryContainer = almostBlack,

    /** this is a medium gray */
    outline = lightWarmGray,
    /** a lighter (or MORE of the scheme) gray */
    outlineVariant = warmGray
)

@Composable
fun PaulsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Dynamic color is available on Android 12+
     *
     * NOTE
     *  If this is true, then the defined colors above for dark and light mode
     *  are mostly ignored.  Instead the dynamic colors are used (mostly).
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && supportsDynamicColorTheme() -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme

//        else -> LightColorScheme
        else -> LightColorSchemeCyan
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


fun supportsDynamicColorTheme() : Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S