package com.sleepfuriously.paulsapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

//
// Note:
//  Colors are defined as a 32-bit ARGB
//

//---------------------------------
//  raw colors
//---------------------------------

val yellowMain = Color(0xffd7ff30)
val yellowDark = Color(0xFF93AE26)
val yellowLight = Color(0xFFE9FE92)
val yellowVeryLight = Color(0xFFFAFEEB)

val lightBlueMain = Color(0xff08a2ff)
val lightBlueLight = Color(0xFF5DC2FF)
val lightBlueDark = Color(0xFF086399)
val lightBlueVeryDark = Color(0xFF08476E)

val darkBlueMain = Color(0xff00205c)
val darkBlueLight = Color(0xFF274173)
val darkBlueDark = Color(0xFF00102F)

val cyan = Color(0xff00ffff)
val cyanLight = Color(0xFFA2F9F9)
val cyanVeryLight = Color(0xFFC6FCFC)
val cyanDark = Color(0xFF00AEAE)
val cyanVeryDark = Color(0xFF004040)

val brownVeryLight = Color(0xFFF2A88D)
val brownLight = Color(0xFFAE7764)
val brownMed = Color(0xFF7F574A)
val brownDark = Color(0xFF4E362F)

val palmGreen = Color(0xFF387138)
val palmGreenLight = Color(0xFF5DAB5D)
val palmGreenVeryLight = Color(0xFF82F082)
val palmGreenVeryVeryLight = Color(0xFFD3F8D3)
val palmGreenDark = Color(0xFF1D441D)
val palmGreenVeryDark = Color(0xFF0E1F0E)

val lightCoolGray = Color(0xFF929DBF)
val veryLightCoolGray = Color(0xFFD2DDFF)
/** This is medium gray that's find to use on its own. */
val coolGray = Color(0xFF5A637D)
val darkCoolGray = Color(0xFF40485F)
val veryDarkCoolGray = Color(0xFF2A3040)

val warmGray = Color(0xFF7C7979)
val lightWarmGray = Color(0xFFB2AEAE)
val veryLightWarmGray = Color(0xFFE5E1E1)
val darkWarmGray = Color(0xFF514E4E)
val veryDarkWarmGray = Color(0xFF322F2F)

val almostBlack = Color(0xFF000817)
val almostBlackLighter = Color(0xFF12151B)
val almostBlackMuchLighter = Color(0xFF1A1E26)

val myRed = Color(0xFF8D1E1E)
val darkMyRed = Color(0xFF661515)
val lightMyRed = Color(0xFFE64343)

val violet = Color(0xffc64191)
val lightViolet = Color(0xFFBE6A9A)
val darkViolet = Color(0xFF752758)
val onViolet = Color(0xFFF0F0EC)

val marineGreen = Color(0xff1a5e63)
val lightMarineGreen = Color(0xFF629BA4)
val darkMarineGreen = Color(0xFF0F3839)
val onMarineGreen = Color(0xFFE0E4E6)

/*

//---------------------------------
//  themes
//---------------------------------

data class MyColorTheme(

    //
    // backgrounds
    //

    /** primary background */
    val surface: Color,
    /** lighter version of [surface] (darker for darkmode) */
    val surfaceMore: Color,
    /** Less light or dark than a regular surface */
    val surfaceLess: Color,
    /** Inverted colored surface. It'll be light for dark mode & vice-versa. */
    val surfaceInverse: Color,

    //
    // foregrounds
    //

    /** text color on [surface] */
    val surfaceText: Color,
    /** text color variant for [surface] */
    val surfaceText2: Color,

    /** text color for [surfaceInverse] */
    val surfaceInverseText: Color,
    /** text color variant for [surfaceInverse] */
    val surfaceInverseText2: Color,

    //
    //  icons
    //

    /** primary color for icons */
    val icon1: Color,
    /** secondary color for icons */
    val icon2: Color,
    /** tertiary color for icons */
    val icon3: Color,
    /** inverse color for icon1 */
    val iconInverse1: Color,
    val iconInverse2: Color,
    val iconInverse3: Color,
    /** color for icons that are disabled */
    val iconDisabled: Color,

    //
    //  borders
    //
    val borderPrimary: Color,
    val borderError: Color,

    //
    //  buttons
    //
    /** background of button */
    val buttonEnabled: Color,
    /** button text color */
    val buttonText: Color,
    /** button icon color */
    val buttonIcon: Color,

    val buttonDisabled: Color,
    val buttonTextDisabled: Color,

    //
    //  FAB buttons
    //
    val fabButtonEnabled: Color,
    val fabButtonText: Color,
    val fabButtonIcon: Color,
    val fabButtonDisabled: Color,
    val fabButtonTextDisabled: Color,

    //
    //  misc
    //

    /** When TRUE, this color scheme is a night-time color scheme */
    val isDarkMode: Boolean
    )

val defaultLightColorScheme = MyColorTheme(
    surface = veryLightWarmGray,
    surfaceMore = Color.White,
    surfaceLess = lightWarmGray,
    surfaceInverse = darkWarmGray,
    surfaceText = almostBlack,
    surfaceText2 = cyanDark,
    surfaceInverseText = veryLightWarmGray,
    surfaceInverseText2 = cyanVeryLight,

    icon1 = darkWarmGray,
    icon2 = cyanDark,
    icon3 = darkViolet,
    iconInverse1 = lightWarmGray,
    iconInverse2 = cyanLight,
    iconInverse3 = lightViolet,
    iconDisabled = warmGray,

    borderPrimary = cyanDark,
    borderError = myRed,

    buttonEnabled = cyanDark,
    buttonText = veryLightWarmGray,
    buttonIcon = veryLightWarmGray,
    buttonDisabled = warmGray,
    buttonTextDisabled = veryDarkWarmGray,

    fabButtonEnabled = cyanDark,
    fabButtonText = veryLightWarmGray,
    fabButtonIcon = veryLightWarmGray,
    fabButtonDisabled = warmGray,
    fabButtonTextDisabled = veryDarkWarmGray,

    isDarkMode = false
)

val defaultDarkColorScheme = MyColorTheme(
    surface = veryDarkCoolGray,
    surfaceMore = Color.Black,
    surfaceLess = darkCoolGray,
    surfaceInverse = lightCoolGray,
    surfaceText = veryLightCoolGray,
    surfaceText2 = lightBlueLight,
    surfaceInverseText = veryDarkCoolGray,
    surfaceInverseText2 = yellowLight,

    icon1 = lightCoolGray,
    icon2 = lightBlueMain,
    icon3 = yellowMain,
    iconInverse1 = darkCoolGray,
    iconInverse2 = lightBlueDark,
    iconInverse3 = yellowDark,
    iconDisabled = coolGray,

    borderPrimary = lightBlueLight,
    borderError = lightMyRed,

    buttonEnabled = lightBlueMain,
    buttonText = almostBlack,
    buttonIcon = almostBlack,
    buttonDisabled = coolGray,
    buttonTextDisabled = veryLightCoolGray,

    fabButtonEnabled = lightBlueMain,
    fabButtonText = almostBlack,
    fabButtonIcon = almostBlack,
    fabButtonDisabled = coolGray,
    fabButtonTextDisabled = veryLightCoolGray,

    isDarkMode = true
)

val LocalTheme = staticCompositionLocalOf<MyColorTheme> {
    error("no colors provided")
}

 */