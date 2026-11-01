package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon

/** The few signs Community needs that the app had not drawn yet: cloud_off, refresh, arrow_upward, schedule, group, open_in_full, remove, arrow_forward. */
private const val STROKE = 1.8f

private fun line(scale: Float) = Stroke(width = STROKE * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
internal fun CloudOffIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val cloud = Path().apply {
        moveTo(7f * s, 18f * s)
        cubicTo(3.5f * s, 18f * s, 3f * s, 12.5f * s, 6.8f * s, 12f * s)
        cubicTo(7.2f * s, 7.5f * s, 13.5f * s, 6f * s, 15.8f * s, 10.2f * s)
        cubicTo(19.5f * s, 10f * s, 21f * s, 15.5f * s, 18f * s, 17.6f * s)
    }
    drawPath(cloud, color, style = line(s))
    drawLine(color, Offset(4f * s, 4f * s), Offset(20f * s, 20f * s), STROKE * s, StrokeCap.Round)
}

@Composable
internal fun RefreshIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawArc(color, -60f, 300f, false, Offset(5f * s, 5f * s), Size(14f * s, 14f * s), style = line(s))
    val head = Path().apply {
        moveTo(15.5f * s, 3.5f * s)
        lineTo(18.6f * s, 6.2f * s)
        lineTo(15f * s, 8.4f * s)
    }
    drawPath(head, color, style = line(s))
}

@Composable
internal fun ArrowUpwardIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawLine(color, Offset(12f * s, 19f * s), Offset(12f * s, 5f * s), STROKE * s, StrokeCap.Round)
    val head = Path().apply {
        moveTo(6.5f * s, 10.5f * s)
        lineTo(12f * s, 5f * s)
        lineTo(17.5f * s, 10.5f * s)
    }
    drawPath(head, color, style = line(s))
}

@Composable
internal fun ArrowForwardIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { s ->
    drawLine(color, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), STROKE * s, StrokeCap.Round)
    val head = Path().apply {
        moveTo(13.5f * s, 6.5f * s)
        lineTo(19f * s, 12f * s)
        lineTo(13.5f * s, 17.5f * s)
    }
    drawPath(head, color, style = line(s))
}

@Composable
internal fun ScheduleIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawCircle(color, 8.5f * s, Offset(12f * s, 12f * s), style = line(s))
    val hands = Path().apply {
        moveTo(12f * s, 7.5f * s)
        lineTo(12f * s, 12f * s)
        lineTo(15f * s, 14f * s)
    }
    drawPath(hands, color, style = line(s))
}

@Composable
internal fun RemoveIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawLine(color, Offset(6f * s, 12f * s), Offset(18f * s, 12f * s), STROKE * s, StrokeCap.Round)
}

/** A dashed rounded square: a topic waiting for a moderator, in the strip and in Manage. */
@Composable
internal fun DashedSquare(iconSize: Dp, color: Color, corner: Float) = VectorIcon(iconSize) { s ->
    drawRoundRect(
        color = color,
        topLeft = Offset(1f * s, 1f * s),
        size = Size(22f * s, 22f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner * s),
        style = Stroke(width = 1.5f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * s, 2.5f * s))),
    )
}

/** The back arrow in a colour of the caller's: white over a section's picture. */
@Composable
internal fun BackArrowIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { s ->
    drawLine(color, Offset(19f * s, 12f * s), Offset(5f * s, 12f * s), 1.7f * s, StrokeCap.Round)
    drawLine(color, Offset(11f * s, 6f * s), Offset(5f * s, 12f * s), 1.7f * s, StrokeCap.Round)
    drawLine(color, Offset(5f * s, 12f * s), Offset(11f * s, 18f * s), 1.7f * s, StrokeCap.Round)
}
