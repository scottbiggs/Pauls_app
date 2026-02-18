package com.sleepfuriously.paulsapp.ui.theme

import androidx.compose.foundation.text.selection.TextSelectionColors
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

val black = Color(0xff000000)
val almostBlack = Color(0xFF000817)
val almostBlackLighter = Color(0xFF12151B)
val almostBlackMuchLighter = Color(0xFF1A1E26)

val myRed = Color(0xFF8D1E1E)
val darkMyRed = Color(0xFF661515)
val lightMyRed = Color(0xFFE64343)

val violet = Color(0xffc64191)
val lightViolet = Color(0xFFBB7298)
val darkViolet = Color(0xFF752758)
val onViolet = Color(0xFFF0F0EC)

val marineGreen = Color(0xff1a5e63)
val lightMarineGreen = Color(0xFF629BA4)
val darkMarineGreen = Color(0xFF0F3839)
val onMarineGreen = Color(0xFFE0E4E6)

//
//  Cowboys Colors (yeeha!)
//
/** a very bright blue, suitable for highlights and emphasis on text */
val royalBlue = Color(0xff003594)
/** lighter variant of [royalBlue] */
val royalBlueLight = Color(0xFF0543B9)
/** nice neutral gray, good for large swaths and backgrounds */
val silver = Color(0xff869397)
/** lighter version of [silver] */
val silverLight = Color(0xFFEEEFF0)
/** a darker blue, good for special backgrounds when an inverse is wanted */
val helmetBlue = Color(0xff041E42)
/** Same as [helmetBlue] but not so black */
val helmetBlueLight = Color(0xFF213B59)
/** greenish gray. use for a special color when there is not much blue around */
val pantsGray = Color(0xff7F9695)
val pantsGrayLight = Color(0xFFE8F5F5)
/** pure white, default for backgrounds */
val dallasWhite = Color(0xffffffff)

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
    val surfaceMore: Color = surface,
    /** Less light or dark than a regular surface */
    val surfaceLess: Color = surface,
    /** Inverted colored surface. It'll be light for dark mode & vice-versa. */
    val surfaceInverse: Color,
    /** Surface that has a strong color */
    val surfaceColored: Color,

    val roomBackground: Color,
    val roomBorder: Color,
    val zoneBackground: Color,
    val zoneBorder: Color,
    val flockBackground: Color,
    val flockBorder: Color,
    val scenesBorder: Color,
    val scenesBackground: Color,

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
    val surfaceInverseText2: Color = surfaceInverseText,

    /** Text color for the colored surface */
    val surfaceColoredText: Color,

    /** Color to represent any positive text */
    val positiveTextColor: Color,
    /** Represents negative text */
    val negativeTextColor: Color,

    /** Text to catch the attention of error messages */
    val errorTextColor: Color,

    //
    //  gradients
    //
    /** This gradient color is close to the background [surface]] */
    val gradientSmall: Color,
    /** This gradient color contrasts greatly with the background */
    val gradientLarge: Color,

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
    //  light bulbs
    //
    /** color of the light bulb icon in a room, zone, or flock */
    val bulbColor1: Color,
    val bulbColor2: Color = bulbColor1,
    val bulbColor3: Color = bulbColor1,
    val bulbColor4: Color = bulbColor1,

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
    val buttonIcon: Color = buttonText,

    val buttonDisabled: Color,
    val buttonTextDisabled: Color,

    val radioButtonSelected: Color,
    val radioButtonUnselected: Color,
    val radioButtonDisabled: Color,

    //
    //  FAB buttons
    //
    val fabButtonEnabled: Color,
    val fabButtonText: Color,
    val fabButtonIcon: Color = fabButtonText,
    val fabButtonDisabled: Color,
    val fabButtonTextDisabled: Color,

    //
    // TextFields (just some of 'em)
    //
    val textFieldFocusedTextColor: Color,
    val textFieldUnfocusedTextColor: Color,
    val textFieldDisabledTextColor: Color,
    val textFieldFocusedContainerColor: Color,
    val textFieldUnfocusedContainerColor: Color,
    val textFieldDisabledContainerColor: Color,
    val textFieldCursorColor: Color,
    val textFieldSelectionColors: TextSelectionColors = TextSelectionColors(
        handleColor = borderPrimary,
        backgroundColor = fabButtonEnabled
    ),

    //
    //  progress indicators
    //
    val progressIndicatorColor: Color,
    val progressIndicatorBackground: Color = buttonEnabled,
    val progressIndicatorText: Color = buttonText,

    //
    // switch
    //
    //  I'm not doing the thumb colors
    //
    val switchTrackColor: Color,
    val switchDisabledTrackColor: Color,

    //
    // slider
    //
    val sliderThumbColor: Color,
    /** The part to the left of the thumb */
    val sliderActiveTrackColor: Color,
    /** part to the right of the thumb */
    val sliderInactiveTrackColor: Color,
    /** slider part (left and right of the thumb) for inactive sliders */
    val sliderDisabledTrackColor: Color,
    /** color the for border of the slider when it is enabled */
    /** slider background color when enabled */
    val sliderBackgroundEnabled: Color,
    /** slider background color when disabled */
    val sliderBackgroundDisabled: Color,
    /** the color of the border around the slider widget when enabled */
    val sliderBorderEnabled: Color,
    /** border color for disabled slider */
    val sliderBorderDisabled: Color,

    //
    //  misc
    //

    /** When TRUE, this color scheme is a night-time color scheme */
    val isDarkMode: Boolean
    )

