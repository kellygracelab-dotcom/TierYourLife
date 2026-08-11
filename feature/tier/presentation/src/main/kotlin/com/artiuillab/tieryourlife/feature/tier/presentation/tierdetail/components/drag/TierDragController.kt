package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.drag

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
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

internal sealed interface DragTarget {
    data class Tier(val tierId: Long) : DragTarget
    data object Trash : DragTarget
}

internal sealed interface DropOutcome {
    data class MoveTo(val itemId: Long, val toTierId: Long, val toPosition: Int) : DropOutcome
    data class Delete(val itemId: Long) : DropOutcome
}

internal sealed interface TierDropOutcome {
    data class Reorder(val orderedTierIds: List<Long>) : TierDropOutcome
    data class Delete(val tierId: Long) : TierDropOutcome
}

private data class ItemsRowInfo(
    val bounds: Rect? = null,
    val state: LazyListState? = null,
    val itemIds: List<Long> = emptyList(),
)

private data class TileRecord(val tierId: Long, val index: Int, val bounds: Rect)

// Keeps drag geometry outside composables and ignores bounds for stale item IDs.
@Stable
internal class TierDragController {
    var draggedPayload by mutableStateOf<DragPayload?>(null)
        private set
    var pointerPositionInRoot by mutableStateOf(Offset.Zero)
        private set
    var hoveredTarget by mutableStateOf<DragTarget?>(null)
        private set

    var draggedTierId by mutableStateOf<Long?>(null)
        private set
    var tierPointerPositionInRoot by mutableStateOf(Offset.Zero)
        private set

    var draggedTierBandWidthPx by mutableFloatStateOf(0f)
        private set
    private var rankedTierIdsAtDragStart: List<Long> = emptyList()

    val isDragging: Boolean get() = draggedPayload != null || draggedTierId != null
    val isDraggingTier: Boolean get() = draggedTierId != null
    val hoveredTierId: Long? get() = (hoveredTarget as? DragTarget.Tier)?.tierId
    val isHoveringTrash: Boolean get() = hoveredTarget is DragTarget.Trash

    private val rowBounds = mutableStateMapOf<DragTarget, Rect>()

    private val itemsRowInfo = mutableStateMapOf<Long, ItemsRowInfo>()

    private val tileBounds = mutableStateMapOf<Long, TileRecord>()

    private var rightToLeft: Boolean = false
    private var validTierIds: Set<Long> = emptySet()
    private var validItemIds: Set<Long> = emptySet()

    fun registerRowBounds(tierId: Long, bounds: Rect) {
        rowBounds[DragTarget.Tier(tierId)] = bounds
    }

    fun unregisterRowBounds(tierId: Long) {
        rowBounds.remove(DragTarget.Tier(tierId))
    }

    fun registerTrashBounds(bounds: Rect) {
        rowBounds[DragTarget.Trash] = bounds
    }

    fun unregisterTrashBounds() {
        rowBounds.remove(DragTarget.Trash)
    }

    fun tierBounds(tierId: Long): Rect? = rowBounds[DragTarget.Tier(tierId)]

