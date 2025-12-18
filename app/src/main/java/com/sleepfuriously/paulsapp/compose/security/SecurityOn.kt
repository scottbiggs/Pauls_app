package com.sleepfuriously.paulsapp.compose.security

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SecurityOn: ImageVector
    get() {
        if (_SecurityOn != null) {
            return _SecurityOn!!
        }
        _SecurityOn = ImageVector.Builder(
            name = "SecurityOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 6.35f,
            viewportHeight = 6.35f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.171009f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveToRelative(3.175f, 0.4374f)
                curveToRelative(0f, 0f, 0.9049f, 0.6084f, 2.7147f, 0.8747f)
                curveTo(5.6635f, 3.4429f, 5.2864f, 5.4283f, 3.175f, 5.9126f)
                curveTo(1.0636f, 5.4283f, 0.6865f, 3.4429f, 0.4603f, 1.3122f)
                curveTo(2.2701f, 1.0458f, 3.175f, 0.4374f, 3.175f, 0.4374f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.236659f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.2358f, 4.3319f)
                horizontalLineTo(4.1142f)
                verticalLineTo(2.8037f)
                horizontalLineTo(2.2358f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.3175f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.7054f, 2.7293f)
                verticalLineTo(2.1518f)
                curveToRelative(0f, 0f, 0.0323f, -0.391f, 0.4696f, -0.391f)
                curveToRelative(0.4373f, 0f, 0.4696f, 0.391f, 0.4696f, 0.391f)
                verticalLineToRelative(0.5775f)
            }
        }.build()

        return _SecurityOn!!
    }

@Suppress("ObjectPropertyName")
private var _SecurityOn: ImageVector? = null
