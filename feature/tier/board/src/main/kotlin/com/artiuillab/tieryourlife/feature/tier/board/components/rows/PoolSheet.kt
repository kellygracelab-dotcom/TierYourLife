package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.BoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.core.theme.tips.tipAnchor
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TileScroll
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.coroutines.isActive

/** A pull past this on the handle changes the sheet's state; less is a wobble. */
private val SHEET_PULL_THRESHOLD = 24.dp
private val GLIMPSE_TILE = 28.dp
private const val GLIMPSE_TILES = 3
private val POOL_TILE_WIDTH = 52.dp
private val POOL_TILE_HEIGHT = 76.dp
private val DOCKED_GAP = 10.dp
private val DOCKED_ADD_FIELD_WIDTH = 300.dp
private val STRIP_GAP = 10.dp
private val STRIP_SCROLL_EDGE = 48.dp
private const val STRIP_SCROLL_SPEED_DP_PER_SEC = 600

/**
 * The pool as a sheet docked under the rows: folded, a strip with the count,
 * a glimpse of the cards and the Add chip; open, the whole row of cards. It
 * opens on its own only where there is work to do, someone's own board with
 * cards left to rank; a reader's pool and an empty one start folded. Given a
 * [side], it opens whenever that side's pool has cards and folds when it has
 * none, each time the side changes. A tap on the strip or a pull on it
 * switches, and a drop lands in it either way.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PoolSheet(
    pool: Tier,
    onAddClick: (() -> Unit)?,
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
    side: Any? = null,
    /** False leaves the count out of a folded, empty strip, for [accessories] to say what it means. */
    countWhenEmpty: Boolean = true,
    /** The pool ranked one card at a time, offered by a switch in the head; null where the board is not one's own. */
    oneByOnePanel: (@Composable (beside: Boolean) -> Unit)? = null,
    /** A reader's pool is the cards still to place: its head counts what is left rather than naming the pool. */
    countsCardsLeft: Boolean = false,
) {
    var expanded by rememberSaveable(pool.id, side) { mutableStateOf(poolOpensOnItsOwn(side, readOnly, pool.items.size)) }
    val memory = LocalBoardLayoutMemory.current
    val dockRows = memory.poolDockRows.takeIf { it in BoardLayoutMemory.DockRowChoices } ?: BoardLayoutMemory.DEFAULT_DOCK_ROWS
    val lifting = docked && dragController.draggedPayload != null && !readOnly
    val toggle = oneByOneSwitch(offered = oneByOnePanel != null, memory = memory)
    val isHovered = dragController.isDragging && dragController.hoveredTierId == pool.id
    val count = pool.items.size
    val panelInTray = panelInTray(docked, offered = oneByOnePanel != null, memory.oneCardAtATime, expanded, count)
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
                    toggle = toggle,
                    countsCardsLeft = countsCardsLeft,
                )
                return@Column
            }
            accessories?.invoke(!expanded)
            TrayHead(pool, expanded, readOnly, onAddClick, onGenerateClick, toggle, panelInTray, countWhenEmpty, countsCardsLeft)
        }
        SheetCards(
            pool = pool,
            expanded = expanded,
            docked = docked,
            lines = dockRows,
            panel = oneByOnePanel.takeIf { memory.oneCardAtATime },
            onOpen = { expanded = true },
            dragController = dragController,
            onMoveItem = onMoveItem,
            onDeleteItem = onDeleteItem,
            onDoubleTap = onDoubleTap,
        )
    }
}

/** On a phone's open tray the one-at-a-time panel stands in for the cards. */
private fun panelInTray(docked: Boolean, offered: Boolean, oneCardAtATime: Boolean, expanded: Boolean, count: Int): Boolean =
    !docked && offered && oneCardAtATime && expanded && count > 0

