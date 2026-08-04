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

// What a tile can be dropped onto. A tier (ranked row or pool, both addressed by
// their tier id) or the trash — one registry, one hover computation, for every kind.
internal sealed interface DragTarget {
    data class Tier(val tierId: Long) : DragTarget
    data object Trash : DragTarget
}

internal sealed interface DropOutcome {
    data class MoveTo(val itemId: Long, val toTierId: Long, val toPosition: Int) : DropOutcome
    data class Delete(val itemId: Long) : DropOutcome
}

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
    var hoveredTarget by mutableStateOf<DragTarget?>(null)
        private set

    val isDragging: Boolean get() = draggedPayload != null
    val hoveredTierId: Long? get() = (hoveredTarget as? DragTarget.Tier)?.tierId
    val isHoveringTrash: Boolean get() = hoveredTarget is DragTarget.Trash

    private val rowBounds = mutableStateMapOf<DragTarget, Rect>()

    // Pool only — it stays a single scrolling LazyRow, so its own layout state answers "what's here".
    private val itemsRowInfo = mutableStateMapOf<Long, ItemsRowInfo>()

    // Ranked tiers only — a FlowRow has no layout state of its own, so each tile reports itself.
    private val tileBounds = mutableStateMapOf<Long, TileRecord>()

    fun registerRowBounds(tierId: Long, bounds: Rect) {
        rowBounds[DragTarget.Tier(tierId)] = bounds
    }

    fun registerTrashBounds(bounds: Rect) {
        rowBounds[DragTarget.Trash] = bounds
    }

    // So the trash can anchor itself above a tier's actual measured position
    // (already inset-aware, since that tier registered it) instead of a guess.
    fun tierBounds(tierId: Long): Rect? = rowBounds[DragTarget.Tier(tierId)]

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

    // null means the pointer was released outside every target.
    fun endDrag(): DropOutcome? {
        val drop = computeDrop()
        clear()
        return drop
    }

    fun cancelDrag() {
        clear()
    }

    private fun clear() {
        draggedPayload = null
        hoveredTarget = null
    }

    private fun recomputeHover() {
        val position = pointerPositionInRoot
        // The trash floats above the pool, so their rects can overlap; it must win
        // outright rather than being picked by map iteration order — one target, not two.
        hoveredTarget = if (rowBounds[DragTarget.Trash]?.contains(position) == true) {
            DragTarget.Trash
        } else {
            rowBounds.entries.firstOrNull { (target, rect) -> target is DragTarget.Tier && rect.contains(position) }?.key
        }
    }

    private fun computeDrop(): DropOutcome? {
        val payload = draggedPayload ?: return null
        return when (val target = hoveredTarget) {
            is DragTarget.Trash -> DropOutcome.Delete(payload.itemId)
            is DragTarget.Tier -> computeMove(payload, target.tierId)
            null -> null
        }
    }

    private fun computeMove(payload: DragPayload, tierId: Long): DropOutcome.MoveTo? {
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
        return DropOutcome.MoveTo(payload.itemId, tierId, index)
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
