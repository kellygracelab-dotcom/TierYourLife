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
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import kotlin.math.cos
import kotlin.math.sin

/**
 * A section's icon by its Material Symbols name, drawn on a canvas like every
 * other icon here: the app carries no icon font. The built-ins' nine names
 * draw the built-ins' own signs, the fifteen a proposer may pick
 * (SectionSymbols.PROPOSABLE) are drawn below, and a name nobody drew is
 * Other's sign, the generic `category`.
 */
@Composable
internal fun SymbolIcon(name: String, iconSize: Dp, color: Color) {
    val builtIn = ListCategory.entries.firstOrNull { it.symbol == name }
    if (builtIn != null) {
        SectionIcon(builtIn, iconSize, color)
        return
    }
    when (name) {
        "pets" -> PawIcon(iconSize, color)
        "flight" -> PlaneIcon(iconSize, color)
        "directions_car" -> CarIcon(iconSize, color)
        "palette" -> PaletteIcon(iconSize, color)
        "checkroom" -> HangerIcon(iconSize, color)
        "science" -> FlaskIcon(iconSize, color)
        "computer" -> ComputerIcon(iconSize, color)
        "local_cafe" -> CupIcon(iconSize, color)
        "park" -> TreeIcon(iconSize, color)
        "theater_comedy" -> MasksIcon(iconSize, color)
        "mic" -> MicIcon(iconSize, color)
        "photo_camera" -> CameraIcon(iconSize, color)
        "sports_basketball" -> BasketballIcon(iconSize, color)
        "celebration" -> PartyIcon(iconSize, color)
        "star" -> StarOutlineIcon(iconSize, color)
        else -> SectionIcon(ListCategory.Other, iconSize, color)
    }
}

private const val STROKE = 1.8f

private fun line(scale: Float) = Stroke(width = STROKE * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun path(scale: Float, vararg points: Pair<Float, Float>, close: Boolean = true) = Path().apply {
    points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * scale, y * scale) else lineTo(x * scale, y * scale) }
    if (close) close()
}

@Composable
private fun PawIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    listOf(6.5f to 10f, 9.8f to 6.3f, 14.2f to 6.3f, 17.5f to 10f).forEach { (x, y) -> drawCircle(color, 1.9f * s, Offset(x * s, y * s)) }
    drawOval(color, Offset(7.5f * s, 11.8f * s), Size(9f * s, 7.6f * s))
}

@Composable
private fun PlaneIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawLine(color, Offset(12f * s, 3.5f * s), Offset(12f * s, 18.5f * s), 2.2f * s, StrokeCap.Round)
    drawPath(path(s, 12f to 8.8f, 21f to 14f, 21f to 15.6f, 12f to 13f, 3f to 15.6f, 3f to 14f), color)
    drawPath(path(s, 12f to 17f, 15.5f to 19.4f, 15.5f to 20.6f, 12f to 19.6f, 8.5f to 20.6f, 8.5f to 19.4f), color)
}

@Composable
private fun CarIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(3.5f * s, 10.5f * s), Size(17f * s, 6.5f * s), CornerRadius(2f * s), style = line(s))
    drawPath(path(s, 6.2f to 10.5f, 7.8f to 6.5f, 16.2f to 6.5f, 17.8f to 10.5f, close = false), color, style = line(s))
    drawRoundRect(color, Offset(5f * s, 16.5f * s), Size(3.6f * s, 3f * s), CornerRadius(1f * s))
    drawRoundRect(color, Offset(15.4f * s, 16.5f * s), Size(3.6f * s, 3f * s), CornerRadius(1f * s))
    drawCircle(color, 1.1f * s, Offset(7f * s, 13.7f * s))
    drawCircle(color, 1.1f * s, Offset(17f * s, 13.7f * s))
}

@Composable
private fun PaletteIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawCircle(color, 8.5f * s, Offset(12f * s, 12f * s), style = line(s))
    listOf(7.8f to 11.5f, 10.2f to 7.6f, 14.4f to 7.6f, 16.8f to 11.2f).forEach { (x, y) -> drawCircle(color, 1.35f * s, Offset(x * s, y * s)) }
    drawCircle(color, 2f * s, Offset(14.4f * s, 16f * s), style = line(s))
}

@Composable
private fun HangerIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawArc(color, 180f, 270f, false, Offset(10f * s, 4f * s), Size(4f * s, 4f * s), style = line(s))
    drawLine(color, Offset(12f * s, 8f * s), Offset(12f * s, 10f * s), STROKE * s, StrokeCap.Round)
    drawPath(path(s, 12f to 10f, 20.5f to 16.5f, 3.5f to 16.5f), color, style = line(s))
}

@Composable
private fun FlaskIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawLine(color, Offset(9f * s, 3.5f * s), Offset(15f * s, 3.5f * s), STROKE * s, StrokeCap.Round)
    drawPath(
        path(s, 10.2f to 3.5f, 10.2f to 9.5f, 4.5f to 19.5f, 19.5f to 19.5f, 13.8f to 9.5f, 13.8f to 3.5f, close = false),
        color,
        style = line(s),
    )
    drawLine(color, Offset(7.2f * s, 15f * s), Offset(16.8f * s, 15f * s), STROKE * s, StrokeCap.Round)
}