/** uses warm natural colors */
val lightColorScheme = MyColorTheme(
    surface = veryLightWarmGray,
    surfaceMore = Color.White,
    surfaceLess = lightWarmGray,
    surfaceInverse = darkWarmGray,
    surfaceText = almostBlack,
    surfaceText2 = cyanDark,
    surfaceInverseText = veryLightWarmGray,
    surfaceInverseText2 = cyanVeryLight,
    surfaceColored = palmGreenDark,
    surfaceColoredText = veryLightWarmGray,

    roomBackground = veryLightWarmGray,
    roomBorder = violet,
    zoneBackground = veryLightWarmGray,
    zoneBorder = cyan,
    flockBackground = Color.White,
    flockBorder = palmGreen,
    scenesBackground = veryLightWarmGray,
    scenesBorder = almostBlack,

    gradientSmall =  cyanLight,
    gradientLarge = cyanDark,

    icon1 = darkWarmGray,
    icon2 = cyanDark,
    icon3 = darkViolet,
    iconInverse1 = lightWarmGray,
    iconInverse2 = cyanLight,
    iconInverse3 = lightViolet,
    iconDisabled = warmGray,

    bulbColor1 = almostBlack,

    borderPrimary = cyanDark,
    borderError = myRed,

    buttonEnabled = cyanDark,
    buttonText = veryLightWarmGray,
    buttonIcon = veryLightWarmGray,
    buttonDisabled = warmGray,
    buttonTextDisabled = veryDarkWarmGray,

    radioButtonSelected = violet,
    radioButtonUnselected = lightViolet,
    radioButtonDisabled = lightWarmGray,

    fabButtonEnabled = cyanDark,
    fabButtonText = veryLightWarmGray,
    fabButtonIcon = veryLightWarmGray,
    fabButtonDisabled = warmGray,
    fabButtonTextDisabled = veryDarkWarmGray,

    textFieldFocusedTextColor = almostBlack,
    textFieldUnfocusedTextColor = veryDarkWarmGray,
    textFieldDisabledTextColor = warmGray,
    textFieldFocusedContainerColor = cyanLight,
    textFieldUnfocusedContainerColor = cyanLight,
    textFieldDisabledContainerColor = lightWarmGray,
    textFieldCursorColor = lightViolet,
    textFieldSelectionColors = TextSelectionColors(
        handleColor = lightWarmGray,
        backgroundColor = violet
    ),
    errorTextColor = myRed,

    positiveTextColor = brownMed,
    negativeTextColor = palmGreen,

    progressIndicatorColor = cyanDark,
    progressIndicatorBackground = veryLightWarmGray,

    switchTrackColor = palmGreen,
    switchDisabledTrackColor = warmGray,

    sliderThumbColor = violet,
    sliderActiveTrackColor = violet,
    sliderInactiveTrackColor = warmGray,
    sliderDisabledTrackColor = lightWarmGray,
    sliderBorderEnabled = almostBlack,
    sliderBorderDisabled = warmGray,
    sliderBackgroundEnabled = veryLightWarmGray,
    sliderBackgroundDisabled = veryLightWarmGray,

    isDarkMode = false,
)

