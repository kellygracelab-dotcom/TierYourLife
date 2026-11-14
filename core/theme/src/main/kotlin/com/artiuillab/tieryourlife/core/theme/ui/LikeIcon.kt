package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

private const val THUMB_UP =
    "M2 21h3V10H2v11z " +
        "M22 11.5c0-1.1-.9-2-2-2h-5.3l.85-4.1.03-.32c0-.42-.17-.8-.44-1.07L14 3 8.6 8.4C8.23 8.77 8 9.27 8 9.82V19 " +
        "c0 1.1.9 2 2 2h7.6c.8 0 1.5-.48 1.82-1.2l2.45-5.75c.08-.21.13-.44.13-.67V11.5z"

/** A thumb up: filled once liked, an outline before. */
@Composable
fun ThumbUpIcon(iconSize: Dp, color: Color, filled: Boolean) = VectorIcon(iconSize) { unit ->
    val thumb = PathParser().parsePathString(THUMB_UP).toPath()
    scale(unit, unit, pivot = Offset.Zero) {
        if (filled) drawPath(thumb, color) else drawPath(thumb, color, style = Stroke(1.6f, join = StrokeJoin.Round))
    }
}
