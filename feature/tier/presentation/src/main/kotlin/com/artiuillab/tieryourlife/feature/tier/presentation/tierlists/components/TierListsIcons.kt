package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import kotlin.math.cos
import kotlin.math.sin

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
internal fun DragIcon() = DragVectorIcon(16.dp, MaterialTheme.colorScheme.outline)

@Composable
private fun DragVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) {
        drawCircle(color, 1.3f * scale, Offset(9f * scale, (7f + it * 5f) * scale))
        drawCircle(color, 1.3f * scale, Offset(15f * scale, (7f + it * 5f) * scale))
    }
}

// A gear: a ring plus eight radial teeth, drawn the same hand-path way as every other
// icon in this file rather than pulling in material-icons-extended.
@Composable
internal fun SettingsIcon() = SettingsVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SettingsVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val center = Offset(12f * scale, 12f * scale)
    drawCircle(color, 3.4f * scale, center, style = Stroke(1.7f * scale))
    repeat(8) { i ->
        val angle = (i * Math.PI / 4).toFloat()
        val inner = Offset(center.x + 5.2f * scale * cos(angle), center.y + 5.2f * scale * sin(angle))
        val outer = Offset(center.x + 8.4f * scale * cos(angle), center.y + 8.4f * scale * sin(angle))
        drawLine(color, inner, outer, 1.8f * scale, StrokeCap.Round)
    }
}

// A magnifying glass with a diagonal slash — the "nothing found" glyph.
@Composable
internal fun SearchOffIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawCircle(color, 6f * scale, Offset(10f * scale, 10f * scale), style = Stroke(1.7f * scale))
    drawLine(
        color,
        Offset(14.5f * scale, 14.5f * scale),
        Offset(19f * scale, 19f * scale),
        1.7f * scale,
        StrokeCap.Round,
    )
    drawLine(color, Offset(4f * scale, 4f * scale), Offset(20f * scale, 20f * scale), 1.7f * scale, StrokeCap.Round)
}

// Three bullet-and-line rows — the "no lists at all" glyph.
@Composable
internal fun FormatListBulletedIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    repeat(3) { row ->
        val y = (6f + row * 6f) * scale
        drawCircle(color, radius = 1.2f * scale, center = Offset(5f * scale, y))
        drawLine(color, Offset(9f * scale, y), Offset(20f * scale, y), 1.7f * scale, StrokeCap.Round)
    }
}
