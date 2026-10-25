package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.artiuillab.tieryourlife.feature.community.presentation.components.AuthorLine
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TIER_LIST_ITEM_SPACING
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TIER_BAND_WIDTH
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.TierScreenTopBar

/** The board's smallest row, which is what a row of placeholders stands in for. */
private val ROW_HEIGHT = 84.dp
private val TILE_SIZE = 56.dp
private val ROW_CORNER = 12.dp
private val TILE_CORNER = 8.dp

/** Bands to draw for a list published before its palette travelled with it. */
private const val ROWS_WHEN_UNKNOWN = 3

/**
 * The screen as the card already knew it, drawn before the answer: the real
 * title and author, the author's palette down the bands, grey cards by the
 * count. The board replaces it in place, so nothing goes blank or jumps.
 */
@Composable
internal fun ListSkeleton(preview: ListPreview, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(CommunityListTestTags.SKELETON),
    ) {
        TierScreenTopBar(title = preview.title, onBack = onBack, onManualAdd = {}, readOnly = true)
        preview.authorName?.let { name ->
            AuthorLine(name = name, photoUrl = null, follow = null, onOpenAuthor = {}, onToggleFollow = {})
        }
        val bands: List<Color?> = preview.tierColors.map { it.toColorOrNull() }.ifEmpty { List(ROWS_WHEN_UNKNOWN) { null } }
        val tiles = skeletonTiles(preview.itemCount, bands.size)
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(TIER_LIST_ITEM_SPACING),
        ) {
            bands.forEachIndexed { index, band ->
                SkeletonRow(band = band ?: MaterialTheme.colorScheme.surfaceContainerHighest, tiles = tiles[index])
            }
        }
    }
}

@Composable
private fun SkeletonRow(band: Color, tiles: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(ROW_CORNER))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(
            Modifier
                .width(TIER_BAND_WIDTH)
                .fillMaxHeight()
                .background(band),
        )
        Row(
            Modifier
                .fillMaxHeight()
                .padding(horizontal = TIER_LIST_ITEM_SPACING),
            horizontalArrangement = Arrangement.spacedBy(TIER_LIST_ITEM_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(tiles) {
                Box(
                    Modifier
                        .size(TILE_SIZE)
                        .clip(RoundedCornerShape(TILE_CORNER))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}

private fun String.toColorOrNull(): Color? = runCatching { Color(toColorInt()) }.getOrNull()
