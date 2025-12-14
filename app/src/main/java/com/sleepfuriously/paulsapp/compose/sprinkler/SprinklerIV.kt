package com.sleepfuriously.paulsapp.compose.sprinkler

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * On version of the sprinkler [ImageVector].  Created with the Valkyrie
 * plugin.
 */
val SprinklerOn: ImageVector
    get() {
        if (_SprinklerOn != null) {
            return _SprinklerOn!!
        }
        _SprinklerOn = ImageVector.Builder(
            name = "SprinklerOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 6.35f,
            viewportHeight = 6.35f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                strokeLineWidth = 0.529168f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.9688f, 2.1167f)
                lineTo(3.7042f, 2.6458f)
                horizontalLineTo(2.6458f)
                lineTo(2.3813f, 2.1167f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                strokeLineWidth = 0.422906f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.1077f, 2.3813f)
                lineTo(2.796f, 5.2917f)
                horizontalLineTo(3.554f)
                lineTo(3.2428f, 2.3858f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.15875f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(2.6458f, 1.9646f)
                curveToRelative(0f, 0f, -0.5016f, -0.939f, -2.3813f, -0.6417f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.15875f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(3.7108f, 1.9646f)
                curveToRelative(0f, 0f, 0.5016f, -0.939f, 2.3813f, -0.6417f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.15875f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(2.9104f, 1.8521f)
                curveToRelative(0f, 0f, -0.4782f, -1.2149f, -1.4552f, -1.3636f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.15875f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(3.425f, 1.8475f)
                curveToRelative(0f, 0f, 0.4782f, -1.2149f, 1.4552f, -1.3636f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(2.3151f, 2.3151f)
                curveToRelative(0f, 0f, -1.4264f, -0.2664f, -2.1167f, 0.2646f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(4.0371f, 2.3151f)
                curveToRelative(0f, 0f, 1.4264f, -0.2664f, 2.1167f, 0.2646f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(2.3813f, 2.6458f)
                curveToRelative(0f, 0f, -0.8308f, -0.0018f, -1.1919f, 0.5292f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveToRelative(3.902f, 2.6458f)
                curveToRelative(0f, 0f, 0.8308f, -0.0018f, 1.1919f, 0.5292f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveToRelative(1.5875f, 5.2917f)
                lineToRelative(3.175f, 0f)
            }
        }.build()

        return _SprinklerOn!!
    }

@Suppress("ObjectPropertyName")
private var _SprinklerOn: ImageVector? = null

/**
 * [ImageVector] of the off state of the sprinkler
 */
val SprinklerOff: ImageVector
    get() {
        if (_SprinklerOff != null) {
            return _SprinklerOff!!
        }
        _SprinklerOff = ImageVector.Builder(
            name = "SprinklerOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 6.35f,
            viewportHeight = 6.35f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                strokeLineWidth = 0.529168f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.9688f, 4.5838f)
                lineTo(3.7042f, 5.1129f)
                horizontalLineTo(2.6458f)
                lineTo(2.3813f, 4.5838f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                strokeLineWidth = 0.422906f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.8614f, 4.6811f)
                lineTo(2.796f, 5.2917f)
                horizontalLineTo(3.554f)
                lineTo(3.4878f, 4.6731f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.132292f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveToRelative(1.5875f, 5.2917f)
                lineToRelative(3.175f, 0f)
            }
        }.build()

        return _SprinklerOff!!
    }

@Suppress("ObjectPropertyName")
private var _SprinklerOff: ImageVector? = null
