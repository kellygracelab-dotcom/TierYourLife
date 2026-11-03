package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.DraggableTile
import com.artiuillab.tieryourlife.feature.tier.board.components.drag.TierDragController
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import kotlin.math.ceil

/**
 * Where a wrapping row's cards go: one equal cell after another, as many to a
 * line as fit, the way FlowRow breaks them. While a card hovers over the row,
 * its slot takes a cell and the lifted card, if it is this row's, takes none.
 */
internal data class CardCells(val cards: Int, val slot: Int?, val liftedIndex: Int) {
    private val liftedTakesNoCell = slot != null && liftedIndex >= 0

    private val placed = if (liftedTakesNoCell) cards - 1 else cards

    private val slotCell = slot?.coerceIn(0, placed)

    val count: Int = placed + if (slotCell != null) 1 else 0

    fun isSlot(cell: Int): Boolean = cell == slotCell

    /** The card drawn in [cell]; meaningless for the slot's. */
    fun itemIndex(cell: Int): Int {
        val card = if (slotCell != null && cell > slotCell) cell - 1 else cell
        return if (liftedTakesNoCell && card >= liftedIndex) card + 1 else card
    }

    /** The cell [itemIndex] is drawn in, or null for a lifted card that takes none. */
    fun cellOf(itemIndex: Int): Int? {
        if (liftedTakesNoCell && itemIndex == liftedIndex) return null
        val card = if (liftedTakesNoCell && itemIndex > liftedIndex) itemIndex - 1 else itemIndex
        return if (slotCell != null && card >= slotCell) card + 1 else card
    }
}

/** The pixel measures FlowRow uses: cells placed a rounded gap apart, broken against the gap rounded up. */
internal data class CellGrid(val cellWidth: Int, val cellHeight: Int, val gap: Int, val breakGap: Int) {
    val pitch: Int get() = cellHeight + gap

    fun perLine(width: Int): Int = ((width + breakGap) / (cellWidth + breakGap)).coerceAtLeast(1)

    fun lines(cells: Int, width: Int): Int = (cells + perLine(width) - 1) / perLine(width)

    fun height(cells: Int, width: Int): Int {
        val lines = lines(cells, width)
        return if (lines == 0) 0 else lines * cellHeight + (lines - 1) * gap
    }

    companion object {
        fun of(density: Density, width: Dp, height: Dp, gap: Dp): CellGrid = with(density) {
            CellGrid(width.roundToPx(), height.roundToPx(), gap.roundToPx(), ceil(gap.toPx()).toInt())
        }
    }
}

/**
 * The lines of a row inside the board's list that are on screen, give or take
 * [margin] lines: the list's layout is a frame behind the scroll, and the
 * margin is what keeps that frame from showing. A row the list has not placed
 * yet is coming in from one edge, so the lines at that edge are the ones drawn.
 */
internal fun linesOnScreen(
    info: LazyListLayoutInfo,
    firstVisibleIndex: Int,
    firstVisibleOffset: Int,
    itemIndex: Int,
    cardsTop: Int,
    pitch: Int,
    lines: Int,
    fallbackViewport: Int,
    margin: Int = SCREEN_MARGIN_LINES,
): IntRange {
    if (lines <= 0) return IntRange.EMPTY
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).takeIf { it > 0 } ?: fallbackViewport
    val placed = info.visibleItemsInfo.firstOrNull { it.index == itemIndex }
    val linesInViewport = viewport / pitch + 1
    val itemTop = when {
        placed != null -> placed.offset - info.viewportStartOffset
        itemIndex == firstVisibleIndex -> -firstVisibleOffset
        info.visibleItemsInfo.isNotEmpty() && itemIndex < info.visibleItemsInfo.first().index ->
            return (lines - linesInViewport - margin).coerceAtLeast(0) until lines
        else -> return 0 until minOf(lines, linesInViewport + margin)
    }
    val top = -itemTop - cardsTop
    val first = Math.floorDiv(top, pitch) - margin
    val last = Math.floorDiv(top + viewport, pitch) + margin
    return first.coerceAtLeast(0)..last.coerceAtMost(lines - 1)
}

private const val SCREEN_MARGIN_LINES = 2

/**
 * A wrapping row of cards that composes only the lines on screen, laid out
 * exactly as the FlowRow it replaces: a row of three hundred cards is a few
 * dozen composed. Off screen it still takes its whole height, and the lifted
 * card is always composed, so a drag survives its line scrolling away.
 */
