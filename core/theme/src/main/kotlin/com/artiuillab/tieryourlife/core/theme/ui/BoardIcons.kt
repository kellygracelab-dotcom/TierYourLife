package com.artiuillab.tieryourlife.core.theme.ui

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

/** A filled circle with a tick cut out of it: everything is placed. */
@Composable
fun CheckCircleIcon(iconSize: Dp, color: Color, tick: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, 10f * scale, Offset(12f * scale, 12f * scale))
    val stroke = 1.9f * scale
    drawLine(tick, Offset(7.5f * scale, 12.5f * scale), Offset(10.5f * scale, 15.5f * scale), stroke, StrokeCap.Round)
    drawLine(tick, Offset(10.5f * scale, 15.5f * scale), Offset(16.5f * scale, 9f * scale), stroke, StrokeCap.Round)
}

/** A bookmark with a plus: keep this. */
@Composable
fun BookmarkAddIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    val outline = Path().apply {
        moveTo(13f * scale, 4f * scale)
        lineTo(7f * scale, 4f * scale)
        quadraticTo(5f * scale, 4f * scale, 5f * scale, 6f * scale)
        lineTo(5f * scale, 20f * scale)
        lineTo(12f * scale, 16.5f * scale)
        lineTo(19f * scale, 20f * scale)
        lineTo(19f * scale, 11f * scale)
    }
    drawPath(outline, color, style = Stroke(stroke, cap = StrokeCap.Round))
    drawLine(color, Offset(18f * scale, 2.5f * scale), Offset(18f * scale, 8.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(15f * scale, 5.5f * scale), Offset(21f * scale, 5.5f * scale), stroke, StrokeCap.Round)
}

/** A panel at the end edge with an arrow into it: fold the side panel away. */
@Composable
fun PanelCloseIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.6f * scale
    drawRoundRect(color, Offset(3.5f * scale, 4.5f * scale), Size(17f * scale, 15f * scale), CornerRadius(2f * scale), style = Stroke(stroke))
    drawLine(color, Offset(15f * scale, 4.5f * scale), Offset(15f * scale, 19.5f * scale), stroke)
    drawLine(color, Offset(8f * scale, 9f * scale), Offset(11f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(11f * scale, 12f * scale), Offset(8f * scale, 15f * scale), stroke, StrokeCap.Round)
}

/** The same panel with the arrow out of it: open the side panel. */
@Composable
fun PanelOpenIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.6f * scale
    drawRoundRect(color, Offset(3.5f * scale, 4.5f * scale), Size(17f * scale, 15f * scale), CornerRadius(2f * scale), style = Stroke(stroke))
    drawLine(color, Offset(15f * scale, 4.5f * scale), Offset(15f * scale, 19.5f * scale), stroke)
    drawLine(color, Offset(11f * scale, 9f * scale), Offset(8f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(8f * scale, 12f * scale), Offset(11f * scale, 15f * scale), stroke, StrokeCap.Round)
}

/** A triangle pointing on: let duels rank the cards. Not mirrored, as media controls never are. */
@Composable
fun PlayArrowIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val triangle = Path().apply {
        moveTo(8f * scale, 5.5f * scale)
        lineTo(18.5f * scale, 12f * scale)
        lineTo(8f * scale, 18.5f * scale)
        close()
    }
    drawPath(triangle, color)
    drawPath(triangle, color, style = Stroke(1.6f * scale, join = StrokeJoin.Round))
}

/** An arrow up out of a tray: send the board out for others to see. */
@Composable
fun PublishIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.8f * scale
    drawLine(color, Offset(12f * scale, 15f * scale), Offset(12f * scale, 4.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(7.5f * scale, 9f * scale), Offset(12f * scale, 4.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(16.5f * scale, 9f * scale), Offset(12f * scale, 4.5f * scale), stroke, StrokeCap.Round)
    val tray = Path().apply {
        moveTo(4.5f * scale, 14f * scale)
        lineTo(4.5f * scale, 19.5f * scale)
        lineTo(19.5f * scale, 19.5f * scale)
        lineTo(19.5f * scale, 14f * scale)
    }
    drawPath(tray, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** A cloud with an arrow up: send the published copy again. */
@Composable
fun CloudUploadIcon(iconSize: Dp, color: Color) = CloudIcon(iconSize, color, struck = false)

/** The same cloud struck through: waiting for a connection. */
@Composable
fun CloudOffIcon(iconSize: Dp, color: Color) = CloudIcon(iconSize, color, struck = true)

@Composable
private fun CloudIcon(iconSize: Dp, color: Color, struck: Boolean) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    val cloud = Path().apply {
        moveTo(7f * scale, 18.5f * scale)
        cubicTo(4f * scale, 18.5f * scale, 2.5f * scale, 16.5f * scale, 2.5f * scale, 14.5f * scale)
        cubicTo(2.5f * scale, 12.3f * scale, 4.2f * scale, 10.6f * scale, 6.3f * scale, 10.5f * scale)
        cubicTo(7f * scale, 7.6f * scale, 9.3f * scale, 5.5f * scale, 12f * scale, 5.5f * scale)
        cubicTo(15.1f * scale, 5.5f * scale, 17.5f * scale, 7.9f * scale, 17.7f * scale, 10.9f * scale)
        cubicTo(19.8f * scale, 11.1f * scale, 21.5f * scale, 12.7f * scale, 21.5f * scale, 14.8f * scale)
        cubicTo(21.5f * scale, 16.9f * scale, 19.8f * scale, 18.5f * scale, 17.7f * scale, 18.5f * scale)
        close()
    }
    drawPath(cloud, color, style = Stroke(stroke, join = StrokeJoin.Round))
    if (struck) {
        drawLine(color, Offset(3.5f * scale, 3.5f * scale), Offset(20.5f * scale, 20.5f * scale), stroke, StrokeCap.Round)
    } else {
        drawLine(color, Offset(12f * scale, 16f * scale), Offset(12f * scale, 10f * scale), stroke, StrokeCap.Round)
        drawLine(color, Offset(9.5f * scale, 12.5f * scale), Offset(12f * scale, 10f * scale), stroke, StrokeCap.Round)
        drawLine(color, Offset(14.5f * scale, 12.5f * scale), Offset(12f * scale, 10f * scale), stroke, StrokeCap.Round)
    }
}
