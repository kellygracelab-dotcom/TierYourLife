package com.artiuillab.tieryourlife.feature.tier.board.components.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.floor

/** The cells stay put while the cards make room for the insertion slot. */
internal data class WrappedDropGrid(
    val bounds: Rect,
    val cellWidth: Int,
    val cellHeight: Int,
    val gap: Int,
    val columns: Int,
    val itemIds: Set<Long>,
) {
    fun insertionIndex(pointer: Offset, liftedId: Long, rightToLeft: Boolean): Int {
        val x = if (rightToLeft) bounds.right - pointer.x else pointer.x - bounds.left
        val y = pointer.y - bounds.top
        val row = floor((y + gap / 2f) / (cellHeight + gap)).toInt().coerceAtLeast(0)
        val column = floor((x + cellWidth / 2f) / (cellWidth + gap)).toInt().coerceIn(0, columns)
        val count = itemIds.size - if (liftedId in itemIds) 1 else 0
        return (row * columns + column).coerceIn(0, count)
    }
}