@Composable
internal fun WrappedCards(
    tier: Tier,
    rowDensity: TierRowDensity,
    widthPx: Int,
    dragController: TierDragController,
    onMoveItem: (itemId: Long, toTierId: Long, toPosition: Int) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onDoubleTap: (itemId: Long) -> Unit,
    listState: LazyListState?,
    itemIndex: Int,
    hint: (@Composable () -> Unit)?,
    slotContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val grid = remember(density, rowDensity) { CellGrid.of(density, rowDensity.tileWidth, rowDensity.tileHeight, rowDensity.gap) }
    val isHovered = dragController.isDragging && dragController.hoveredTierId == tier.id
    val slot = if (isHovered) dragController.hoverIndex else null
    val liftedId = dragController.draggedPayload?.itemId
    val liftedIndex = if (liftedId == null) -1 else tier.items.indexOfFirst { it.id == liftedId }
    val cells = CardCells(tier.items.size, slot, liftedIndex)
    val lineCount = grid.lines(cells.count, widthPx)
    val cardsTop = with(density) { rowDensity.padding.roundToPx() }
    val fallbackViewport = LocalWindowInfo.current.containerSize.height
    val window by remember(listState, itemIndex, grid, lineCount, cardsTop, fallbackViewport) {
        derivedStateOf {
            if (listState == null) {
                0 until lineCount
            } else {
                linesOnScreen(
                    info = listState.layoutInfo,
                    firstVisibleIndex = listState.firstVisibleItemIndex,
                    firstVisibleOffset = listState.firstVisibleItemScrollOffset,
                    itemIndex = itemIndex,
                    cardsTop = cardsTop,
                    pitch = grid.pitch,
                    lines = lineCount,
                    fallbackViewport = fallbackViewport,
                )
            }
        }
    }
    val showsHint = cells.count == 0 && hint != null
    val children = childrenFor(tier, cells, window, grid.perLine(widthPx), liftedIndex, showsHint)

    Layout(
        content = {
            for (child in children) {
                key(child.key) {
                    when {
                        child.key == HINT_KEY -> hint?.invoke()
                        child.key == SLOT_KEY -> slotContent()
                        else -> {
                            val item = tier.items[child.itemIndex]
                            val index = child.itemIndex
                            DraggableTile(
                                item = item,
                                sourceTierId = tier.id,
                                width = rowDensity.tileWidth,
                                height = rowDensity.tileHeight,
                                dragController = dragController,
                                onMoveItem = onMoveItem,
                                onDeleteItem = onDeleteItem,
                                onPositioned = { bounds -> dragController.registerTileBounds(tier.id, item.id, index, bounds) },
                                onDoubleTap = onDoubleTap,
                                collapseWhileLifted = slot != null,
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
        measurePolicy = WrappedCardsPolicy(grid, cells.count, children.map { it.cell }, showsHint),
    )
}

private data class Child(val key: Any, val cell: Int, val itemIndex: Int)

/**
 * One flat run of keyed children: a card keeps its composition, and the
 * lifted card its gesture, while the lines around it come and go.
 */
private fun childrenFor(tier: Tier, cells: CardCells, window: IntRange, perLine: Int, liftedIndex: Int, showsHint: Boolean): List<Child> {
    val firstCell = window.first * perLine
    val endCell = minOf(cells.count, (window.last + 1) * perLine)
    val liftedCell = if (liftedIndex >= 0) cells.cellOf(liftedIndex) else null
    val liftedApart = liftedIndex >= 0 && (liftedCell == null || liftedCell !in firstCell until endCell)
    return buildList {
        if (showsHint) add(Child(HINT_KEY, NO_CELL, NO_CELL))
        for (cell in firstCell until endCell) {
            if (cells.isSlot(cell)) {
                add(Child(SLOT_KEY, cell, NO_CELL))
            } else {
                val index = cells.itemIndex(cell)
                add(Child(tier.items[index].id, cell, index))
            }
        }
        if (liftedApart) add(Child(tier.items[liftedIndex].id, NO_CELL, liftedIndex))
    }
}

private const val NO_CELL = -1
private const val HINT_KEY = "hint"
private const val SLOT_KEY = "drop-slot"

/** Each child's cell, in the order they were composed; a hint or a lifted card composed apart has none. */
private class WrappedCardsPolicy(
    private val grid: CellGrid,
    private val cells: Int,
    private val childCells: List<Int>,
    private val hasHint: Boolean,
) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val placeables = measurables.map { it.measure(loose) }
        val content = if (hasHint) placeables.first().height else grid.height(cells, width)
        val perLine = grid.perLine(width)
        return layout(width, constraints.constrainHeight(content)) {
            placeables.forEachIndexed { childIndex, placeable ->
                val cell = childCells.getOrElse(childIndex) { NO_CELL }
                if (cell == NO_CELL) {
                    placeable.placeRelative(0, 0)
                } else {
                    placeable.placeRelative(
                        x = (cell % perLine) * (grid.cellWidth + grid.gap),
                        y = (cell / perLine) * grid.pitch,
                    )
                }
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        if (hasHint) measurables.first().minIntrinsicHeight(width) else grid.height(cells, width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        if (hasHint) measurables.first().maxIntrinsicHeight(width) else grid.height(cells, width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        if (hasHint) measurables.first().minIntrinsicWidth(height) else if (cells > 0) grid.cellWidth else 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        if (hasHint) {
            measurables.first().maxIntrinsicWidth(height)
        } else {
            (cells * (grid.cellWidth + grid.gap) - grid.gap).coerceAtLeast(0)
        }
}
