package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon

private const val STROKE = 1.7f

@Composable
internal fun PersonIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(STROKE * scale, cap = StrokeCap.Round)
    drawCircle(color, radius = 3.6f * scale, center = Offset(12f * scale, 8f * scale), style = stroke)
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(4.5f * scale, 13.5f * scale),
        size = Size(15f * scale, 14f * scale),
        style = stroke,
    )
}

@Composable
internal fun HideIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(STROKE * scale, cap = StrokeCap.Round)
    val lens = Path().apply {
        moveTo(3f * scale, 12f * scale)
        quadraticTo(7.5f * scale, 5.5f * scale, 12f * scale, 5.5f * scale)
        quadraticTo(16.5f * scale, 5.5f * scale, 21f * scale, 12f * scale)
        quadraticTo(16.5f * scale, 18.5f * scale, 12f * scale, 18.5f * scale)
        quadraticTo(7.5f * scale, 18.5f * scale, 3f * scale, 12f * scale)
        close()
    }
    drawPath(lens, color, style = stroke)
    drawCircle(color, radius = 2.4f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
    drawLine(
        color,
        Offset(4.5f * scale, 4.5f * scale),
        Offset(19.5f * scale, 19.5f * scale),
        STROKE * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun FlagIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = Stroke(STROKE * scale, cap = StrokeCap.Round)
    drawLine(
        color,
        Offset(6f * scale, 3.5f * scale),
        Offset(6f * scale, 20.5f * scale),
        STROKE * scale,
        StrokeCap.Round,
    )
    val pennant = Path().apply {
        moveTo(6f * scale, 4.5f * scale)
        lineTo(18.5f * scale, 4.5f * scale)
        lineTo(15.5f * scale, 9f * scale)
        lineTo(18.5f * scale, 13.5f * scale)
        lineTo(6f * scale, 13.5f * scale)
        close()
    }
    drawPath(pennant, color, style = stroke)
}
