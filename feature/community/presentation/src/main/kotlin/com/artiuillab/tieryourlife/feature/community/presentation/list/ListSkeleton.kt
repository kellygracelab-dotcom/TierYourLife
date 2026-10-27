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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.POOL_STRIP_WIDTH
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierRowDensity
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierScreenTopBar

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
            TierScreenTopBar(
                title = preview.title,
                onBack = onBack,
                onManualAdd = {},
                readOnly = true,
                compact = short,
                barHeight = if (docked) 72.dp else null,
                subline = preview.authorName?.takeIf { docked }?.let { name ->
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
            if (!short && !docked) {
                preview.authorName?.let { name ->
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

private fun String.toColorOrNull(): Color? = runCatching { Color(toColorInt()) }.getOrNull()