/** cool wild colors */
val darkColorScheme = MyColorTheme(
    surface = veryDarkCoolGray,
    surfaceMore = Color.Black,
    surfaceLess = darkCoolGray,
    surfaceInverse = lightCoolGray,
    surfaceText = veryLightCoolGray,
    surfaceText2 = lightBlueLight,
    surfaceInverseText = veryDarkCoolGray,
    surfaceInverseText2 = yellowLight,
    surfaceColored = darkViolet,
    surfaceColoredText = veryLightCoolGray,

    roomBackground = almostBlack,
    roomBorder = yellowMain,
    zoneBackground = almostBlack,
    zoneBorder = lightBlueMain,
    flockBackground = almostBlack,
    flockBorder = violet,
    scenesBackground = almostBlack,
    scenesBorder = yellowMain,

    gradientSmall =  lightBlueDark,
    gradientLarge = brownMed,

    icon1 = lightCoolGray,
    icon2 = lightBlueMain,
    icon3 = yellowMain,
    iconInverse1 = darkCoolGray,
    iconInverse2 = lightBlueDark,
    iconInverse3 = yellowDark,
    iconDisabled = coolGray,

    bulbColor1 = yellowVeryLight,
    bulbColor2 = yellowLight,
    bulbColor3 = yellowMain,
    bulbColor4 = yellowDark,

    borderPrimary = lightBlueLight,
    borderError = lightMyRed,

    buttonEnabled = lightBlueMain,
    buttonText = almostBlack,
    buttonIcon = almostBlack,
    buttonDisabled = coolGray,
    buttonTextDisabled = veryLightCoolGray,

    radioButtonSelected = yellowMain,
    radioButtonUnselected = yellowDark,
    radioButtonDisabled = coolGray,

    fabButtonEnabled = lightBlueMain,
    fabButtonText = almostBlack,
    fabButtonIcon = almostBlack,
    fabButtonDisabled = coolGray,
    fabButtonTextDisabled = veryLightCoolGray,

    textFieldFocusedTextColor = veryLightCoolGray,
    textFieldUnfocusedTextColor = lightCoolGray,
    textFieldDisabledTextColor = coolGray,
    textFieldFocusedContainerColor = darkBlueMain,
    textFieldUnfocusedContainerColor = darkBlueDark,
    textFieldDisabledContainerColor = darkCoolGray,
    textFieldCursorColor = darkViolet,
    textFieldSelectionColors = TextSelectionColors(
        handleColor = darkCoolGray,
        backgroundColor = violet
    ),

    switchTrackColor = yellowMain,
    switchDisabledTrackColor = coolGray,

    sliderThumbColor = yellowMain,
    sliderActiveTrackColor = yellowMain,
    sliderInactiveTrackColor = coolGray,
    sliderDisabledTrackColor = darkCoolGray,
    sliderBorderEnabled = veryLightCoolGray,
    sliderBorderDisabled = coolGray,
    sliderBackgroundEnabled = veryDarkCoolGray,
    sliderBackgroundDisabled = veryDarkCoolGray,

    progressIndicatorColor = lightBlueMain,
    progressIndicatorBackground = veryDarkCoolGray,

    isDarkMode = true,

    positiveTextColor = yellowMain,
    negativeTextColor = lightBlueMain,
    errorTextColor = myRed,
)

val dallasLightColorScheme = MyColorTheme(
    surface = dallasWhite,
    surfaceInverse = silver,
    surfaceColored = silverLight,
    roomBackground = silverLight,
    roomBorder = helmetBlue,
    zoneBackground = pantsGrayLight,
    zoneBorder = helmetBlue,
    flockBackground = silverLight,
    flockBorder = royalBlue,
    scenesBackground = dallasWhite,
    scenesBorder = almostBlack,

    gradientSmall =  silverLight,
    gradientLarge = royalBlueLight,

    surfaceText = almostBlack,
    surfaceText2 = almostBlack,
    surfaceInverseText = dallasWhite,
    surfaceColoredText = royalBlue,
    positiveTextColor = royalBlue,
    negativeTextColor = almostBlack,
    errorTextColor = myRed,

    icon1 = helmetBlue,
    icon2 = silver,
    icon3 = pantsGray,
    iconInverse1 = dallasWhite,
    iconInverse2 = helmetBlue,
    iconInverse3 = royalBlue,
    iconDisabled = coolGray,

    bulbColor1 = almostBlack,

    borderPrimary = royalBlue,
    borderError = myRed,

    buttonEnabled = royalBlue,
    buttonText = dallasWhite,
    buttonDisabled = coolGray,
    buttonTextDisabled = dallasWhite,
    radioButtonSelected = royalBlue,
    radioButtonUnselected = helmetBlue,
    radioButtonDisabled = coolGray,
    fabButtonEnabled = royalBlue,
    fabButtonText = dallasWhite,
    fabButtonDisabled = coolGray,
    fabButtonTextDisabled = dallasWhite,

    textFieldFocusedTextColor = almostBlack,
    textFieldUnfocusedTextColor = helmetBlue,
    textFieldDisabledTextColor = helmetBlue,
    textFieldFocusedContainerColor = silver,
    textFieldUnfocusedContainerColor = lightCoolGray,
    textFieldDisabledContainerColor = dallasWhite,
    textFieldCursorColor = royalBlue,

    progressIndicatorColor = royalBlue,

    switchTrackColor = royalBlue,
    switchDisabledTrackColor = coolGray,

    sliderThumbColor = royalBlueLight,
    sliderActiveTrackColor = royalBlueLight,
    sliderInactiveTrackColor = lightCoolGray,
    sliderDisabledTrackColor = silver,
    sliderBackgroundEnabled = black,
    sliderBackgroundDisabled = silver,
    sliderBorderEnabled = black,
    sliderBorderDisabled = lightCoolGray,

    isDarkMode = false
)

val LocalTheme = staticCompositionLocalOf<MyColorTheme> {
    error("no colors provided")
}

