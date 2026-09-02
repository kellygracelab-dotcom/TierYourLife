package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun SearchIcon() = SearchVectorIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)

/** Four panes: what the boards would become. Shown while they are rows. */
@Composable
internal fun PicturesIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    VectorIcon(24.dp) { scale ->
        listOf(4f to 4f, 13f to 4f, 4f to 13f, 13f to 13f).forEach { (x, y) ->
            drawRoundRect(
                color = color,
                topLeft = Offset(x * scale, y * scale),
                size = Size(7f * scale, 7f * scale),
                cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale),
                style = Stroke(1.7f * scale),
            )
        }
    }
}

/** Three lines: what the boards would become. Shown while they are pictures. */
@Composable
internal fun RowsIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    VectorIcon(24.dp) { scale ->
        repeat(3) { row ->
            val y = (6f + row * 6f) * scale
            drawLine(color, Offset(4f * scale, y), Offset(20f * scale, y), 1.8f * scale, StrokeCap.Round)
        }
    }
}

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

private const val GEAR_TEETH = 8
private const val GEAR_OUTER_RADIUS = 8.1f
private const val GEAR_ROOT_RADIUS = 5.7f
private const val GEAR_HOLE_RADIUS = 2.6f

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

/** The chevron on a control that opens a menu rather than navigating. */
@Composable
internal fun ChevronDownIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.7f * scale
    drawLine(color, Offset(6f * scale, 9f * scale), Offset(12f * scale, 15f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(12f * scale, 15f * scale), Offset(18f * scale, 9f * scale), stroke, StrokeCap.Round)
}

/**
 * A funnel, filled once something is caught in it.
 *
 * Filled rather than badged: a dot beside an icon says "there is news", and
 * a filter that is on is not news, it is a state the screen is already in.
 */
@Composable
internal fun FilterIcon(on: Boolean) {
    val color = if (on) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    VectorIcon(24.dp) { scale ->
        val funnel = Path().apply {
            moveTo(4f * scale, 5f * scale)
            lineTo(20f * scale, 5f * scale)
            lineTo(14f * scale, 12f * scale)
            lineTo(14f * scale, 19f * scale)
            lineTo(10f * scale, 16.5f * scale)
            lineTo(10f * scale, 12f * scale)
            close()
        }
        if (on) {
            drawPath(funnel, color)
        } else {
            drawPath(funnel, color, style = Stroke(1.7f * scale, join = StrokeJoin.Round))
        }
    }
}

/** The mark of a menu that opens downwards, beside the value it would change. */
@Composable
internal fun ChevronDownIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    VectorIcon(18.dp) { scale ->
        val stroke = 1.7f * scale
        drawLine(color, Offset(6f * scale, 9.5f * scale), Offset(12f * scale, 15f * scale), stroke, StrokeCap.Round)
        drawLine(color, Offset(18f * scale, 9.5f * scale), Offset(12f * scale, 15f * scale), stroke, StrokeCap.Round)
    }
}

/**
 * A star, hollow until it is earned.
 *
 * Drawn either way rather than only when it is on: showing it only on the
 * starred boards would hide the way to star the others.
 */
@Composable
internal fun StarIcon(on: Boolean, size: Dp = 24.dp, colorOverride: Color? = null) {
    val color = colorOverride ?: if (on) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    VectorIcon(size) { scale ->
        val centre = 12f * scale
        val outer = 8f * scale
        val inner = 3.4f * scale
        val star = Path().apply {
            repeat(10) { step ->
                val radius = if (step % 2 == 0) outer else inner
                val angle = (-90f + step * 36f) * (Math.PI / 180f).toFloat()
                val x = centre + radius * cos(angle)
                val y = centre + radius * sin(angle)
                if (step == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        if (on) {
            drawPath(star, color)
        } else {
            drawPath(star, color, style = Stroke(1.6f * scale, join = StrokeJoin.Round))
        }
    }
}
