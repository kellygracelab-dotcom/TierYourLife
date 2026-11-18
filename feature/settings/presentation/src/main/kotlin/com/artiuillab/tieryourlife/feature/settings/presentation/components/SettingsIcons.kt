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

/** "↗": the row leaves the app, for Play or the mail app. */
@Composable
internal fun OpenOutsideIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(7f * scale, 17f * scale), Offset(17f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(9f * scale, 7f * scale), Offset(17f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(17f * scale, 7f * scale), Offset(17f * scale, 15f * scale), stroke, StrokeCap.Round)
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

/** A circle with a question in it: how the app works. */
@Composable
internal fun HelpIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(1.7f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawCircle(color, 8.5f * scale, Offset(12f * scale, 12f * scale), style = stroke)
    val hook = Path().apply {
        moveTo(9.6f * scale, 9.8f * scale)
        cubicTo(9.6f * scale, 6.9f * scale, 14.4f * scale, 6.9f * scale, 14.4f * scale, 9.8f * scale)
        cubicTo(14.4f * scale, 11.6f * scale, 12f * scale, 11.8f * scale, 12f * scale, 13.6f * scale)
    }
    drawPath(hook, color, style = stroke)
    drawCircle(color, 1.05f * scale, Offset(12f * scale, 16.6f * scale))
}

/** Three tiers, each shorter than the one above: placing cards. */
@Composable
internal fun TierStackIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    listOf(16f, 12f, 8f).forEachIndexed { index, width ->
        drawRoundRect(color, Offset(4f * scale, (5f + index * 5.5f) * scale), Size(width * scale, 3.4f * scale), CornerRadius(1.2f * scale))
    }
}
