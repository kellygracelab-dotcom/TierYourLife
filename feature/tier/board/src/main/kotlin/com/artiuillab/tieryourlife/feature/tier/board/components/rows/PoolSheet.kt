package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.BoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** A pull past this on the handle changes the sheet's state; less is a wobble. */
private val SHEET_PULL_THRESHOLD = 24.dp
private val GLIMPSE_TILE = 28.dp
private const val GLIMPSE_TILES = 3
private val POOL_TILE_WIDTH = 52.dp
private val POOL_TILE_HEIGHT = 76.dp
private val DOCKED_GAP = 10.dp
private val DOCKED_ADD_FIELD_WIDTH = 300.dp

/**
 * The pool as a sheet docked under the rows: folded, a strip with the count,
 * a glimpse of the cards and the Add chip; open, the whole row of cards. It
 * opens on its own only where there is work to do, someone's own board with
 * cards left to rank; a reader's pool and an empty one start folded. A tap
 * on the strip or a pull on it switches, and a drop lands in it either way.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoolSheet(
    pool: Tier,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit = {},
    readOnly: Boolean = false,
    /** What the screen around the board puts in the strip, folded or open: a reader's switch and save. */
    accessories: (@Composable (folded: Boolean) -> Unit)? = null,
    /**
     * A tablet held upright: the head holds the count and the add field, the
     * cards wrap across the width in one, two or four lines that the handle
     * chooses, and the head becomes the trash while a card is lifted.
     */
    docked: Boolean = false,
    /** A line under a docked pool's head from the screen around the board: a reader's word on what their copy is. */
    note: (@Composable () -> Unit)? = null,
) {
    var expanded by rememberSaveable(pool.id) { mutableStateOf(!readOnly && pool.items.isNotEmpty()) }
    val memory = LocalBoardLayoutMemory.current
    val dockRows = memory.poolDockRows.takeIf { it in BoardLayoutMemory.DockRowChoices } ?: BoardLayoutMemory.DEFAULT_DOCK_ROWS
    val lifting = docked && dragController.draggedPayload != null && !readOnly
    val isHovered = dragController.isDragging && dragController.hoveredTierId == pool.id
    val count = pool.items.size
    val description = pluralStringResource(
        if (expanded) {
            R.plurals.tier_detail_ranked_pool_expanded_description
        } else {
            R.plurals.tier_detail_ranked_pool_collapsed_description
        },
        count,
        count,
    )
    var pulled by remember { mutableFloatStateOf(0f) }
    val threshold = with(LocalDensity.current) { SHEET_PULL_THRESHOLD.toPx() }

    DisposableEffect(pool.id) {
        onDispose { dragController.unregisterRowBounds(pool.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .onGloballyPositioned { coordinates -> dragController.registerRowBounds(pool.id, coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.POOL_PANEL)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isHovered) {
                    Modifier.dashedBorder(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        cornerRadius = 20.dp,
                    )
                } else {
                    Modifier
                },
            )
            .animateContentSize()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        // The handle and the line under it are one control: a tap or a pull.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    role = Role.Button
                }
                .testTag(TierDetailTestTags.POOL_TOGGLE)
                .clickable { expanded = !expanded }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> pulled += delta },
                    onDragStopped = {
                        when (val outcome = pullOutcome(pulled, threshold, docked, expanded, dockRows)) {
                            is Pull.Lines -> memory.poolDockRows = outcome.lines
                            is Pull.Open -> expanded = outcome.open
                            null -> Unit
                        }
                        pulled = 0f
                    },
                ),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline),
            )
            if (docked) {
                DockedHead(
                    count = count,
                    readOnly = readOnly,
                    lifting = lifting,
                    dragController = dragController,
                    note = note,
                    onAddClick = onAddClick,
                    onGenerateClick = onGenerateClick,
                )
                return@Column
            }
            accessories?.invoke(!expanded)
            StripHead(pool = pool, expanded = expanded, readOnly = readOnly, onAddClick = onAddClick, onGenerateClick = onGenerateClick)
        }
        if (expanded && docked) {
            PoolCardsWrapped(
                pool = pool,
                lines = dockRows,
                dragController = dragController,
                onMoveItem = onMoveItem,
                onDeleteItem = onDeleteItem,
                onDoubleTap = onDoubleTap,
            )
        } else if (expanded) {
            PoolCards(
                pool = pool,
                dragController = dragController,
                onMoveItem = onMoveItem,
                onDeleteItem = onDeleteItem,
                onDoubleTap = onDoubleTap,
            )
        }
    }
}

/** The first few pictures, small, so a folded pool still says what is in it. */
@Composable
private fun Glimpse(items: List<TierItem>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.filter { it.imageUrl != null }.take(GLIMPSE_TILES).forEach { item ->
            ItemTile(item = item, width = GLIMPSE_TILE, height = GLIMPSE_TILE)
        }
    }
}

