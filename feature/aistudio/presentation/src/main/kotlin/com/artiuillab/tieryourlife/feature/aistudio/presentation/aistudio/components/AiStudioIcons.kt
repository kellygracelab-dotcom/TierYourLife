package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun VectorIcon(
    iconSize: Dp,
    autoMirror: Boolean = false,
    draw: DrawScope.(Float) -> Unit,
) {
    val mirror = autoMirror && LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(iconSize)) {
        if (mirror) {
            scale(scaleX = -1f, scaleY = 1f) { draw(size.minDimension / 24f) }
        } else {
            draw(size.minDimension / 24f)
        }
    }
}

@Composable
internal fun BackIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(19f * scale, 12f * scale), Offset(5f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(11f * scale, 6f * scale), Offset(5f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(5f * scale, 12f * scale), Offset(11f * scale, 18f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun ArrowUpwardIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.8f * scale
    drawLine(color, Offset(12f * scale, 19f * scale), Offset(12f * scale, 5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(6f * scale, 11f * scale), Offset(12f * scale, 5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(18f * scale, 11f * scale), Offset(12f * scale, 5f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun AutoAwesomeIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawSparkle(color, scale)
}

@Composable
internal fun ErrorOutlineIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = Stroke(1.6f * scale))
    drawLine(color, Offset(12f * scale, 7f * scale), Offset(12f * scale, 13.5f * scale), 1.8f * scale, StrokeCap.Round)
    drawCircle(color, radius = 1f * scale, center = Offset(12f * scale, 16.5f * scale))
}

@Composable
internal fun ClearIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(7f * scale, 7f * scale), Offset(17f * scale, 17f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(17f * scale, 7f * scale), Offset(7f * scale, 17f * scale), stroke, StrokeCap.Round)
}

private fun DrawScope.drawSparkle(
    color: Color,
    scale: Float,
    centerX: Float = 12f,
    centerY: Float = 12f,
    outerRadius: Float = 9f,
    innerRadius: Float = 3.6f,
) {
    val path = Path()
    for (index in 0 until 8) {
        val angleDegrees = -90.0 + index * 45.0
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angleRadians = Math.toRadians(angleDegrees)
        val x = (centerX + radius * cos(angleRadians)).toFloat() * scale
        val y = (centerY + radius * sin(angleRadians)).toFloat() * scale
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
