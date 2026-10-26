package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** An eye, struck through when [off]: whether pictures are shown. */
@Composable
fun VisibilityIcon(iconSize: Dp, color: Color, off: Boolean) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    val eye = Path().apply {
        moveTo(2.5f * scale, 12f * scale)
        quadraticTo(12f * scale, 3f * scale, 21.5f * scale, 12f * scale)
        quadraticTo(12f * scale, 21f * scale, 2.5f * scale, 12f * scale)
        close()
    }
    drawPath(eye, color, style = Stroke(stroke))
    drawCircle(color, 3f * scale, Offset(12f * scale, 12f * scale), style = Stroke(stroke))
    if (off) {
        drawLine(color, Offset(4f * scale, 4f * scale), Offset(20f * scale, 20f * scale), stroke, StrokeCap.Round)
    }
}
