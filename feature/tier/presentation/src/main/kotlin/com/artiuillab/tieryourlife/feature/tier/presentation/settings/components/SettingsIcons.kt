package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon

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
