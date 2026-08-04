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

// Owns everything a drag gesture needs across tile, row and screen: the payload
// being carried, where the pointer is in root coordinates, which row it is
// currently over, and enough per-row layout info to turn that position into a
// repository-shaped (tierId, position) drop target. One instance is shared by
// every row and the pool panel for a single tier list screen.
@Stable
internal class TierDragController {
    var draggedPayload by mutableStateOf<DragPayload?>(null)
        private set
    var pointerPositionInRoot by mutableStateOf(Offset.Zero)
        private set
    var hoveredTierId by mutableStateOf<Long?>(null)
        private set

    val isDragging: Boolean get() = draggedPayload != null

    // Whole-row bounds (band + items), used to decide which row the pointer is over.
    private val rowBounds = mutableStateMapOf<Long, Rect>()

    // Items-row bounds/state/ids, used to turn a pointer position into an insertion index.
    private val itemsRowInfo = mutableStateMapOf<Long, ItemsRowInfo>()

    fun registerRowBounds(tierId: Long, bounds: Rect) {
        rowBounds[tierId] = bounds
    }

    fun registerItemsRowBounds(tierId: Long, bounds: Rect) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(bounds = bounds)
    }

    fun registerItemsRowMeta(tierId: Long, state: LazyListState, itemIds: List<Long>) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(state = state, itemIds = itemIds)
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

    // Returns the drop target if the pointer was released over an eligible row,
    // null if it was released over empty space (the caller should treat that as a cancel).
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
        val row = itemsRowInfo[tierId] ?: return null
        val bounds = row.bounds ?: return null
        val state = row.state ?: return null
        val localX = pointerPositionInRoot.x - bounds.left
        // The dragged tile stays composed (so its own gesture keeps running) but
        // renders empty-handed while lifted; exclude it here so the index we hand
        // to the repository is always "position within the list without it" —
        // moving it forward vs. backward inside the same row must land on the
        // exact index requested, not one off depending on direction.
        val excludedIndex = row.itemIds.indexOf(payload.itemId).takeIf { it >= 0 }
        val index = insertionIndex(state, localX, excludedIndex)
        return DropTarget(payload.itemId, tierId, index)
    }
}

private fun insertionIndex(state: LazyListState, localX: Float, excludedFullIndex: Int?): Int {
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

private fun translate(rawIndex: Int, excludedFullIndex: Int?): Int =
    if (excludedFullIndex != null && rawIndex > excludedFullIndex) rawIndex - 1 else rawIndex
