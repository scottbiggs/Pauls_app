package com.sleepfuriously.paulsapp.compose.pool

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PoolOff: ImageVector
    get() {
        if (_PoolOff != null) {
            return _PoolOff!!
        }
        _PoolOff = ImageVector.Builder(
            name = "PoolOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 6.35f,
            viewportHeight = 6.35f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.34395832f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(0.329f, 5.7148f)
                curveTo(0.7655f, 5.6334f, 1.1251f, 5.4213f, 1.4553f, 5.2275f)
                curveToRelative(0f, 0f, 0.3462f, 0.4768f, 0.5569f, 0.4873f)
                curveToRelative(0.2713f, 0.0136f, 0.673f, -0.3346f, 0.673f, -0.3346f)
                curveToRelative(0f, 0f, 0.2828f, 0.2963f, 0.4898f, 0.3346f)
                curveToRelative(0.2204f, 0.0408f, 0.6791f, -0.4544f, 0.6791f, -0.4544f)
                curveToRelative(0f, 0f, 0.5471f, 0.4854f, 0.8607f, 0.4544f)
                curveToRelative(0.2867f, -0.0283f, 0.9028f, -0.4573f, 0.9028f, -0.4573f)
                curveToRelative(0f, 0f, 0.2494f, 0.4361f, 0.4565f, 0.4573f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 0.34395832f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(0.3025f, 4.3901f)
                curveTo(0.7389f, 4.3087f, 1.0985f, 4.0966f, 1.4287f, 3.9027f)
                curveToRelative(0f, 0f, 0.3462f, 0.4768f, 0.5569f, 0.4873f)
                curveToRelative(0.2713f, 0.0136f, 0.673f, -0.3346f, 0.673f, -0.3346f)
                curveToRelative(0f, 0f, 0.2828f, 0.2963f, 0.4898f, 0.3346f)
                curveToRelative(0.2204f, 0.0408f, 0.6791f, -0.4544f, 0.6791f, -0.4544f)
                curveToRelative(0f, 0f, 0.5471f, 0.4854f, 0.8607f, 0.4544f)
                curveToRelative(0.2867f, -0.0283f, 0.9028f, -0.4573f, 0.9028f, -0.4573f)
                curveToRelative(0f, 0f, 0.2494f, 0.4361f, 0.4565f, 0.4573f)
            }
        }.build()

        return _PoolOff!!
    }

@Suppress("ObjectPropertyName")
private var _PoolOff: ImageVector? = null
