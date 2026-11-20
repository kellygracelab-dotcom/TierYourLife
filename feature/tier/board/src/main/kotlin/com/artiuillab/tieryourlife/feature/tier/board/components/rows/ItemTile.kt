package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.ui.CardLabel
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.components.FocusedImage
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** Told of a card whose picture failed to load, so the board can look for the one its original shows now. */
val LocalPictureLost = staticCompositionLocalOf<(TierItem) -> Unit> { {} }

/** [named]: the picture's name on a strip along its foot, as the board shows it unless turned off. */
@Composable
fun ItemTile(item: TierItem, width: Dp, height: Dp, named: Boolean = LocalBoardLayoutMemory.current.namesOnCards) {
    // A picture its source took away (404/403) leaves the card's name, as on the website, not an empty tile.
    var broken by remember(item.imageUrl) { mutableStateOf(false) }
    val lost = LocalPictureLost.current
    val picture = item.imageUrl.takeUnless { broken }
    if (picture == null && width >= TEXT_CARD_MIN_WIDTH) {
        TextCard(title = item.title, width = width, height = height, bordered = width == COMPACT_TILE_WIDTH || width == ROOMY_TILE_WIDTH)
        return
    }
    val media = TierYourLifeMedia.current
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(media.tilePlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        if (picture != null) {
            // Each poster fades in on its own; one already in memory is simply there.
            FocusedImage(
                model = picture,
                focus = item.focus,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                crossfadeMillis = POSTER_FADE_MILLIS,
                onError = {
                    broken = true
                    lost(item)
                },
            )
            if (named) NameStrip(item.title, width, small = width < ROOMY_TILE_WIDTH)
        } else {
            CardLabel(
                text = item.title,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = media.tileLabel,
            )
        }
    }
}

/**
 * White on a shade that darkens toward the foot, so a light poster still
 * carries its name; two lines at most, broken between words. A word too long
 * takes a smaller size, then breaks after its hyphens, then ends in an ellipsis.
 */
@Composable
private fun BoxScope.NameStrip(title: String, width: Dp, small: Boolean) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = MaterialTheme.typography.labelSmall
    val largest = if (small) SMALL_NAME_SP else NAME_SP
    val name = remember(title, width, largest, density, base) {
        fun style(size: Float) = base.copy(fontSize = size.sp, lineHeight = (size * NAME_LINE_HEIGHT).sp)
        val lineHeight = { size: Float -> with(density) { (size * NAME_LINE_HEIGHT).sp.roundToPx() } }
        fitCardTitle(
            title = title,
            room = CardTitleRoom(with(density) { (width - NAME_SIDE * 2).roundToPx() }, spill = 0, height = 2 * lineHeight(largest)),
            largest = largest,
            smallest = NAME_SMALLEST_SP,
            lineHeight = lineHeight,
            measure = { text, size -> measurer.measure(text, style(size), softWrap = false, maxLines = 1).size.width },
        )
    }
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, NAME_SHADE)))
            .padding(start = NAME_SIDE, end = NAME_SIDE, top = if (small) 6.dp else 10.dp, bottom = 2.dp)
            .testTag(TierDetailTestTags.NAME_STRIP)
            // The picture already says its name to a screen reader.
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        name.lines.take(2).forEach { line ->
            Text(
                text = line,
                color = Color.White,
                fontSize = name.size.sp,
                lineHeight = (name.size * NAME_LINE_HEIGHT).sp,
                style = base,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val NAME_SP = 10f
private const val SMALL_NAME_SP = 8f
private const val NAME_SMALLEST_SP = 7f
private const val NAME_LINE_HEIGHT = 1.2f
private val NAME_SIDE = 3.dp

private val NAME_SHADE = Color.Black.copy(alpha = 0.78f)
private const val POSTER_FADE_MILLIS = 150
private val COMPACT_TILE_WIDTH = 40.dp
private val ROOMY_TILE_WIDTH = 52.dp