@Composable
private fun ComputerIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(3.5f * s, 5f * s), Size(17f * s, 11f * s), CornerRadius(1.5f * s), style = line(s))
    drawLine(color, Offset(12f * s, 16f * s), Offset(12f * s, 19.5f * s), STROKE * s, StrokeCap.Round)
    drawLine(color, Offset(8f * s, 19.5f * s), Offset(16f * s, 19.5f * s), STROKE * s, StrokeCap.Round)
}

@Composable
private fun CupIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val cup = Path().apply {
        moveTo(4f * s, 6f * s)
        lineTo(16f * s, 6f * s)
        lineTo(16f * s, 12.5f * s)
        quadraticTo(16f * s, 16f * s, 12.5f * s, 16f * s)
        lineTo(7.5f * s, 16f * s)
        quadraticTo(4f * s, 16f * s, 4f * s, 12.5f * s)
        close()
    }
    drawPath(cup, color, style = line(s))
    drawArc(color, -90f, 180f, false, Offset(14f * s, 7f * s), Size(5f * s, 5f * s), style = line(s))
    drawLine(color, Offset(3.5f * s, 19.5f * s), Offset(18.5f * s, 19.5f * s), STROKE * s, StrokeCap.Round)
}

@Composable
private fun TreeIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawPath(path(s, 12f to 3f, 18f to 11.5f, 15f to 11.5f, 19f to 17.5f, 5f to 17.5f, 9f to 11.5f, 6f to 11.5f), color, style = line(s))
    drawLine(color, Offset(12f * s, 17.5f * s), Offset(12f * s, 21f * s), STROKE * s, StrokeCap.Round)
}

@Composable
private fun MasksIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val front = Path().apply {
        moveTo(9f * s, 8.5f * s)
        lineTo(20f * s, 8.5f * s)
        lineTo(20f * s, 13f * s)
        cubicTo(20f * s, 17.5f * s, 17f * s, 20.5f * s, 14.5f * s, 20.5f * s)
        cubicTo(12f * s, 20.5f * s, 9f * s, 17.5f * s, 9f * s, 13f * s)
        close()
    }
    drawPath(front, color, style = line(s))
    drawCircle(color, 1f * s, Offset(12.5f * s, 11.8f * s))
    drawCircle(color, 1f * s, Offset(16.5f * s, 11.8f * s))
    drawArc(color, 0f, 180f, false, Offset(12f * s, 13.6f * s), Size(5f * s, 3.4f * s), style = line(s))
    val back = Path().apply {
        moveTo(7f * s, 14f * s)
        cubicTo(5f * s, 13f * s, 4f * s, 10.5f * s, 4f * s, 8.5f * s)
        lineTo(4f * s, 4f * s)
        lineTo(14.5f * s, 4f * s)
        lineTo(14.5f * s, 6.5f * s)
    }
    drawPath(back, color, style = line(s))
}

@Composable
private fun MicIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(9f * s, 3f * s), Size(6f * s, 11f * s), CornerRadius(3f * s), style = line(s))
    drawArc(color, 0f, 180f, false, Offset(5.5f * s, 7.5f * s), Size(13f * s, 9.5f * s), style = line(s))
    drawLine(color, Offset(12f * s, 17f * s), Offset(12f * s, 20.5f * s), STROKE * s, StrokeCap.Round)
}

@Composable
private fun CameraIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawRoundRect(color, Offset(3f * s, 7f * s), Size(18f * s, 13f * s), CornerRadius(2f * s), style = line(s))
    drawPath(path(s, 8f to 7f, 9.5f to 4.5f, 14.5f to 4.5f, 16f to 7f, close = false), color, style = line(s))
    drawCircle(color, 3.5f * s, Offset(12f * s, 13.5f * s), style = line(s))
}

@Composable
private fun BasketballIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawCircle(color, 8.5f * s, Offset(12f * s, 12f * s), style = line(s))
    drawLine(color, Offset(12f * s, 3.5f * s), Offset(12f * s, 20.5f * s), STROKE * s)
    drawLine(color, Offset(3.5f * s, 12f * s), Offset(20.5f * s, 12f * s), STROKE * s)
    listOf(6.2f to 8.6f, 17.8f to 15.4f).forEach { (edge, bend) ->
        val seam = Path().apply {
            moveTo(edge * s, 5.8f * s)
            cubicTo(bend * s, 8.5f * s, bend * s, 15.5f * s, edge * s, 18.2f * s)
        }
        drawPath(seam, color, style = line(s))
    }
}

@Composable
private fun PartyIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    drawPath(path(s, 4f to 20f, 8.5f to 9f, 15f to 15.5f), color, style = line(s))
    drawLine(color, Offset(12f * s, 8.5f * s), Offset(14.5f * s, 5.5f * s), STROKE * s, StrokeCap.Round)
    drawLine(color, Offset(15.5f * s, 12f * s), Offset(18.5f * s, 10.5f * s), STROKE * s, StrokeCap.Round)
    listOf(17.5f to 5f, 19.5f to 14.5f, 10f to 4.5f).forEach { (x, y) -> drawCircle(color, 1.2f * s, Offset(x * s, y * s)) }
}

@Composable
private fun StarOutlineIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { s ->
    val star = Path().apply {
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) 9f else 3.9f
            val angle = Math.toRadians((-90 + i * 36).toDouble())
            val x = (12f + radius * cos(angle).toFloat()) * s
            val y = (12.8f + radius * sin(angle).toFloat()) * s
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(star, color, style = line(s))
}
