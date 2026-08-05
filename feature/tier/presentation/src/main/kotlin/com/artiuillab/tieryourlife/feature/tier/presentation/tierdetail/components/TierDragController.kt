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

// A tier row's own drag: reorder among the other ranked tiers, or delete via the same
// trash a poster uses. Never a move "into" another tier — tiers don't nest.
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

    // A tier row being dragged, separate from draggedPayload (an item) above — the two
    // never happen at once, since one starts from a tile and the other from a band, but
    // they share this same controller instance, hoveredTarget and the trash's bounds
    // rather than each tracking their own, so there is exactly one drag state per screen.
    var draggedTierId by mutableStateOf<Long?>(null)
        private set
    var tierPointerPositionInRoot by mutableStateOf(Offset.Zero)
        private set
    private var rankedTierIdsAtDragStart: List<Long> = emptyList()

    val isDragging: Boolean get() = draggedPayload != null || draggedTierId != null
    val isDraggingTier: Boolean get() = draggedTierId != null
    val hoveredTierId: Long? get() = (hoveredTarget as? DragTarget.Tier)?.tierId
    val isHoveringTrash: Boolean get() = hoveredTarget is DragTarget.Trash

    private val rowBounds = mutableStateMapOf<DragTarget, Rect>()

    // Pool only — it stays a single scrolling LazyRow, so its own layout state answers "what's here".
    private val itemsRowInfo = mutableStateMapOf<Long, ItemsRowInfo>()

    // Ranked tiers only — a FlowRow has no layout state of its own, so each tile reports itself.
    private val tileBounds = mutableStateMapOf<Long, TileRecord>()

    // The second layer: whatever the screen is actually showing right now, refreshed every
    // recomposition (see setValidTargets). Registration below only ever adds bounds and
    // never sees a tier or item get deleted — a row/tile that leaves composition is
    // supposed to unregister itself (see the unregister* functions), but nothing here
    // should have to trust that happened. A target whose id isn't in these sets is never
    // chosen, whatever its bounds map still says.
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

    // So the trash can anchor itself above a tier's actual measured position
    // (already inset-aware, since that tier registered it) instead of a guess.
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

    // Called every recomposition (see TierScreenBody) with the TierList actually on
    // screen. Plain fields, not Compose state: nothing observes them reactively, they're
    // only read inside a drag's own hover/drop computation, always against whatever was
    // freshest the last time the screen recomposed.
    fun setValidTargets(tierIds: Collection<Long>, itemIds: Collection<Long>) {
        validTierIds = tierIds.toSet()
        validItemIds = itemIds.toSet()
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
            rowBounds.entries.firstOrNull { (target, rect) ->
                target is DragTarget.Tier && target.tierId in validTierIds && rect.contains(position)
            }?.key
        }
    }

    // rankedTierIds is the current order, pool excluded — the pool never takes part in
    // reordering, and passing only ranked ids keeps it out of the candidate set entirely
    // rather than filtering it back out at every hover recomputation.
    fun beginTierDrag(tierId: Long, rankedTierIds: List<Long>, rootPosition: Offset) {
        draggedTierId = tierId
        rankedTierIdsAtDragStart = rankedTierIds
        tierPointerPositionInRoot = rootPosition
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

        // Every *other* ranked row keeps reporting its own (now collapsed, uniform-height)
        // bounds throughout the drag via the same registerRowBounds every row already
        // calls unconditionally — so the insertion point is just "which of those bounds
        // is the pointer's Y still above", the same midpoint-comparison idea insertionIndexInRow
        // already uses for tiles, just against registered rects instead of lazy layout info.
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
            insertionIndexInRow(state, pointerPositionInRoot.x - bounds.left, excludedIndex)
        } else {
            val tiles = tileBounds.entries
                .filter { (itemId, record) -> record.tierId == tierId && itemId in validItemIds }
                .map { it.value }
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
