package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon

@Composable
internal fun MenuIcon() = LineMenuIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun LineMenuIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) {
        drawLine(
            color,
            Offset(5f * scale, (8f + it * 4f) * scale),
            Offset(19f * scale, (8f + it * 4f) * scale),
            1.7f * scale,
            StrokeCap.Round,
        )
    }
}

@Composable
internal fun SearchIcon() = SearchVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SearchVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, 6f * scale, Offset(10f * scale, 10f * scale), style = Stroke(1.7f * scale))
    drawLine(
        color,
        Offset(14.5f * scale, 14.5f * scale),
        Offset(19f * scale, 19f * scale),
        1.7f * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun ChevronIcon() = ChevronVectorIcon(20.dp, MaterialTheme.colorScheme.outline)

@Composable
private fun ChevronVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(
        color,
        Offset(9f * scale, 7f * scale),
        Offset(15f * scale, 12f * scale),
        1.7f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(15f * scale, 12f * scale),
        Offset(9f * scale, 17f * scale),
        1.7f * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun UpIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawLine(
        color,
        Offset(12f * scale, 17f * scale),
        Offset(12f * scale, 7f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(8f * scale, 11f * scale),
        Offset(12f * scale, 7f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(12f * scale, 7f * scale),
        Offset(16f * scale, 11f * scale),
        1.8f * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun HistoryIcon() = HistoryVectorIcon(16.dp, MaterialTheme.colorScheme.primary)

@Composable
private fun HistoryVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, 7f * scale, Offset(12f * scale, 12f * scale), style = Stroke(1.6f * scale))
    drawLine(
        color,
        Offset(12f * scale, 8f * scale),
        Offset(12f * scale, 12f * scale),
        1.6f * scale,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(12f * scale, 12f * scale),
        Offset(15f * scale, 14f * scale),
        1.6f * scale,
        StrokeCap.Round,
    )
}

@Composable
internal fun DragIcon() = DragVectorIcon(16.dp, MaterialTheme.colorScheme.outline)

@Composable
private fun DragVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) {
        drawCircle(color, 1.3f * scale, Offset(9f * scale, (7f + it * 5f) * scale))
        drawCircle(color, 1.3f * scale, Offset(15f * scale, (7f + it * 5f) * scale))
    }
}

@Composable
internal fun ScheduleIcon() {
    val color = MaterialTheme.colorScheme.outline
    VectorIcon(16.dp) { scale ->
        drawRoundRect(
            color,
            Offset(4f * scale, 5f * scale),
            androidx.compose.ui.geometry.Size(16f * scale, 15f * scale),
            CornerRadius(2f * scale),
            style = Stroke(1.6f * scale),
        )
        drawLine(
            color,
            Offset(8f * scale, 3f * scale),
            Offset(8f * scale, 7f * scale),
            1.6f * scale,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(16f * scale, 3f * scale),
            Offset(16f * scale, 7f * scale),
            1.6f * scale,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(8f * scale, 12f * scale),
            Offset(12f * scale, 12f * scale),
            1.6f * scale,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(12f * scale, 12f * scale),
            Offset(15f * scale, 14f * scale),
            1.6f * scale,
            StrokeCap.Round,
        )
    }
}