    fun registerItemsRowBounds(tierId: Long, bounds: Rect) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(bounds = bounds)
    }

    fun registerItemsRowMeta(tierId: Long, state: LazyListState, itemIds: List<Long>) {
        itemsRowInfo[tierId] = (itemsRowInfo[tierId] ?: ItemsRowInfo()).copy(state = state, itemIds = itemIds)
    }

    fun unregisterItemsRow(tierId: Long) {
        itemsRowInfo.remove(tierId)
    }

    fun registerTileBounds(tierId: Long, itemId: Long, index: Int, bounds: Rect) {
        tileBounds[itemId] = TileRecord(tierId, index, bounds)
    }

    fun unregisterTileBounds(itemId: Long) {
        tileBounds.remove(itemId)
    }

    fun setValidTargets(
        tierIds: Collection<Long>,
        itemIds: Collection<Long>,
        rightToLeft: Boolean = false,
    ) {
        validTierIds = tierIds.toSet()
        validItemIds = itemIds.toSet()
        this.rightToLeft = rightToLeft
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
        hoveredTarget = if (rowBounds[DragTarget.Trash]?.contains(position) == true) {
            DragTarget.Trash
        } else {
            rowBounds.entries.firstOrNull { (target, rect) ->
                target is DragTarget.Tier && target.tierId in validTierIds && rect.contains(position)
            }?.key
        }
    }

    fun beginTierDrag(tierId: Long, rankedTierIds: List<Long>, rootPosition: Offset, bandWidthPx: Float) {
        draggedTierId = tierId
        rankedTierIdsAtDragStart = rankedTierIds
        tierPointerPositionInRoot = rootPosition
        draggedTierBandWidthPx = bandWidthPx
        recomputeTierHover()
    }

    fun updateTierDrag(delta: Offset) {
        if (draggedTierId == null) return
        tierPointerPositionInRoot += delta
        recomputeTierHover()
    }

    fun endTierDrag(): TierDropOutcome? {
        val drop = computeTierDrop()
        clearTierDrag()
        return drop
    }

    fun cancelTierDrag() {
        clearTierDrag()
    }

    private fun clearTierDrag() {
        draggedTierId = null
        hoveredTarget = null
        rankedTierIdsAtDragStart = emptyList()
        draggedTierBandWidthPx = 0f
    }

    private fun recomputeTierHover() {
        val position = tierPointerPositionInRoot
        hoveredTarget = if (rowBounds[DragTarget.Trash]?.contains(position) == true) {
            DragTarget.Trash
        } else {
            null
        }
    }

    private fun computeTierDrop(): TierDropOutcome? {
        val tierId = draggedTierId ?: return null
        if (hoveredTarget is DragTarget.Trash) return TierDropOutcome.Delete(tierId)

        val others = rankedTierIdsAtDragStart
            .filter { it != tierId && it in validTierIds }
            .mapNotNull { id -> rowBounds[DragTarget.Tier(id)]?.let { id to it } }
            .sortedBy { (_, rect) -> rect.top }
        val pointerY = tierPointerPositionInRoot.y
        val insertAt = others.indexOfFirst { (_, rect) -> pointerY < (rect.top + rect.bottom) / 2f }
            .let { if (it < 0) others.size else it }
        val newOrder = others.map { it.first }.toMutableList()
        newOrder.add(insertAt.coerceIn(0, newOrder.size), tierId)
        return TierDropOutcome.Reorder(newOrder)
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
            val fromStart = if (rightToLeft) {
                bounds.right - pointerPositionInRoot.x
            } else {
                pointerPositionInRoot.x - bounds.left
            }
            insertionIndexInRow(state, fromStart, excludedIndex)
        } else {
            val tiles = tileBounds.entries
                .filter { (itemId, record) -> record.tierId == tierId && itemId in validItemIds }
                .map { it.value }
            val excludedIndex = tileBounds[payload.itemId]?.takeIf { it.tierId == tierId }?.index
            insertionIndexInGrid(tiles, pointerPositionInRoot, excludedIndex, rightToLeft)
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

private fun insertionIndexInGrid(
    tiles: List<TileRecord>,
    pointer: Offset,
    excludedFullIndex: Int?,
    rightToLeft: Boolean,
): Int {
    val visible = tiles.filter { it.index != excludedFullIndex }
    if (visible.isEmpty()) return 0

    val lines = visible.groupBy { it.bounds.top }.toSortedMap().values.toList()
    for (line in lines) {
        val lineMidY = (line.minOf { it.bounds.top } + line.maxOf { it.bounds.bottom }) / 2f
        if (pointer.y < lineMidY) {
            return translate(columnIndex(line, pointer.x, rightToLeft), excludedFullIndex)
        }
    }

    return translate(columnIndex(lines.last(), pointer.x, rightToLeft), excludedFullIndex)
}

private fun columnIndex(line: List<TileRecord>, pointerX: Float, rightToLeft: Boolean): Int {
    val sorted = if (rightToLeft) {
        line.sortedByDescending { it.bounds.right }
    } else {
        line.sortedBy { it.bounds.left }
    }
    for (tile in sorted) {
        val midX = (tile.bounds.left + tile.bounds.right) / 2f
        val beforeThisTile = if (rightToLeft) pointerX > midX else pointerX < midX
        if (beforeThisTile) return tile.index
    }
    return sorted.last().index + 1
}

private fun translate(rawIndex: Int, excludedFullIndex: Int?): Int =
    if (excludedFullIndex != null && rawIndex > excludedFullIndex) rawIndex - 1 else rawIndex
