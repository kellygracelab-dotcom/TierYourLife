package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.tier.board.common.sectionLabel
import com.artiuillab.tieryourlife.feature.tier.board.common.tierColorOrNull
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.OwnBoardTopBar
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.POOL_STRIP_WIDTH
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.SubtitleLink
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRowDensity
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierScreenTopBar
import com.artiuillab.tieryourlife.core.theme.R as ThemeR
import com.artiuillab.tieryourlife.feature.tier.board.R as BoardR

/** The board's own measures: a row of placeholders must be the row that replaces it, or the cards jump. */
private val ROW_CORNER = 12.dp
private val TILE_CORNER = 6.dp

/** The folded pool strip that the board draws under the rows: handle, count, Add. */
private val POOL_STRIP_HEIGHT = 74.dp

/** Bands to draw for a list published before its palette travelled with it. */
private const val ROWS_WHEN_UNKNOWN = 3

/**
 * The screen as the card already knew it, drawn before the answer: the real
 * title and author, the author's palette down the bands, grey cards by the
 * count. The board replaces it in place, so nothing goes blank or jumps.
 */
@Composable
internal fun ListSkeleton(preview: ListPreview, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val authorName = preview.authorName?.takeIf { it.isNotBlank() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The board's own rule, so the skeleton is laid out as the board that replaces it.
        val short = WindowShape.isShort(maxHeight)
        val poolBeside = short || (maxWidth >= WindowShape.WideFrom && maxWidth > maxHeight)
        val rows = TierRowDensity.of(short, poolBeside, maxWidth)
        val docked = !short && !poolBeside && maxWidth >= WindowShape.MediumFrom
        Column(
            Modifier
                .fillMaxSize()
                .testTag(CommunityListTestTags.SKELETON),
        ) {
            SkeletonBar(preview, authorName, short, docked, poolBeside, onBack)
            if (!short && !docked) {
                authorName?.let { name ->
                    AuthorLine(name = name, photoUrl = null, follow = null, onOpenAuthor = {}, onToggleFollow = {})
                }
            }
            val bands: List<Color?> = preview.tierColors.map { it.toColorOrNull() }.ifEmpty { List(ROWS_WHEN_UNKNOWN) { null } }
            val tiles = skeletonTiles(preview.itemCount, bands.size)
            Row(Modifier.weight(1f)) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(TIER_LIST_ITEM_SPACING),
                ) {
                    bands.forEachIndexed { index, band ->
                        SkeletonRow(band = band ?: MaterialTheme.colorScheme.surfaceContainerHighest, tiles = tiles[index], rows = rows)
                    }
                }
                if (poolBeside) PoolColumnPlaceholder(POOL_STRIP_WIDTH)
            }
            if (!poolBeside) PoolStripPlaceholder()
        }
    }
}

/** The bar the board will have: two storeys on a phone held upright, one storey otherwise. */
@Composable
private fun SkeletonBar(preview: ListPreview, authorName: String?, short: Boolean, docked: Boolean, poolBeside: Boolean, onBack: () -> Unit) {
    if (!short && !docked && !poolBeside) {
        SkeletonTwoStoreyBar(preview, onBack)
    } else {
        SkeletonFlatBar(preview.title, authorName?.takeIf { docked }, short, docked, onBack)
    }
}

/** The board's one-storey bar: held sideways, or docked beside the rows with the author under the title. */
@Composable
private fun SkeletonFlatBar(title: String, author: String?, short: Boolean, docked: Boolean, onBack: () -> Unit) {
    TierScreenTopBar(
        title = title,
        onBack = onBack,
        onManualAdd = {},
        readOnly = true,
        compact = short,
        barHeight = if (docked) 72.dp else null,
        subline = author?.let { name ->
            {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        },
    )
}

/** A phone held upright: the board's bar in two storeys, the whole name and its section, as the board that replaces it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkeletonTwoStoreyBar(preview: ListPreview, onBack: () -> Unit) {
    val cards = pluralStringResource(BoardR.plurals.board_cards_count, preview.itemCount, preview.itemCount)
    val counts = preview.section?.let { stringResource(BoardR.string.board_header_counts, sectionLabel(it), cards) } ?: cards
    // The board's line ends in the rankings link: drawn here too, or the line grows as the board arrives.
    val rankings = preview.rankingCount.takeIf { it > 0 }?.let { count ->
        SubtitleLink(stringResource(R.string.community_rankings_link, pluralStringResource(ThemeR.plurals.tier_lists_rankings_count, count, count))) {}
    }
    OwnBoardTopBar(
        title = preview.title,
        counts = counts,
        state = rememberTopAppBarState(),
        onBack = onBack,
        onManualAdd = null,
        onMoreClick = {},
        onRenameList = null,
        play = null,
        publish = null,
        view = null,
        countsLink = rankings,
    )
}

/**
 * Where the pool will stand, at the folded strip's width: the author's
 * arrangement, which opens first, has usually left nothing in it.
 */
@Composable
private fun PoolColumnPlaceholder(width: Dp) {
    Box(
        Modifier
            .navigationBarsPadding()
            .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** Where the folded pool will sit, at its height, so the rows do not shrink when it arrives. */
@Composable
private fun PoolStripPlaceholder() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .height(POOL_STRIP_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(top = 6.dp)
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}

@Composable
private fun SkeletonRow(band: Color, tiles: Int, rows: TierRowDensity) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(rows.occupiedHeight)
            .clip(RoundedCornerShape(ROW_CORNER))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .width(rows.bandWidth)
                .fillMaxHeight()
                .background(band),
        )
        Row(
            Modifier
                .fillMaxHeight()
                .padding(rows.padding),
            horizontalArrangement = Arrangement.spacedBy(rows.gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(tiles) {
                Box(
                    Modifier
                        .size(rows.tileWidth, rows.tileHeight)
                        .clip(RoundedCornerShape(TILE_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

private fun String.toColorOrNull(): Color? = tierColorOrNull(this)
