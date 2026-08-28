package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
private fun ChevronVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
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

@Composable
internal fun SettingsIcon() = SettingsVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

// Teeth joined into one outline, not spokes radiating from a disc — detached
// rays read as a sun, which is what this icon used to look like.
private const val GEAR_TEETH = 8
private const val GEAR_OUTER_RADIUS = 8.1f
private const val GEAR_ROOT_RADIUS = 5.7f
private const val GEAR_HOLE_RADIUS = 2.6f

// Half-width of a tooth at its tip, and the angle the flank takes to fall back
// to the root. Together they leave a visible gap between neighbours.
private const val GEAR_TIP_HALF_ANGLE = 0.16f
private const val GEAR_FLANK_ANGLE = 0.11f

@Composable
private fun SettingsVectorIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val center = Offset(12f * scale, 12f * scale)
    val stroke = Stroke(1.6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

    drawPath(gearPath(center, scale), color, style = stroke)
    drawCircle(color, GEAR_HOLE_RADIUS * scale, center, style = stroke)
}

private fun gearPath(center: Offset, scale: Float): Path = Path().apply {
    val step = (2 * Math.PI / GEAR_TEETH).toFloat()

    fun pointAt(radius: Float, angle: Float) = Offset(
        center.x + radius * scale * cos(angle),
        center.y + radius * scale * sin(angle),
    )

    repeat(GEAR_TEETH) { tooth ->
        val axis = tooth * step
        val vertices = listOf(
            pointAt(GEAR_ROOT_RADIUS, axis - GEAR_TIP_HALF_ANGLE - GEAR_FLANK_ANGLE),
            pointAt(GEAR_OUTER_RADIUS, axis - GEAR_TIP_HALF_ANGLE),
            pointAt(GEAR_OUTER_RADIUS, axis + GEAR_TIP_HALF_ANGLE),
            pointAt(GEAR_ROOT_RADIUS, axis + GEAR_TIP_HALF_ANGLE + GEAR_FLANK_ANGLE),
        )
        if (tooth == 0) moveTo(vertices.first().x, vertices.first().y) else lineTo(vertices.first().x, vertices.first().y)
        vertices.drop(1).forEach { lineTo(it.x, it.y) }
    }
    close()
}

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

@Composable
internal fun FormatListBulletedIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize, autoMirror = true) { scale ->
    repeat(3) { row ->
        val y = (6f + row * 6f) * scale
        drawCircle(color, radius = 1.2f * scale, center = Offset(5f * scale, y))
        drawLine(color, Offset(9f * scale, y), Offset(20f * scale, y), 1.7f * scale, StrokeCap.Round)
    }
}
