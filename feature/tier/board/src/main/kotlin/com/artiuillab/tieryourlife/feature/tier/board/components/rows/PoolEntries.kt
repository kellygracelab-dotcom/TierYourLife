package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.common.dashedBorder
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

/** One place in the pool's lazy row or grid: a card, or the slot of the card hovering over it. */
internal sealed interface PoolEntry {
    val key: Any

    /** [index] is the card's place in the pool, which the grids report its bounds by. */
    data class Card(val item: TierItem, val index: Int) : PoolEntry {
        override val key: Any get() = item.id
    }

    data object Slot : PoolEntry {
        override val key: Any = "pool-slot"
    }
}

/**
 * The pool as the rows draw themselves while a card hovers: its slot takes a
 * place among the cards, and the lifted card, if it is the pool's, takes no
 * room and stays composed for the gesture it carries. A grid gives every entry
 * a cell, so there it goes [last]; a row keeps it where it was.
 */
internal fun poolEntries(items: List<TierItem>, slot: Int?, liftedId: Long?, last: Boolean = true): List<PoolEntry> {
    val liftedIndex = if (liftedId == null) -1 else items.indexOfFirst { it.id == liftedId }
    val cells = CardCells(items.size, slot, liftedIndex)
    return buildList {
        for (cell in 0 until cells.count) {
            if (cells.isSlot(cell)) {
                add(PoolEntry.Slot)
            } else {
                val index = cells.itemIndex(cell)
                add(PoolEntry.Card(items[index], index))
            }
        }
        if (liftedIndex >= 0 && cells.cellOf(liftedIndex) == null) {
            val next = indexOfFirst { it is PoolEntry.Card && it.index == liftedIndex + 1 }
            add(if (last || next < 0) size else next, PoolEntry.Card(items[liftedIndex], liftedIndex))
        }
    }
}

/**
 * A lazy list follows its first card when the cards move: a slot put before it
 * would be drawn out of sight, and the lifted card going last would carry the
 * view to the end. [hold] asks for the place it is at instead, whenever the
 * order drawn changes.
 */
@Composable
internal fun HoldPlaceWhileReordering(entries: List<PoolEntry>, hold: () -> Unit) {
    val keys = entries.map { it.key }
    val drawn = remember { arrayOfNulls<List<Any>>(1) }
    SideEffect {
        if (drawn[0] != null && drawn[0] != keys) hold()
        drawn[0] = keys
    }
}

/** Where the lifted card would land, as the rows and the pool draw it. */
@Composable
internal fun CardSlot(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width, height)
            .dashedBorder(width = 2.dp, color = MaterialTheme.colorScheme.primary, cornerRadius = 6.dp)
            .testTag(TierDetailTestTags.DROP_SLOT),
    )
}
