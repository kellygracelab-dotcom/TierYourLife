package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardCellsTest {

    @Test
    fun withNothingLifted_eachCardHasItsOwnCell() {
        val cells = CardCells(cards = 4, slot = null, liftedIndex = -1)

        assertEquals(4, cells.count)
        assertEquals(listOf(0, 1, 2, 3), (0 until 4).map(cells::itemIndex))
    }

    @Test
    fun aCardFromAnotherRowHovering_putsItsSlotBeforeTheCardItWouldPrecede() {
        val cells = CardCells(cards = 4, slot = 1, liftedIndex = -1)

        assertEquals(5, cells.count)
        assertEquals(true, cells.isSlot(1))
        assertEquals(listOf(0, 2, 3, 4), (0 until 4).map { cells.cellOf(it) })
    }

    @Test
    fun aCardOfThisRowHovering_takesNoCellAndItsSlotCountsWithoutIt() {
        // Card 1 lifted and held over the place after card 2.
        val cells = CardCells(cards = 4, slot = 2, liftedIndex = 1)

        assertEquals(4, cells.count)
        assertNull(cells.cellOf(1))
        assertEquals(listOf(0, 2, -1, 3), (0 until 4).map { if (cells.isSlot(it)) -1 else cells.itemIndex(it) })
    }

    @Test
    fun aCardOfThisRowNotHovering_keepsItsPlace() {
        val cells = CardCells(cards = 3, slot = null, liftedIndex = 1)

        assertEquals(3, cells.count)
        assertEquals(1, cells.cellOf(1))
    }

    @Test
    fun aSlotPastTheEnd_isTheLastCell() {
        val cells = CardCells(cards = 2, slot = 9, liftedIndex = -1)

        assertEquals(3, cells.count)
        assertEquals(true, cells.isSlot(2))
    }

    @Test
    fun theGridBreaksLikeFlowRow_againstTheGapRoundedUp() {
        // 10px cards, a gap of 2.5px: placed 3px apart, broken against 3px.
        val grid = CellGrid(cellWidth = 10, cellHeight = 20, gap = 3, breakGap = 3)

        assertEquals(3, grid.perLine(36))
        assertEquals(2, grid.perLine(35))
        assertEquals(1, grid.perLine(5))
        assertEquals(2 * 20 + 3, grid.height(cells = 5, width = 36))
        assertEquals(0, grid.height(cells = 0, width = 36))
    }
}
