package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon

@Composable
internal fun BackIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    VectorIcon(24.dp, autoMirror = true) { scale ->
        drawLine(color, Offset(19f * scale, 12f * scale), Offset(5f * scale, 12f * scale), 1.7f * scale, StrokeCap.Round)
        drawLine(color, Offset(11f * scale, 6f * scale), Offset(5f * scale, 12f * scale), 1.7f * scale, StrokeCap.Round)
        drawLine(color, Offset(5f * scale, 12f * scale), Offset(11f * scale, 18f * scale), 1.7f * scale, StrokeCap.Round)
    }
}

@Composable
internal fun NoteAddIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        drawNoteAdd(color, scale)
    }
}

private fun DrawScope.drawNoteAdd(color: Color, scale: Float) {
    drawRoundRect(
        color,
        Offset(4f * scale, 4f * scale),
        Size(16f * scale, 16f * scale),
        CornerRadius(2f * scale),
        style = Stroke(1.6f * scale),
    )
    drawLine(color, Offset(8f * scale, 12f * scale), Offset(16f * scale, 12f * scale), 1.6f * scale, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 8f * scale), Offset(12f * scale, 16f * scale), 1.6f * scale, StrokeCap.Round)
}

@Composable
internal fun CheckIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.8f * scale
    drawLine(color, Offset(4f * scale, 12.5f * scale), Offset(9f * scale, 17.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(9f * scale, 17.5f * scale), Offset(20f * scale, 6f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun DeleteOutlineIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.6f * scale
    drawLine(color, Offset(4f * scale, 7f * scale), Offset(20f * scale, 7f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(9f * scale, 4f * scale), Offset(15f * scale, 4f * scale), stroke, StrokeCap.Round)
    drawRoundRect(
        color,
        Offset(6f * scale, 7f * scale),
        Size(12f * scale, 13f * scale),
        CornerRadius(2f * scale),
        style = Stroke(stroke),
    )
    drawLine(color, Offset(10f * scale, 10f * scale), Offset(10f * scale, 17f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(14f * scale, 10f * scale), Offset(14f * scale, 17f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun SouthIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(12f * scale, 4f * scale), Offset(12f * scale, 20f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 20f * scale), Offset(6f * scale, 14f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 20f * scale), Offset(18f * scale, 14f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun DeleteFilledIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawRoundRect(color, Offset(9f * scale, 3.5f * scale), Size(6f * scale, 2.5f * scale), CornerRadius(1f * scale))
    drawRoundRect(color, Offset(4f * scale, 6f * scale), Size(16f * scale, 2f * scale), CornerRadius(1f * scale))
    drawRoundRect(color, Offset(6f * scale, 8f * scale), Size(12f * scale, 12f * scale), CornerRadius(2f * scale))
}

@Composable
internal fun ChevronRightIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(9f * scale, 6f * scale), Offset(15f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(15f * scale, 12f * scale), Offset(9f * scale, 18f * scale), stroke, StrokeCap.Round)
}

@Composable
internal fun TuneIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val trackStroke = 1.3f * scale
    val knobStroke = 1.8f * scale
    drawLine(color, Offset(4f * scale, 6f * scale), Offset(20f * scale, 6f * scale), trackStroke, StrokeCap.Round)
    drawLine(color, Offset(8f * scale, 3.5f * scale), Offset(8f * scale, 8.5f * scale), knobStroke, StrokeCap.Round)
    drawLine(color, Offset(4f * scale, 12f * scale), Offset(20f * scale, 12f * scale), trackStroke, StrokeCap.Round)
    drawLine(color, Offset(16f * scale, 9.5f * scale), Offset(16f * scale, 14.5f * scale), knobStroke, StrokeCap.Round)
    drawLine(color, Offset(4f * scale, 18f * scale), Offset(20f * scale, 18f * scale), trackStroke, StrokeCap.Round)
    drawLine(color, Offset(11f * scale, 15.5f * scale), Offset(11f * scale, 20.5f * scale), knobStroke, StrokeCap.Round)
}

@Composable
internal fun ContrastIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawArc(
        color,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(3f * scale, 3f * scale),
        size = Size(18f * scale, 18f * scale),
    )
    drawCircle(color, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = Stroke(1.5f * scale))
}

@Composable
internal fun GridViewIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val cell = CornerRadius(1.5f * scale)
    drawRoundRect(color, Offset(4f * scale, 4f * scale), Size(7f * scale, 7f * scale), cell)
    drawRoundRect(color, Offset(13f * scale, 4f * scale), Size(7f * scale, 7f * scale), cell)
    drawRoundRect(color, Offset(4f * scale, 13f * scale), Size(7f * scale, 7f * scale), cell)
    drawRoundRect(color, Offset(13f * scale, 13f * scale), Size(7f * scale, 7f * scale), cell)
}

@Composable
internal fun ViewCarouselIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val corner = CornerRadius(1.5f * scale)
    drawRoundRect(color, Offset(2f * scale, 8f * scale), Size(4f * scale, 8f * scale), corner)
    drawRoundRect(color, Offset(8f * scale, 4f * scale), Size(8f * scale, 16f * scale), corner)
    drawRoundRect(color, Offset(18f * scale, 8f * scale), Size(4f * scale, 8f * scale), corner)
}

@Composable
internal fun FormatListNumberedIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.6f * scale
    repeat(3) { row ->
        val y = (6f + row * 6f) * scale
        drawCircle(color, radius = 1f * scale, center = Offset(4.5f * scale, y))
        drawLine(color, Offset(9f * scale, y), Offset(20f * scale, y), stroke, StrokeCap.Round)
    }
}

@Composable
internal fun ExpandLessIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(6f * scale, 15f * scale), Offset(12f * scale, 9f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 9f * scale), Offset(18f * scale, 15f * scale), stroke, StrokeCap.Round)
}

// A generic picture placeholder — frame, a sun, a mountain — for the empty photo slot
// in the manual-entry dialog.
@Composable
internal fun ImagePlaceholderIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.5f * scale
    drawRoundRect(
        color,
        Offset(3f * scale, 4f * scale),
        Size(18f * scale, 16f * scale),
        CornerRadius(2f * scale),
        style = Stroke(stroke),
    )
    drawCircle(color, radius = 1.6f * scale, center = Offset(8f * scale, 9.5f * scale))
    drawLine(color, Offset(5f * scale, 17f * scale), Offset(10.5f * scale, 11f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(10.5f * scale, 11f * scale), Offset(14f * scale, 14.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(14f * scale, 14.5f * scale), Offset(19f * scale, 9.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(19f * scale, 9.5f * scale), Offset(21f * scale, 12f * scale), stroke, StrokeCap.Round)
}

// Two overlapping frames — the "choose from photos" glyph on the assist chip.
@Composable
internal fun PhotoLibraryIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    val stroke = 1.4f * scale
    drawRoundRect(
        color,
        Offset(3f * scale, 6f * scale),
        Size(13f * scale, 13f * scale),
        CornerRadius(2f * scale),
        style = Stroke(stroke),
    )
    drawLine(color, Offset(8f * scale, 3f * scale), Offset(19f * scale, 3f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(19f * scale, 3f * scale), Offset(19f * scale, 14f * scale), stroke, StrokeCap.Round)
    drawCircle(color, radius = 1.3f * scale, center = Offset(7f * scale, 10.5f * scale))
    drawLine(color, Offset(5f * scale, 17f * scale), Offset(9.5f * scale, 12.5f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(9.5f * scale, 12.5f * scale), Offset(16f * scale, 19f * scale), stroke, StrokeCap.Round)
}