/** The strip's head; with the panel in the tray it keeps only Add, the panel bringing its own switch and count. */
@Composable
private fun TrayHead(
    pool: Tier,
    expanded: Boolean,
    readOnly: Boolean,
    onAddClick: (() -> Unit)?,
    onGenerateClick: () -> Unit,
    toggle: (@Composable (worded: Boolean) -> Unit)?,
    panelInTray: Boolean,
    countWhenEmpty: Boolean,
    countsCardsLeft: Boolean,
) {
    val count = pool.items.size
    val adds = !readOnly && onAddClick != null
    if (!stripCounts(countWhenEmpty, count, readOnly) || panelInTray && !adds) return
    StripHead(
        pool = pool,
        expanded = expanded,
        readOnly = readOnly,
        onAddClick = onAddClick,
        onGenerateClick = onGenerateClick,
        toggle = toggle.takeUnless { panelInTray },
        title = if (panelInTray) null else poolTitle(count, countsCardsLeft),
    )
}

/** What the open sheet holds: the panel while ranking one at a time, which opens it, or the cards. */
@Composable
private fun SheetCards(
    pool: Tier,
    expanded: Boolean,
    docked: Boolean,
    lines: Int,
    panel: (@Composable (beside: Boolean) -> Unit)?,
    onOpen: () -> Unit,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit,
) {
    val oneByOne = panel != null
    LaunchedEffect(oneByOne) { if (oneByOne) onOpen() }
    when {
        !expanded -> Unit
        panel != null -> panel(true)
        docked -> PoolCardsWrapped(
            pool = pool,
            lines = lines,
            dragController = dragController,
            onMoveItem = onMoveItem,
            onDeleteItem = onDeleteItem,
            onDoubleTap = onDoubleTap,
        )
        else -> PoolCards(
            pool = pool,
            dragController = dragController,
            onMoveItem = onMoveItem,
            onDeleteItem = onDeleteItem,
            onDoubleTap = onDoubleTap,
        )
    }
}

/** Open where there are cards to rank: on someone's own board, or on whichever side of a screen with sides is shown. */
internal fun poolOpensOnItsOwn(side: Any?, readOnly: Boolean, cards: Int): Boolean = (side != null || !readOnly) && cards > 0

/** A count of none with nothing to add beside it says nothing the accessories cannot say better. */
private fun stripCounts(countWhenEmpty: Boolean, cards: Int, readOnly: Boolean): Boolean = countWhenEmpty || cards > 0 || !readOnly

/** The pool's name and count, or, on a reader's pool, how many cards are left to place. */
@Composable
internal fun poolTitle(count: Int, countsCardsLeft: Boolean): String = if (countsCardsLeft) {
    pluralStringResource(R.plurals.pool_cards_left, count, count)
} else {
    pluralStringResource(R.plurals.tier_detail_pool_unranked_count, count, count)
}

/** The first few pictures, small, so a folded pool still says what is in it. */
@Composable
private fun Glimpse(items: List<TierItem>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.filter { it.imageUrl != null }.take(GLIMPSE_TILES).forEach { item ->
            ItemTile(item = item, width = GLIMPSE_TILE, height = GLIMPSE_TILE, named = false)
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
    val slot = if (dragController.isDragging && dragController.hoveredTierId == pool.id) dragController.hoverIndex else null
    val liftedId = dragController.draggedPayload?.itemId
    val entries = poolEntries(pool.items, slot, liftedId, last = false)
    var rowBounds by remember { mutableStateOf(Rect.Zero) }

    SideEffect {
        dragController.registerItemsRowMeta(pool.id, listState, pool.items.map { it.id })
    }
    HoldPlaceWhileReordering(entries) {
        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
    }
    ScrollStripNearItsEnds(listState, rowBounds, dragController, pool.id)

    // Folded, the row is gone and a drop goes to the front of the pool by the
    // grid rule; open, it lands where the finger points along the row.
    DisposableEffect(pool.id) {
        onDispose { dragController.unregisterItemsRow(pool.id) }
    }

    ProvideTextCardRoom(pool.items, POOL_TILE_WIDTH, POOL_TILE_HEIGHT, STRIP_GAP) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    rowBounds = coordinates.boundsInRoot()
                    dragController.registerItemsRowBounds(pool.id, rowBounds)
                }
                .testTag(TierDetailTestTags.POOL_ITEMS),
            // Each card brings its own gap, so the lifted one, which takes no room, leaves none.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp - STRIP_GAP),
        ) {
            items(entries, key = { it.key }) { entry ->
                when (entry) {
                    PoolEntry.Slot -> CardSlot(POOL_TILE_WIDTH, POOL_TILE_HEIGHT, dragController.draggedPayload, Modifier.padding(end = STRIP_GAP))
                    is PoolEntry.Card -> DraggableTile(
                        item = entry.item,
                        sourceTierId = pool.id,
                        width = POOL_TILE_WIDTH,
                        height = POOL_TILE_HEIGHT,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        modifier = Modifier
                            .padding(end = if (slot != null && entry.item.id == liftedId) 0.dp else STRIP_GAP)
                            .then(if (entry.index == 0) Modifier.tipAnchor(Tip.Place) else Modifier),
                        onDoubleTap = onDoubleTap,
                        collapseWhileLifted = slot != null,
                        scroll = TileScroll.Sideways,
                    )
                }
            }
        }
    }
}

