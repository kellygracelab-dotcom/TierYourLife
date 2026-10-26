package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.ui.CheckIcon
import com.artiuillab.tieryourlife.core.theme.ui.ChevronDownIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier

/** Open in a short window: three cards abreast, readable and draggable. Nothing narrower is either. */
val POOL_COLUMN_WIDTH: Dp = 216.dp

/** Open on a tablet held sideways, where the cards are the roomy size. */
val POOL_COLUMN_WIDE_WIDTH: Dp = 284.dp

/** Folded: a strip with the way back, the count and Add. */
val POOL_STRIP_WIDTH: Dp = 56.dp

private const val POOL_COLUMNS = 3
private val POOL_TILE_WIDTH = 52.dp
private val POOL_TILE_HEIGHT = 76.dp
private val POOL_CORNER = 16.dp

/**
 * The pool beside the rows instead of under them, for a window whose height
 * is better spent on rows: a phone held sideways, a tablet held as one. Open
 * while anything is left to rank, a card as tall as what it holds; folded to
 * a strip when it is empty or the person folds it. Cards are dragged out of
 * it and back exactly as from the strip under the rows; a drop lands where
 * the finger points, by the same rule the rows use for their grids.
 */
@Composable
fun PoolColumn(
    pool: Tier,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (itemId: Long) -> Unit = {},
    readOnly: Boolean = false,
    openWidth: Dp = POOL_COLUMN_WIDTH,
    /** A line under the pool's title from the screen around the board: a reader's word on what their copy is. */
    note: (@Composable () -> Unit)? = null,
) {
    // Null until the person chooses; till then the pool is open exactly when it has work in it.
    var choice by rememberSaveable(pool.id) { mutableStateOf<Boolean?>(null) }
    val open = choice ?: pool.items.isNotEmpty()
    val isHovered = dragController.isDragging && dragController.hoveredTierId == pool.id
    val count = pool.items.size

    // The strip under the rows registered its row of cards for drops along
    // it; a column has none, so a drop here goes by the cards' own places.
    SideEffect { dragController.unregisterItemsRow(pool.id) }
    DisposableEffect(pool.id) {
        onDispose { dragController.unregisterRowBounds(pool.id) }
    }

    val shape = RoundedCornerShape(POOL_CORNER)
    Column(
        modifier = modifier
            .width(if (open) openWidth else POOL_STRIP_WIDTH)
            .clip(shape)
            .onGloballyPositioned { coordinates -> dragController.registerRowBounds(pool.id, coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.POOL_COLUMN)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isHovered) {
                    Modifier.dashedBorder(width = 2.dp, color = MaterialTheme.colorScheme.primary, cornerRadius = POOL_CORNER)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (open) {
            OpenHead(count = count, onFold = { choice = false })
            note?.let { line ->
                Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { line() }
            }
            if (!readOnly) {
                AddField(onAddClick = onAddClick, onGenerateClick = onGenerateClick)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(POOL_COLUMNS),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .testTag(TierDetailTestTags.POOL_ITEMS),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(pool.items, key = { _, item -> item.id }) { index, item ->
                    Box(contentAlignment = Alignment.Center) {
                        DraggableTile(
                            item = item,
                            sourceTierId = pool.id,
                            width = POOL_TILE_WIDTH,
                            height = POOL_TILE_HEIGHT,
                            dragController = dragController,
                            onMoveItem = onMoveItem,
                            onDeleteItem = onDeleteItem,
                            onPositioned = { bounds -> dragController.registerTileBounds(pool.id, item.id, index, bounds) },
                            onDoubleTap = onDoubleTap,
                        )
                    }
                }
            }
        } else {
            FoldedStrip(count = count, readOnly = readOnly, onOpen = { choice = true }, onAddClick = onAddClick)
        }
    }
}

@Composable
private fun OpenHead(count: Int, onFold: () -> Unit) {
    val foldDescription = pluralStringResource(R.plurals.tier_detail_ranked_pool_expanded_description, count, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.tier_detail_pool_unranked_count, count, count),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onFold,
            modifier = Modifier
                .semantics { contentDescription = foldDescription }
                .testTag(TierDetailTestTags.POOL_TOGGLE),
        ) {
            // Towards the edge it folds into.
            Box(Modifier.rotate(-90f)) { ChevronDownIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Where a card is added, at the head of the pool it goes into; the AI studio beside it where it is offered. */
@Composable
private fun AddField(onAddClick: () -> Unit, onGenerateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onAddClick)
                .testTag(TierDetailTestTags.ADD_CHIP)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlusIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.tier_detail_add_card),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (Features.GENERATION_OFFERED) {
            GenerateChip(onClick = onGenerateClick, compact = true)
        }
    }
}

@Composable
private fun FoldedStrip(count: Int, readOnly: Boolean, onOpen: () -> Unit, onAddClick: () -> Unit) {
    val openDescription = pluralStringResource(R.plurals.tier_detail_ranked_pool_collapsed_description, count, count)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onOpen,
            modifier = Modifier
                .semantics { contentDescription = openDescription }
                .testTag(TierDetailTestTags.POOL_TOGGLE),
        ) {
            Box(Modifier.rotate(90f)) { ChevronDownIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (count == 0) {
            CheckIcon(20.dp, MaterialTheme.colorScheme.primary)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!readOnly) {
            val addDescription = stringResource(R.string.tier_detail_add_card)
            FilledTonalIconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = addDescription },
            ) { PlusIcon(20.dp, MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Preview(widthDp = 320, heightDp = 400, showBackground = true)
@Composable
private fun PoolColumnPreview() = TierYourLifeTheme(false) {
    PoolColumn(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        dragController = remember { TierDragController() },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
    )
}
