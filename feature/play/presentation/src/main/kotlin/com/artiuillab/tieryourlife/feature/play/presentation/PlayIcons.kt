package com.artiuillab.tieryourlife.feature.play.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import kotlin.math.cos
import kotlin.math.sin

/** Each game's picture: two arrows round for Swiss, a crown for the hill, a bracket for the bracket. */
@Composable
internal fun GameIcon(kind: GameKind, iconSize: Dp, color: Color) = when (kind) {
    GameKind.Swiss -> AutorenewIcon(iconSize, color)
    GameKind.Hill -> CrownIcon(iconSize, color)
    GameKind.Bracket -> BracketIcon(iconSize, color)
}

@Composable
private fun AutorenewIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.8f * scale
    val ring = Size(14f * scale, 14f * scale)
    val corner = Offset(5f * scale, 5f * scale)
    drawArc(color, 200f, 130f, false, corner, ring, style = Stroke(stroke, cap = StrokeCap.Round))
    drawArc(color, 20f, 130f, false, corner, ring, style = Stroke(stroke, cap = StrokeCap.Round))
    arrowHead(color, scale, angle = 330f, stroke = stroke)
    arrowHead(color, scale, angle = 150f, stroke = stroke)
}

/** A head on the ring round the middle, at [angle], pointing the way the ring turns. */
private fun DrawScope.arrowHead(color: Color, scale: Float, angle: Float, stroke: Float) {
    val radians = Math.toRadians(angle.toDouble())
    val tip = Offset(
        (RING_CENTER + RING_RADIUS * cos(radians)).toFloat() * scale,
        (RING_CENTER + RING_RADIUS * sin(radians)).toFloat() * scale,
    )
    val heading = radians + Math.PI / 2
    listOf(-1, 1).forEach { side ->
        val back = heading + Math.PI + side * HEAD_SPREAD
        val end = Offset(tip.x + (HEAD_LENGTH * cos(back)).toFloat() * scale, tip.y + (HEAD_LENGTH * sin(back)).toFloat() * scale)
        drawLine(color, tip, end, stroke, StrokeCap.Round)
    }
}

private const val RING_CENTER = 12.0
private const val RING_RADIUS = 7.0
private const val HEAD_LENGTH = 3.2
private const val HEAD_SPREAD = 0.7

@Composable
private fun CrownIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    val crown = Path().apply {
        moveTo(4.5f * scale, 16f * scale)
        lineTo(4f * scale, 8f * scale)
        lineTo(8.5f * scale, 11.5f * scale)
        lineTo(12f * scale, 5.5f * scale)
        lineTo(15.5f * scale, 11.5f * scale)
        lineTo(20f * scale, 8f * scale)
        lineTo(19.5f * scale, 16f * scale)
        close()
    }
    drawPath(crown, color, style = Stroke(stroke, join = StrokeJoin.Round))
    drawLine(color, Offset(5f * scale, 19f * scale), Offset(19f * scale, 19f * scale), stroke, StrokeCap.Round)
}

@Composable
private fun BracketIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.6f * scale
    val box = Size(6f * scale, 4f * scale)
    val corner = CornerRadius(1f * scale)
    listOf(Offset(3f, 4f), Offset(3f, 16f), Offset(15f, 10f)).forEach { at ->
        drawRoundRect(color, Offset(at.x * scale, at.y * scale), box, corner, style = Stroke(stroke))
    }
    drawLine(color, Offset(9f * scale, 6f * scale), Offset(12f * scale, 6f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(9f * scale, 18f * scale), Offset(12f * scale, 18f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 6f * scale), Offset(12f * scale, 18f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 12f * scale), Offset(15f * scale, 12f * scale), stroke, StrokeCap.Round)
}

/** A clock with its ring running back: a game from before. */
@Composable
internal fun HistoryIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.7f * scale
    drawArc(
        color = color,
        startAngle = 160f,
        sweepAngle = 320f,
        useCenter = false,
        topLeft = Offset(5f * scale, 4f * scale),
        size = Size(16f * scale, 16f * scale),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    drawLine(color, Offset(5.5f * scale, 14.7f * scale), Offset(3f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(5.5f * scale, 14.7f * scale), Offset(8f * scale, 12.4f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(13f * scale, 8f * scale), Offset(13f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(13f * scale, 12f * scale), Offset(16f * scale, 14f * scale), stroke, StrokeCap.Round)
}

/** An arrow turning back on itself: take the last pick back. */
@Composable
internal fun UndoIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.8f * scale
    val tail = Path().apply {
        moveTo(5f * scale, 9f * scale)
        lineTo(14.5f * scale, 9f * scale)
        cubicTo(17.5f * scale, 9f * scale, 20f * scale, 11.2f * scale, 20f * scale, 14f * scale)
        cubicTo(20f * scale, 16.8f * scale, 17.5f * scale, 19f * scale, 14.5f * scale, 19f * scale)
        lineTo(10f * scale, 19f * scale)
    }
    drawPath(tail, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color, Offset(5f * scale, 9f * scale), Offset(9f * scale, 5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(5f * scale, 9f * scale), Offset(9f * scale, 13f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun InfoIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawCircle(color, 9f * scale, Offset(12f * scale, 12f * scale), style = Stroke(stroke))
    drawCircle(color, 1.1f * scale, Offset(12f * scale, 8f * scale))
    drawLine(color, Offset(12f * scale, 11f * scale), Offset(12f * scale, 16.5f * scale), stroke, StrokeCap.Round)
}