/** A card held near either end of the strip scrolls it, faster toward the edge, as the board does near its top and bottom. */
@Composable
private fun ScrollStripNearItsEnds(listState: LazyListState, rowBounds: Rect, dragController: TierDragController, poolId: Long) {
    val density = LocalDensity.current
    val edge = with(density) { STRIP_SCROLL_EDGE.toPx() }
    val fastest = with(density) { STRIP_SCROLL_SPEED_DP_PER_SEC.dp.toPx() }
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val speed = {
        val pointer = dragController.pointerPositionInRoot
        val held = dragController.draggedPayload != null && dragController.hoveredTierId == poolId
        if (!held || pointer.y !in rowBounds.top..rowBounds.bottom) {
            0f
        } else {
            val toRight = stripEdgeSpeed(pointer.x - rowBounds.left, rowBounds.right - pointer.x, edge, fastest)
            val forward = if (rightToLeft) -toRight else toRight
            when {
                forward < 0f && !listState.canScrollBackward -> 0f
                forward > 0f && !listState.canScrollForward -> 0f
                else -> forward
            }
        }
    }
    val scrolling by remember(listState, rowBounds) { derivedStateOf { speed() != 0f } }
    LaunchedEffect(scrolling) {
        if (!scrolling) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            val delta = withFrameNanos { now ->
                val seconds = if (last < 0L) 0f else (now - last) / 1_000_000_000f
                last = now
                speed() * seconds
            }
            if (delta != 0f) {
                listState.scrollBy(delta)
                dragController.refreshHover()
            }
        }
    }
}

/** Toward the right is positive; nothing outside [edge] of either end. */
internal fun stripEdgeSpeed(fromLeft: Float, fromRight: Float, edge: Float, fastest: Float): Float = when {
    fromLeft < edge -> -fastest * (1f - fromLeft / edge).coerceIn(0f, 1f)
    fromRight < edge -> fastest * (1f - fromRight / edge).coerceIn(0f, 1f)
    else -> 0f
}

/** The phone's strip under the handle: the count, a glimpse while folded, Add; no [title] while the panel says it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StripHead(
    pool: Tier,
    expanded: Boolean,
    readOnly: Boolean,
    onAddClick: (() -> Unit)?,
    onGenerateClick: () -> Unit,
    toggle: (@Composable (worded: Boolean) -> Unit)?,
    title: String?,
) = BoxWithConstraints(Modifier.fillMaxWidth()) {
    val adds = !readOnly && onAddClick != null
    // The count and the chips sit on one line while they fit. When they do
    // not, as in a long language, the count takes a line of its own and the
    // chips keep their words on the next: the switch's picture alone said nothing.
    if (!headFitsOneLine(title, adds, maxWidth)) {
        StripHeadInTwoLines(title, onAddClick.takeIf { adds }, onGenerateClick, toggle)
        return@BoxWithConstraints
    }
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
            if (title != null) {
                Text(
                    text = title,
                    modifier = Modifier.testTag(TierDetailTestTags.POOL_COUNT),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            if (!expanded && title != null) {
                Glimpse(pool.items)
            }
        }
        // Someone else's pool is material to rank, not a place to add to.
        if (adds || toggle != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                toggle?.invoke(true)
                if (adds && onAddClick != null) StripAddChips(onAddClick, onGenerateClick)
            }
        }
    }
}

/** The strip's head when one line is too short: the count above, the switch and Add under it, each with its word. */
@Composable
private fun StripHeadInTwoLines(
    title: String?,
    onAddClick: (() -> Unit)?,
    onGenerateClick: () -> Unit,
    toggle: (@Composable (worded: Boolean) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.testTag(TierDetailTestTags.POOL_COUNT),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onAddClick != null || toggle != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                toggle?.invoke(true)
                Spacer(Modifier.weight(1f))
                onAddClick?.let { StripAddChips(it, onGenerateClick) }
            }
        }
    }
}

