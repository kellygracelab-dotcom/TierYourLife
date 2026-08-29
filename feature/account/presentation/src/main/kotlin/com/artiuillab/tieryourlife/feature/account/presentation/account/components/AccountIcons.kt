package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp

@Composable
internal fun VectorIcon(iconSize: Dp, draw: DrawScope.(scale: Float) -> Unit) {
    Canvas(Modifier.size(iconSize)) { draw(size.minDimension / 24f) }
}

@Composable
internal fun CloseIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(color, Offset(6f * scale, 6f * scale), Offset(18f * scale, 18f * scale), 1.8f * scale, StrokeCap.Round)
    drawLine(color, Offset(18f * scale, 6f * scale), Offset(6f * scale, 18f * scale), 1.8f * scale, StrokeCap.Round)
}

@Composable
internal fun PencilIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(color, Offset(4f * scale, 20f * scale), Offset(8f * scale, 19f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(7f * scale, 19.5f * scale), Offset(19f * scale, 7.5f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(4.5f * scale, 19.5f * scale), Offset(16.5f * scale, 5f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(16.5f * scale, 5f * scale), Offset(19f * scale, 7.5f * scale), 1.7f * scale, StrokeCap.Round)
}

@Composable
internal fun PersonIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, radius = 4f * scale, center = Offset(12f * scale, 8f * scale))
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(4f * scale, 13f * scale),
        size = androidx.compose.ui.geometry.Size(16f * scale, 14f * scale),
    )
}

@Composable
internal fun CheckIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(color, Offset(5f * scale, 12.5f * scale), Offset(10f * scale, 17.5f * scale), 1.9f * scale, StrokeCap.Round)
    drawLine(color, Offset(10f * scale, 17.5f * scale), Offset(19f * scale, 7f * scale), 1.9f * scale, StrokeCap.Round)
}
