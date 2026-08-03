package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Icons used by more than one screen. Screen-specific icons stay next to their screen.

@Composable
internal fun VectorIcon(
    iconSize: Dp,
    draw: DrawScope.(Float) -> Unit,
) = Canvas(Modifier.size(iconSize)) {
    draw(size.minDimension / 24f)
}

@Composable
internal fun MoreIcon() = MoreVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun MoreVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) { drawCircle(color, 1.5f * scale, Offset(12f * scale, (7f + it * 5f) * scale)) }
}

@Composable
internal fun PlusIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(
        color,
        Offset(12f * scale, 7f * scale),
        Offset(12f * scale, 17f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(7f * scale, 12f * scale),
        Offset(17f * scale, 12f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
}