@Composable
private fun StripAddChips(onAddClick: () -> Unit, onGenerateClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AddChip(onClick = onAddClick)
        if (Features.GENERATION_OFFERED) {
            GenerateChip(onClick = onGenerateClick)
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
    onAddClick: (() -> Unit)?,
    onGenerateClick: () -> Unit,
    toggle: (@Composable (worded: Boolean) -> Unit)?,
    countsCardsLeft: Boolean,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (lifting) {
            PoolHeadTrash(dragController = dragController, caption = stringResource(R.string.tier_detail_drop_unrank_dock))
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (countsCardsLeft) pluralStringResource(R.plurals.pool_cards_left, count, count) else stringResource(R.string.tier_detail_pool_count, count),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            toggle?.let { Box(Modifier.padding(end = 12.dp)) { it(true) } }
            if (!readOnly && onAddClick != null) {
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
    val gridState = rememberLazyGridState()
    val slot = if (dragController.isDragging && dragController.hoveredTierId == pool.id) dragController.hoverIndex else null
    val entries = poolEntries(pool.items, slot, dragController.draggedPayload?.itemId)
    SideEffect { dragController.unregisterItemsRow(pool.id) }
    HoldPlaceWhileReordering(entries) {
        gridState.requestScrollToItem(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
    }
    ProvideTextCardRoom(pool.items, POOL_TILE_WIDTH, POOL_TILE_HEIGHT, DOCKED_GAP) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.FixedSize(POOL_TILE_WIDTH),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = POOL_TILE_HEIGHT * lines + DOCKED_GAP * (lines - 1))
                .testTag(TierDetailTestTags.POOL_ITEMS),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(DOCKED_GAP),
            verticalArrangement = Arrangement.spacedBy(DOCKED_GAP),
        ) {
            items(entries, key = { it.key }) { entry ->
                when (entry) {
                    PoolEntry.Slot -> CardSlot(POOL_TILE_WIDTH, POOL_TILE_HEIGHT, dragController.draggedPayload)
                    is PoolEntry.Card -> DraggableTile(
                        item = entry.item,
                        sourceTierId = pool.id,
                        width = POOL_TILE_WIDTH,
                        height = POOL_TILE_HEIGHT,
                        dragController = dragController,
                        onMoveItem = onMoveItem,
                        onDeleteItem = onDeleteItem,
                        onPositioned = { bounds -> dragController.registerTileBounds(pool.id, entry.item.id, entry.index, bounds) },
                        onDoubleTap = onDoubleTap,
                        collapseWhileLifted = slot != null,
                        modifier = if (entry.index == 0) Modifier.tipAnchor(Tip.Place) else Modifier,
                    )
                }
            }
        }
    }
}

private fun Int.moreLines(): Int = BoardLayoutMemory.DockRowChoices.firstOrNull { it > this } ?: this

private fun Int.fewerLines(): Int = BoardLayoutMemory.DockRowChoices.lastOrNull { it < this } ?: this

/** Whether the count, the switch with its word and Add stand on one line, the count unbroken. */
@Composable
private fun headFitsOneLine(title: String?, adds: Boolean, width: Dp): Boolean {
    if (title == null) return true
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    fun wide(text: String, style: TextStyle): Dp = with(density) { measurer.measure(text, style, maxLines = 1).size.width.toDp() }
    val titleWidth = wide(title, MaterialTheme.typography.titleSmall)
    val toggleWidth = 8.dp + FilterChipDefaults.IconSize + 8.dp + wide(stringResource(R.string.one_by_one_toggle), MaterialTheme.typography.labelLarge) + 16.dp
    val addWidth = if (adds) 8.dp + 12.dp + 18.dp + 4.dp + wide(stringResource(R.string.tier_detail_add), TierYourLifeType.current.supportingLabel) + 12.dp else 0.dp
    return 32.dp + titleWidth + 8.dp + toggleWidth + addWidth <= width
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .tipAnchor(Tip.Add)
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
