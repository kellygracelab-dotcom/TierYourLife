package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
internal fun MoreIcon() = MoreVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun MoreVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) { drawCircle(color, 1.5f * scale, Offset(12f * scale, (7f + it * 5f) * scale)) }
}

@Composable
internal fun PlusIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(
        color,
        Offset(12f * scale, 7f * scale),
        Offset(12f * scale, 17f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(7f * scale, 12f * scale),
        Offset(17f * scale, 12f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun ClearIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(7f * scale, 7f * scale), Offset(17f * scale, 17f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(17f * scale, 7f * scale), Offset(7f * scale, 17f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun DeleteSweepIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.6f * scale
    drawLine(color, Offset(3f * scale, 7f * scale), Offset(14f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(8f * scale, 4f * scale), Offset(12f * scale, 4f * scale), stroke, StrokeCap.Round)
    drawRoundRect(
        color,
        Offset(5f * scale, 7f * scale),
        androidx.compose.ui.geometry.Size(9f * scale, 12f * scale),
        androidx.compose.ui.geometry.CornerRadius(2f * scale),
        style = Stroke(stroke),
    )
    drawLine(color, Offset(7.5f * scale, 10f * scale), Offset(7.5f * scale, 16f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(11.5f * scale, 10f * scale), Offset(11.5f * scale, 16f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(16f * scale, 9f * scale), Offset(21f * scale, 9f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(17f * scale, 13f * scale), Offset(21f * scale, 13f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(16f * scale, 17f * scale), Offset(20f * scale, 17f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun FileDownloadIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.6f * scale
    drawLine(color, Offset(12f * scale, 3f * scale), Offset(12f * scale, 14f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(7.5f * scale, 9.5f * scale), Offset(12f * scale, 14f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 14f * scale), Offset(16.5f * scale, 9.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(4f * scale, 17f * scale), Offset(4f * scale, 20f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(4f * scale, 20f * scale), Offset(20f * scale, 20f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(20f * scale, 20f * scale), Offset(20f * scale, 17f * scale), stroke, StrokeCap.Round)
}
