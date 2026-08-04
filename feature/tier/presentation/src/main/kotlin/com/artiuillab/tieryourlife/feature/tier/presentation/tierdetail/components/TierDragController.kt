package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp

internal data class DragPayload(
    val itemId: Long,
    val title: String,
    val imageUrl: String?,
    val sourceTierId: Long,
    val width: Dp,
    val height: Dp,
)

internal data class DropTarget(val itemId: Long, val toTierId: Long, val toPosition: Int)

private data class ItemsRowInfo(
    val bounds: Rect? = null,
    val state: LazyListState? = null,
    val itemIds: List<Long> = emptyList(),
)

private data class TileRecord(val tierId: Long, val index: Int, val bounds: Rect)

// Shared by every row and the pool panel so they agree on one drag state instead
// of each tracking their own.
@Stable
internal class TierDragController {
    var draggedPayload by mutableStateOf<DragPayload?>(null)
        private set
    var pointerPositionInRoot by mutableStateOf(Offset.Zero)
        private set
    var hoveredTierId by mutableStateOf<Long?>(null)
        private set

    val isDragging: Boolean get() = draggedPayload != null

    private val rowBounds = mutableStateMapOf<Long, Rect>()

    // Pool only — it stays a single scrolling LazyRow, so its own layout state answers "what's here".
    private val itemsRowInfo = mutableStateMapOf<Long, ItemsRowInfo>()

    // Ranked tiers only — a FlowRow has no layout state of its own, so each tile reports itself.
    private val tileBounds = mutableStateMapOf<Long, TileRecord>()

    fun registerRowBounds(tierId: Long, bounds: Rect) {
        rowBounds[tierId] = bounds
    }

    fun registerItemsRowBounds(tierId: Long, bounds: Rect) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(bounds = bounds)
    }

    fun registerItemsRowMeta(tierId: Long, state: LazyListState, itemIds: List<Long>) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(state = state, itemIds = itemIds)
    }

    fun registerTileBounds(tierId: Long, itemId: Long, index: Int, bounds: Rect) {
        tileBounds[itemId] = TileRecord(tierId, index, bounds)
    }

    fun beginDrag(payload: DragPayload, rootPosition: Offset) {
        draggedPayload = payload
        pointerPositionInRoot = rootPosition
        recomputeHover()
    }

    fun updateDrag(delta: Offset) {
        if (draggedPayload == null) return
        pointerPositionInRoot += delta
        recomputeHover()
    }

    // null means the pointer was released outside any row.
    fun endDrag(): DropTarget? {
        val drop = computeDrop()
        clear()
        return drop
    }

    fun cancelDrag() {
        clear()
    }

    private fun clear() {
        draggedPayload = null
        hoveredTierId = null
    }

    private fun recomputeHover() {
        val position = pointerPositionInRoot
        hoveredTierId = rowBounds.entries.firstOrNull { (_, rect) -> rect.contains(position) }?.key
    }

    private fun computeDrop(): DropTarget? {
        val payload = draggedPayload ?: return null
        val tierId = hoveredTierId ?: return null

        val pool = itemsRowInfo[tierId]
        val index = if (pool != null) {
            val bounds = pool.bounds ?: return null
            val state = pool.state ?: return null
            val excludedIndex = pool.itemIds.indexOf(payload.itemId).takeIf { it >= 0 }
            insertionIndexInRow(state, pointerPositionInRoot.x - bounds.left, excludedIndex)
        } else {
            val tiles = tileBounds.values.filter { it.tierId == tierId }
            val excludedIndex = tileBounds[payload.itemId]?.takeIf { it.tierId == tierId }?.index
            insertionIndexInGrid(tiles, pointerPositionInRoot, excludedIndex)
        }
        return DropTarget(payload.itemId, tierId, index)
    }
}

private fun insertionIndexInRow(state: LazyListState, localX: Float, excludedFullIndex: Int?): Int {
    val visible = state.layoutInfo.visibleItemsInfo.filter { it.index != excludedFullIndex }
    if (visible.isEmpty()) return 0

    for (info in visible) {
        val midpoint = info.offset + info.size / 2f
        if (localX < midpoint) {
            return translate(info.index, excludedFullIndex)
        }
    }

    return translate(visible.last().index + 1, excludedFullIndex)
}

// Reading order: the line whose vertical midpoint the pointer is still above, then
// the tile within that line whose horizontal midpoint the pointer is still left of.
private fun insertionIndexInGrid(tiles: List<TileRecord>, pointer: Offset, excludedFullIndex: Int?): Int {
    val visible = tiles.filter { it.index != excludedFullIndex }
    if (visible.isEmpty()) return 0

    val lines = visible.groupBy { it.bounds.top }.toSortedMap().values.toList()
    for (line in lines) {
        val lineMidY = (line.minOf { it.bounds.top } + line.maxOf { it.bounds.bottom }) / 2f
        if (pointer.y < lineMidY) {
            return translate(columnIndex(line, pointer.x), excludedFullIndex)
        }
    }

    return translate(columnIndex(lines.last(), pointer.x), excludedFullIndex)
}

private fun columnIndex(line: List<TileRecord>, pointerX: Float): Int {
    val sorted = line.sortedBy { it.bounds.left }
    for (tile in sorted) {
        val midX = (tile.bounds.left + tile.bounds.right) / 2f
        if (pointerX < midX) return tile.index
    }
    return sorted.last().index + 1
}

private fun translate(rawIndex: Int, excludedFullIndex: Int?): Int =
    if (excludedFullIndex != null && rawIndex > excludedFullIndex) rawIndex - 1 else rawIndex
