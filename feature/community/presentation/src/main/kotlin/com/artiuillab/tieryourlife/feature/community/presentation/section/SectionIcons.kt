package com.artiuillab.tieryourlife.feature.community.presentation.section

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
import com.artiuillab.tieryourlife.core.theme.ui.PersonIcon
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlin.math.cos
import kotlin.math.sin

/** A section's sign: a built-in keeps its own whatever the server names; anybody else's is drawn by its icon name. */
@Composable
internal fun SectionIcon(section: Section, iconSize: Dp, color: Color) {
    val art = section.art
    if (art != null) SectionIcon(art, iconSize, color) else SymbolIcon(section.icon, iconSize, color)
}

/**
 * Each built-in's sign, drawn like the app's other icons rather than taken
 * from an icon font: animation, movie, sports_esports, music_note, menu_book,
 * restaurant, sports_soccer, person, category.
 */
@Composable
internal fun SectionIcon(section: ListCategory, iconSize: Dp, color: Color) {
    when (section) {
        ListCategory.Anime -> AnimeIcon(iconSize, color)
        ListCategory.FilmTv -> MovieIcon(iconSize, color)
        ListCategory.Games -> ControllerIcon(iconSize, color)
        ListCategory.Music -> NoteIcon(iconSize, color)
        ListCategory.Books -> BookIcon(iconSize, color)
        ListCategory.Food -> CutleryIcon(iconSize, color)
        ListCategory.Sport -> BallIcon(iconSize, color)
        ListCategory.People -> PersonIcon(iconSize, color)
        ListCategory.Other -> ShapesIcon(iconSize, color)
    }
}

private const val STROKE = 1.8f

private fun line(scale: Float) = Stroke(width = STROKE * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** A ball in motion with two fading trails behind it, as Material's "animation" symbol: two rings read as a chain link. */
@Composable
private fun AnimeIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val radius = 5f * s
    drawCircle(color, radius, Offset(15f * s, 15f * s), style = line(s))
    listOf(10.5f, 6.5f).forEach { at ->
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(at * s - radius, at * s - radius),
            size = Size(radius * 2, radius * 2),
            style = line(s),
        )
    }
}

@Composable
private fun MovieIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(4f * s, 6f * s), Size(16f * s, 13f * s), CornerRadius(2f * s), style = line(s))
    drawLine(color, Offset(4f * s, 10f * s), Offset(20f * s, 10f * s), STROKE * s)
    listOf(7f, 11f, 15f).forEach { x ->
        drawLine(color, Offset(x * s, 10f * s), Offset((x + 2f) * s, 6f * s), STROKE * s, StrokeCap.Round)
    }
}

@Composable
private fun ControllerIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(3f * s, 8f * s), Size(18f * s, 10f * s), CornerRadius(5f * s), style = line(s))
    drawLine(color, Offset(6.5f * s, 13f * s), Offset(10.5f * s, 13f * s), STROKE * s, StrokeCap.Round)
    drawLine(color, Offset(8.5f * s, 11f * s), Offset(8.5f * s, 15f * s), STROKE * s, StrokeCap.Round)
    drawCircle(color, 1.2f * s, Offset(15f * s, 12f * s))
    drawCircle(color, 1.2f * s, Offset(17.5f * s, 14.5f * s))
}

@Composable
private fun NoteIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawCircle(color, 3f * s, Offset(9f * s, 17f * s))
    drawLine(color, Offset(11.6f * s, 17f * s), Offset(11.6f * s, 5f * s), STROKE * s, StrokeCap.Round)
    val flag = Path().apply {
        moveTo(11.6f * s, 5f * s)
        lineTo(17f * s, 6.5f * s)
        lineTo(17f * s, 9.5f * s)
        lineTo(11.6f * s, 8f * s)
    }
    drawPath(flag, color, style = line(s))
}

@Composable
private fun BookIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    listOf(3.5f, 20.5f).forEach { edge ->
        val page = Path().apply {
            moveTo(12f * s, 7f * s)
            lineTo(edge * s, 5.5f * s)
            lineTo(edge * s, 17.5f * s)
            lineTo(12f * s, 19f * s)
            close()
        }
        drawPath(page, color, style = line(s))
    }
}

@Composable
private fun CutleryIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawLine(color, Offset(8f * s, 4f * s), Offset(8f * s, 20f * s), STROKE * s, StrokeCap.Round)
    drawLine(color, Offset(5.5f * s, 4f * s), Offset(5.5f * s, 8.5f * s), STROKE * s, StrokeCap.Round)
    drawLine(color, Offset(10.5f * s, 4f * s), Offset(10.5f * s, 8.5f * s), STROKE * s, StrokeCap.Round)
    drawArc(color, 0f, 180f, false, Offset(5.5f * s, 6f * s), Size(5f * s, 5f * s), style = line(s))
    val blade = Path().apply {
        moveTo(16.5f * s, 20f * s)
        lineTo(16.5f * s, 4f * s)
        cubicTo(19f * s, 5.5f * s, 19.5f * s, 9f * s, 19f * s, 12.5f * s)
        lineTo(16.5f * s, 12.5f * s)
    }
    drawPath(blade, color, style = line(s))
}

@Composable
private fun BallIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val center = Offset(12f * s, 12f * s)
    drawCircle(color, 8.5f * s, center, style = line(s))
    val pentagon = Path().apply {
        val points = (0 until 5).map { i ->
            val angle = Math.toRadians((-90 + i * 72).toDouble())
            Offset(center.x + 3.4f * s * cos(angle).toFloat(), center.y + 3.4f * s * sin(angle).toFloat())
        }
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(pentagon, color)
}

@Composable
private fun ShapesIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val triangle = Path().apply {
        moveTo(12f * s, 3.5f * s)
        lineTo(16.5f * s, 10.5f * s)
        lineTo(7.5f * s, 10.5f * s)
        close()
    }
    drawPath(triangle, color, style = line(s))
    drawCircle(color, 3.5f * s, Offset(16.5f * s, 16.5f * s), style = line(s))
    drawRect(color, Offset(4f * s, 13f * s), Size(7f * s, 7f * s), style = line(s))
}
