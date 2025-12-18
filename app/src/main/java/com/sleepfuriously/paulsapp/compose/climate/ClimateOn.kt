package com.sleepfuriously.paulsapp.compose.climate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ClimateOn: ImageVector
    get() {
        if (_ClimateOn != null) {
            return _ClimateOn!!
        }
        _ClimateOn = ImageVector.Builder(
            name = "ClimateOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 6.35f,
            viewportHeight = 6.35f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.396875f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.0634f, 0.4148f)
                curveTo(3.5537f, 0.4267f, 3.5535f, 1.0238f, 3.5535f, 1.0238f)
                verticalLineToRelative(3.2171f)
                curveToRelative(0f, 0f, -0.5181f, 0.1734f, -0.5196f, 0.8095f)
                curveTo(3.0336f, 5.5483f, 3.4947f, 5.9521f, 4.0634f, 5.952f)
                curveTo(4.6322f, 5.9521f, 5.0932f, 5.5483f, 5.093f, 5.0504f)
                curveTo(5.0925f, 4.4175f, 4.5734f, 4.2409f, 4.5734f, 4.2409f)
                verticalLineToRelative(-3.2171f)
                curveToRelative(0f, 0f, -2.0E-4f, -0.621f, -0.51f, -0.609f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.2643f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.7059f, 2.5535f)
                lineTo(5.1386f, 2.5535f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.2643f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.7059f, 2.0354f)
                lineTo(5.1386f, 2.0354f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.2643f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.7059f, 1.5173f)
                lineTo(5.1386f, 1.5173f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.2643f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.7059f, 3.0716f)
                lineTo(5.1386f, 3.0716f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.2643f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.7059f, 3.5897f)
                lineTo(5.1386f, 3.5897f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.477657f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.488f, 2.3871f)
                lineTo(1.5545f, 1.522f)
                lineTo(0.6211f, 2.3871f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.477657f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.488f, 3.3966f)
                lineTo(1.5545f, 4.2617f)
                lineTo(0.6211f, 3.3966f)
            }
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.396875f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(3.5535f, 2.4043f)
                lineToRelative(0f, 1.8366f)
                curveToRelative(0f, 0f, -0.5181f, 0.1734f, -0.5196f, 0.8095f)
                curveTo(3.0336f, 5.5483f, 3.4947f, 5.9521f, 4.0634f, 5.952f)
                curveTo(4.6322f, 5.9521f, 5.0932f, 5.5483f, 5.093f, 5.0504f)
                curveTo(5.0925f, 4.4175f, 4.5734f, 4.2409f, 4.5734f, 4.2409f)
                lineToRelative(0f, -1.8366f)
                close()
            }
        }.build()

        return _ClimateOn!!
    }

@Suppress("ObjectPropertyName")
private var _ClimateOn: ImageVector? = null
