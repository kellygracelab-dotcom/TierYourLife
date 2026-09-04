package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R

private val ROW_HEIGHT = 46.dp
private val BAND_WIDTH = 34.dp
private val TILE_WIDTH = 28.dp
private val TILE_HEIGHT = 40.dp

/**
 * A board on a folding phone's cover screen.
 *
 * Read-only, and that is a decision rather than a shortcut. Ranking is a drag,
 * and a drag across 46dp rows with a camera cutout in the corner is a worse
 * version of a gesture that works perfectly one fold away. So the cover shows
 * what somebody already made and says so plainly when they reach for anything
 * else.
 *
 * Captions are dropped. At this size the letter is the tier and the words
 * under it are the first thing to cost a row its cards.
 */
@Composable
internal fun CoverBoard(
    boards: List<TierList>,
    modifier: Modifier = Modifier,
) {
    if (boards.isEmpty()) return
    var shown by remember(boards.size) { mutableIntStateOf(0) }
    val board = boards[shown.coerceIn(boards.indices)]

    Surface(modifier.fillMaxSize().testTag(CoverTestTags.BOARD), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.safeDrawingPadding().pointerInput(boards.size) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -SWIPE_SLOP) {
                        shown = (shown + 1).coerceAtMost(boards.lastIndex)
                    } else if (dragAmount > SWIPE_SLOP) {
                        shown = (shown - 1).coerceAtLeast(0)
                    }
                }
            },
        ) {
            CoverHeader(board)
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The board is shown in the mode its owner chose. A ranked
                // board is a numbered column, and squashing it into a grid of
                // coloured strips here would be answering a question nobody
                // asked -- at this size a numbered list reads better anyway.
                if (board.displayMode == TierListDisplayMode.FLAT_RANKED) {
                    rankedRows(board).take(RANKED_ROWS).forEach { (place, item, tier) ->
                        CoverRankedRow(place = place, item = item, tier = tier)
                    }
                } else {
                    board.tiers.filterNot { it.isPool }.take(VISIBLE_ROWS).forEach { tier ->
                        CoverRow(tier)
                    }
                }
            }
            BoardDots(count = boards.size, shown = shown)
        }
    }
}

@Composable
private fun CoverHeader(board: TierList) {
    val ranked = board.tiers.filterNot { it.isPool }.sumOf { it.items.size }
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = board.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // The plural already says the word; wrapping it said it twice.
            text = pluralStringResource(R.plurals.tier_lists_ranked_count, ranked, ranked),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun CoverRow(tier: Tier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(BAND_WIDTH)
                .height(ROW_HEIGHT)
                .background(tierColour(tier)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tier.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            tier.items.take(MAX_TILES).forEach { item -> CoverTile(item) }
        }
    }
}

/** Where each card stands overall, in the order the tiers are in. */
private fun rankedRows(board: TierList): List<Triple<Int, TierItem, Tier>> {
    var place = 0
    return board.tiers
        .filterNot { it.isPool }
        .flatMap { tier -> tier.items.map { item -> Triple(++place, item, tier) } }
}

@Composable
private fun CoverRankedRow(place: Int, item: TierItem, tier: Tier) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RANKED_ROW_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = place.toString(),
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.title.take(1),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.title,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tierColour(tier)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tier.label.take(2),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun CoverTile(item: TierItem) {
    Box(
        modifier = Modifier
            .size(width = TILE_WIDTH, height = TILE_HEIGHT)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.title.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Which board of several, in the only space left: 40dp along the bottom, and
 * nothing in the corner where the camera is.
 */
@Composable
private fun BoardDots(count: Int, shown: Int) {
    if (count <= 1) {
        Box(Modifier.height(40.dp))
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(start = 14.dp, end = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(width = if (index == shown) 16.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (index == shown) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun tierColour(tier: Tier): Color {
    val hex = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) tier.colorLight else tier.colorDark
    return runCatching { Color(hex.toColorInt()) }
        .getOrDefault(MaterialTheme.colorScheme.surfaceContainerHighest)
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

internal object CoverTestTags {
    const val BOARD = "cover_board"
    const val UNFOLD = "cover_unfold"
}

/** Five rows is what 339dp holds under a header and above the dots. */
private const val VISIBLE_ROWS = 5

private val RANKED_ROW_HEIGHT = 44.dp

/** Shorter rows, so one more of them fits in the same height. */
private const val RANKED_ROWS = 5

/** Nine tiles across 352dp once the band and the padding are taken out. */
private const val MAX_TILES = 9

private const val SWIPE_SLOP = 24f

@androidx.compose.ui.tooling.preview.Preview(
    name = "Flip cover",
    device = "spec:width=352dp,height=339dp,dpi=340",
    showSystemUi = false,
)
@Composable
private fun CoverBoardPreview() = TierYourLifeTheme(true) {
    CoverBoard(
        boards = listOf(
            TierList(
                id = 1,
                title = "Sci-fi films",
                tiers = listOf(
                    coverTier(1, "S", "#B03A32", "#F1948C", 4),
                    coverTier(2, "A", "#B0763A", "#F1C68C", 6),
                    coverTier(3, "B", "#9AA03A", "#E4EC8C", 3),
                ),
            ),
        ),
    )
}

private fun coverTier(id: Long, label: String, light: String, dark: String, items: Int) = Tier(
    id = id,
    label = label,
    colorLight = light,
    colorDark = dark,
    items = List(items) { index -> TierItem(id = id * 100 + index, title = "Arrival", imageUrl = null) },
)
