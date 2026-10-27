package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon

/** The cover's colours that are not the theme's own (§2), in light and dark. */
internal data class CoverColors(
    val selected: Color,
    val onSelected: Color,
    val card: Color,
    val sheet: Color,
    val nextCard: Color,
    val ring: Color,
    val cardBorder: Color,
    val scrim: Color,
)

private val LightCover = CoverColors(
    selected = Color(0xFFDEE0FF),
    onSelected = Color(0xFF2C3A80),
    card = Color(0xFFE7E4ED),
    sheet = Color(0xFFEFEDF4),
    nextCard = Color(0xFFF3F0F9),
    ring = Color(0xFF4A5BAA),
    cardBorder = Color(0xFFE4E1E9),
    scrim = Color.Black.copy(alpha = 0.32f),
)

private val DarkCover = CoverColors(
    selected = Color(0xFF333E8F),
    onSelected = Color(0xFFDDE1FF),
    card = Color(0xFF35353C),
    sheet = Color(0xFF2A2A31),
    nextCard = Color(0xFF1F1F25),
    ring = Color(0xFFBAC3FF),
    cardBorder = Color(0xFF46464F),
    scrim = Color.Black.copy(alpha = 0.5f),
)

/** Follows the app's own light or dark, not the system's: the theme setting decides. */
internal val coverColors: CoverColors
    @Composable get() = if (TierYourLifeMedia.current.isDark) DarkCover else LightCover

internal object CoverListType {
    val BandLetter = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val BandLetterEmpty = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val EmptyLabel = TextStyle(fontSize = 11.sp, lineHeight = 14.sp)

    /** Breaks only between words: a title split mid-word on a 36dp card read as two words. */
    val CardText = TextStyle(
        fontSize = 8.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Medium,
        hyphens = Hyphens.Auto,
        lineBreak = LineBreak.Paragraph,
    )
    val PeekCardText = CardText.copy(fontSize = 10.sp, lineHeight = 12.sp)
    val PlusN = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val PeekTitle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val PeekMeta = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val Chip = TextStyle(fontSize = 12.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val HandOffTitle = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium)
    val HandOffBody = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
    val EmptyTitle = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium)
}

/** Three people: Community, one level below Your lists. */
@Composable
internal fun GroupsIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(width = 1.7f * scale, cap = StrokeCap.Round)
    drawCircle(color, 3f * scale, Offset(12f * scale, 8.5f * scale), style = stroke)
    drawCircle(color, 2.3f * scale, Offset(5.5f * scale, 10f * scale), style = stroke)
    drawCircle(color, 2.3f * scale, Offset(18.5f * scale, 10f * scale), style = stroke)
    drawArc(color, 200f, 140f, false, Offset(6f * scale, 13.5f * scale), Size(12f * scale, 10f * scale), style = stroke)
    drawLine(color, Offset(2f * scale, 18f * scale), Offset(4.5f * scale, 15f * scale), 1.7f * scale, StrokeCap.Round)
    drawLine(color, Offset(22f * scale, 18f * scale), Offset(19.5f * scale, 15f * scale), 1.7f * scale, StrokeCap.Round)
}

@Composable
internal fun ArrowDownwardIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.8f * scale
    drawLine(color, Offset(12f * scale, 5f * scale), Offset(12f * scale, 19f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(6f * scale, 13f * scale), Offset(12f * scale, 19f * scale), stroke, StrokeCap.Round)
    drawLine(color, Offset(18f * scale, 13f * scale), Offset(12f * scale, 19f * scale), stroke, StrokeCap.Round)
}

/** A phone half open: the inner screen is where this goes on. */
@Composable
internal fun DevicesFoldIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = Stroke(width = 1.8f * scale, cap = StrokeCap.Round)
    drawRoundRect(color, Offset(4f * scale, 3f * scale), Size(10f * scale, 18f * scale), CornerRadius(2f * scale), style = stroke)
    drawLine(color, Offset(14f * scale, 5f * scale), Offset(20f * scale, 8f * scale), 1.8f * scale, StrokeCap.Round)
    drawLine(color, Offset(20f * scale, 8f * scale), Offset(20f * scale, 22f * scale), 1.8f * scale, StrokeCap.Round)
    drawLine(color, Offset(20f * scale, 22f * scale), Offset(14f * scale, 19f * scale), 1.8f * scale, StrokeCap.Round)
}

@Composable
internal fun ViewListIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    listOf(6f, 12f, 18f).forEach { y ->
        drawLine(color, Offset(4f * scale, y * scale), Offset(6f * scale, y * scale), 2f * scale, StrokeCap.Round)
        drawLine(color, Offset(9f * scale, y * scale), Offset(20f * scale, y * scale), 2f * scale, StrokeCap.Round)
    }
}
