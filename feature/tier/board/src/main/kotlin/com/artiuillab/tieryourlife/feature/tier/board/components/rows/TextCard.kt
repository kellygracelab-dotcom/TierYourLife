package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** What a picture-less card may borrow from where it sits: the gap beside it, and the size its row's names share. */
@Immutable
data class TextCardRoom(val gap: Dp = 0.dp, val titleSize: Float? = null)

val LocalTextCardRoom = compositionLocalOf { TextCardRoom() }

/** Cards narrower than this keep the old shrinking label: a thumbnail, not a card to read. */
internal val TEXT_CARD_MIN_WIDTH = 40.dp

/** Gives the picture-less cards among [items] one size and lets them lean into [gap]. */
@Composable
internal fun ProvideTextCardRoom(items: List<TierItem>, width: Dp, height: Dp, gap: Dp, content: @Composable () -> Unit) {
    val fitter = rememberTextCardFitter(width, height, gap)
    val titles = remember(items) { items.filter { it.imageUrl == null }.map { it.title } }
    val size = remember(fitter, titles) {
        if (width < TEXT_CARD_MIN_WIDTH) null else sharedCardTitleSize(titles) { fitter.fit(it) }
    }
    CompositionLocalProvider(LocalTextCardRoom provides TextCardRoom(gap, size), content = content)
}

/**
 * A card with no picture: its name, broken only between words, as large as
 * its row allows and never smaller than [CARD_TITLE_SMALLEST_SP]. A word too
 * long for that takes a little of the gap beside the card rather than being
 * cut in two; "Interstellar" split as "Interste / llar" read as two words.
 */
@Composable
internal fun TextCard(title: String, width: Dp, height: Dp, bordered: Boolean) {
    val room = LocalTextCardRoom.current
    val fitter = rememberTextCardFitter(width, height, room.gap)
    val fitted = remember(fitter, title, room.titleSize) { fitter.fit(title, room.titleSize) }
    val style = remember(fitter, fitted.size) { fitter.style(fitted.size) }
    val boxWidth = fitter.tilePx + fitter.growth(fitted.extra)
    val textWidth = fitter.room.width + fitted.extra
    val media = TierYourLifeMedia.current
    val background = if (bordered) MaterialTheme.colorScheme.surfaceContainerHighest else media.tilePlaceholder
    val color = if (bordered) MaterialTheme.colorScheme.onSurface else media.tileLabel
    Box(Modifier.size(width, height), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .exactly(boxWidth, fitter.heightPx)
                .clip(TEXT_CARD_SHAPE)
                .background(background)
                .then(if (bordered) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, TEXT_CARD_SHAPE) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .exactly(textWidth, null)
                    .clearAndSetSemantics { text = AnnotatedString(title) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                fitted.lines.forEach { line -> TitleLine(line, style, color) }
            }
        }
    }
}

@Composable
private fun TitleLine(line: String, style: TextStyle, color: Color) {
    Text(
        text = line,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}

/** Measured in pixels, centred where it overflows: rounding a dp twice is how a word lost its last letter. */
private fun Modifier.exactly(widthPx: Int, heightPx: Int?): Modifier = layout { measurable, constraints ->
    val height = heightPx ?: constraints.maxHeight
    val placeable = measurable.measure(
        Constraints(minWidth = widthPx, maxWidth = widthPx, minHeight = if (heightPx == null) 0 else height, maxHeight = height),
    )
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@Composable
private fun rememberTextCardFitter(width: Dp, height: Dp, gap: Dp): TextCardFitter {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val density = LocalDensity.current
    return remember(measurer, base, density, width, height, gap) { TextCardFitter(measurer, base, density, width, height, gap) }
}

/**
 * A card's measures in pixels. The name keeps [TEXT_PADDING] on each side;
 * leaning out, it first widens the card into the gap, leaving [GAP_KEPT]
 * between neighbours, then gives up all but [LEANING_PADDING] of its own.
 */
internal class TextCardFitter(
    private val measurer: TextMeasurer,
    private val base: TextStyle,
    private val density: Density,
    width: Dp,
    height: Dp,
    gap: Dp,
) {
    val tilePx: Int
    val heightPx: Int
    val room: CardTitleRoom
    private val widening: Int
    private val largest: Float = when {
        width < REGULAR_WIDTH -> SMALL_LARGEST_SP
        width < ROOMY_WIDTH -> REGULAR_LARGEST_SP
        else -> ROOMY_LARGEST_SP
    }
    private val widths = HashMap<Pair<String, Float>, Int>()

    init {
        with(density) {
            tilePx = width.roundToPx()
            heightPx = height.roundToPx()
            val padding = TEXT_PADDING.roundToPx()
            widening = (gap - GAP_KEPT).roundToPx().coerceAtLeast(0)
            room = CardTitleRoom(
                width = tilePx - 2 * padding,
                spill = widening + 2 * (padding - LEANING_PADDING.roundToPx()),
                height = heightPx - 2 * VERTICAL_PADDING.roundToPx(),
            )
        }
    }

    /** How much wider than its cell the card is drawn for a name reaching [extra] past the room. */
    fun growth(extra: Int): Int = extra.coerceAtMost(widening)

    fun style(size: Float): TextStyle = base.merge(
        TextStyle(
            fontSize = size.sp,
            lineHeight = (size * LINE_HEIGHT).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
    )

    fun fit(title: String, cap: Float? = null): CardTitle = fitCardTitle(
        title = title,
        room = room,
        largest = cap?.coerceIn(CARD_TITLE_SMALLEST_SP, largest) ?: largest,
        smallest = CARD_TITLE_SMALLEST_SP,
        lineHeight = { size -> with(density) { (size * LINE_HEIGHT).sp.roundToPx() } },
        measure = { text, size ->
            widths.getOrPut(text to size) { measurer.measure(text, style(size), softWrap = false, maxLines = 1).size.width }
        },
    )
}

private val TEXT_CARD_SHAPE = RoundedCornerShape(6.dp)
private val TEXT_PADDING = 2.dp
private val LEANING_PADDING = 0.5.dp
private val VERTICAL_PADDING = 4.dp
private val GAP_KEPT = 2.dp
private val REGULAR_WIDTH = 44.dp
private val ROOMY_WIDTH = 52.dp
private const val SMALL_LARGEST_SP = 10f
private const val REGULAR_LARGEST_SP = 11f
private const val ROOMY_LARGEST_SP = 12f
private const val LINE_HEIGHT = 1.25f
