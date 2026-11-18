package com.artiuillab.tieryourlife.feature.settings.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon

/** An envelope: a rounded sheet with its flap folded to the middle. */
@Composable
internal fun MailIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(1.7f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(color, Offset(3.5f * scale, 6f * scale), Size(17f * scale, 12f * scale), CornerRadius(2f * scale), style = stroke)
    val flap = Path().apply {
        moveTo(4.5f * scale, 7.5f * scale)
        lineTo(12f * scale, 13f * scale)
        lineTo(19.5f * scale, 7.5f * scale)
    }
    drawPath(flap, color, style = stroke)
}

@Composable
internal fun AccountCircleIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, 3.6f * scale, Offset(12f * scale, 9.4f * scale), style = Stroke(1.7f * scale))
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(4.6f * scale, 13.4f * scale),
        size = Size(14.8f * scale, 14.8f * scale),
        style = Stroke(1.7f * scale, cap = StrokeCap.Round),
    )
}