@Composable
private fun PoolCards(
    pool: Tier,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit,
) {
    val listState = rememberLazyListState()

    SideEffect {
        dragController.registerItemsRowMeta(pool.id, listState, pool.items.map { it.id })
    }

    // Folded, the row is gone and a drop goes to the front of the pool by the
    // grid rule; open, it lands where the finger points along the row.
    DisposableEffect(pool.id) {
        onDispose { dragController.unregisterItemsRow(pool.id) }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> dragController.registerItemsRowBounds(pool.id, coordinates.boundsInRoot()) }
            .testTag(TierDetailTestTags.POOL_ITEMS),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(pool.items, key = { it.id }) { item ->
            DraggableTile(
                item = item,
                sourceTierId = pool.id,
                width = POOL_TILE_WIDTH,
                height = POOL_TILE_HEIGHT,
                dragController = dragController,
                onMoveItem = onMoveItem,
                onDeleteItem = onDeleteItem,
                onDoubleTap = onDoubleTap,
            )
        }
    }
}

/** The phone's strip under the handle: the count, a glimpse while folded, Add. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StripHead(pool: Tier, expanded: Boolean, readOnly: Boolean, onAddClick: () -> Unit, onGenerateClick: () -> Unit) {
    val count = pool.items.size
    // The count and the two chips sit on one line while they fit and take
    // two when they do not. Given weight instead, the count was squeezed
    // into whatever the chips left over -- about a hundred dp on a folding
    // phone's cover screen, which is narrow enough to break "unranked"
    // across three lines and still run into the buttons.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(R.plurals.tier_detail_pool_unranked_count, count, count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!expanded) {
                Glimpse(pool.items)
            }
        }
        // Someone else's pool is material to rank, not a place to add to.
        if (!readOnly) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AddChip(onClick = onAddClick)
                if (Features.GENERATION_OFFERED) {
                    GenerateChip(onClick = onGenerateClick)
                }
            }
        }
    }
}

/** What a pull on the handle does: docked, it walks through the line counts before it folds or opens. */
private sealed interface Pull {
    data class Lines(val lines: Int) : Pull
    data class Open(val open: Boolean) : Pull
}

private fun pullOutcome(pulled: Float, threshold: Float, docked: Boolean, expanded: Boolean, lines: Int): Pull? {
    val up = pulled < -threshold
    val down = pulled > threshold
    val walking = docked && expanded
    return when {
        walking && up -> Pull.Lines(lines.moreLines())
        walking && down && lines > 1 -> Pull.Lines(lines.fewerLines())
        down -> Pull.Open(false)
        up -> Pull.Open(true)
        else -> null
    }
}

/** The docked pool's head: the count and the add field, or, while a card is lifted, the trash. */
@Composable
private fun DockedHead(
    count: Int,
    readOnly: Boolean,
    lifting: Boolean,
    dragController: TierDragController,
    note: (@Composable () -> Unit)?,
    onAddClick: () -> Unit,
    onGenerateClick: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (lifting) {
            PoolHeadTrash(dragController = dragController, caption = stringResource(R.string.tier_detail_drop_unrank_dock))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.tier_detail_pool_count, count),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!readOnly) {
                AddCardField(onAddClick = onAddClick, onGenerateClick = onGenerateClick, modifier = Modifier.width(DOCKED_ADD_FIELD_WIDTH))
            }
        }
        note?.let { line -> Box(Modifier.padding(top = 4.dp)) { line() } }
    }
}

/**
 * The cards across the whole width, wrapping, in as many lines as the
 * handle chose and scrolling inside them. A drop goes by the cards' own
 * places, the rule the rows use for their grids, so the strip's single-row
 * bookkeeping is let go.
 */
@Composable
private fun PoolCardsWrapped(
    pool: Tier,
    lines: Int,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit,
) {
    SideEffect { dragController.unregisterItemsRow(pool.id) }
    LazyVerticalGrid(
        columns = GridCells.FixedSize(POOL_TILE_WIDTH),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = POOL_TILE_HEIGHT * lines + DOCKED_GAP * (lines - 1))
            .testTag(TierDetailTestTags.POOL_ITEMS),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(DOCKED_GAP),
        verticalArrangement = Arrangement.spacedBy(DOCKED_GAP),
    ) {
        itemsIndexed(pool.items, key = { _, item -> item.id }) { index, item ->
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

private fun Int.moreLines(): Int = BoardLayoutMemory.DockRowChoices.firstOrNull { it > this } ?: this

private fun Int.fewerLines(): Int = BoardLayoutMemory.DockRowChoices.lastOrNull { it < this } ?: this

@Composable
private fun AddChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .testTag(TierDetailTestTags.ADD_CHIP)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.tier_detail_add),
            style = TierYourLifeType.current.supportingLabel,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PoolSheetLightPreview() = TierYourLifeTheme(false) {
    PoolSheet(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        dragController = remember { TierDragController() },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun PoolSheetFoldedDarkPreview() = TierYourLifeTheme(true) {
    PoolSheet(
        pool = previewTierList.tiers.first { it.isPool },
        onAddClick = {},
        onGenerateClick = {},
        dragController = remember { TierDragController() },
        onMoveItem = { _, _, _ -> },
        onDeleteItem = {},
        readOnly = true,
    )
}
