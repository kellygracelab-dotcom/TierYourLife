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
import androidx.compose.ui.unit.dp

@Composable
internal fun BackIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        drawArrow(color, scale)
    }
}

private fun DrawScope.drawArrow(color: Color, scale: Float) {
    drawLine(color, Offset(19f * scale, 12f * scale), Offset(5f * scale, 12f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(11f * scale, 6f * scale), Offset(5f * scale, 12f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(5f * scale, 12f * scale), Offset(11f * scale, 18f * scale), 1.7f * scale, StrokeCap.Round)
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
