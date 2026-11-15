package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** What the pool draws while a card hovers over it, written as ids with the slot as "_". */
class PoolEntriesTest {

    private val items = List(5) { TierItem(id = it + 1L, title = "Card $it", imageUrl = null) }

    private fun drawn(slot: Int?, lifted: Long?, last: Boolean = true) =
        poolEntries(items, slot, lifted, last).joinToString(" ") { if (it is PoolEntry.Card) "${it.item.id}" else "_" }

    @Test
    fun nothingLifted_drawsTheCardsInOrder() {
        assertEquals("1 2 3 4 5", drawn(slot = null, lifted = null))
    }

    @Test
    fun aCardFromARow_hoveringThePool_opensASlotWhereItWouldLand() {
        assertEquals("1 2 _ 3 4 5", drawn(slot = 2, lifted = 99))
        assertEquals("1 2 3 4 5 _", drawn(slot = 5, lifted = 99))
    }

    @Test
    fun thePoolsOwnCard_takesNoPlace_andTheSlotCountsTheOthers() {
        assertEquals("1 3 4 _ 5 2", drawn(slot = 3, lifted = 2))
        assertEquals("_ 1 3 4 5 2", drawn(slot = 0, lifted = 2))
    }

    @Test
    fun inARow_theLiftedCardStaysWhereItWas() {
        assertEquals("1 2 3 4 _ 5", drawn(slot = 3, lifted = 2, last = false))
        assertEquals("_ 1 2 3 4 5", drawn(slot = 0, lifted = 2, last = false))
        assertEquals("1 2 3 4 _ 5", drawn(slot = 4, lifted = 5, last = false))
    }

    @Test
    fun liftedButOverAnotherRow_thePoolKeepsItsPlace() {
        assertEquals("1 2 3 4 5", drawn(slot = null, lifted = 2))
    }

    @Test
    fun theStripScrollsOnlyNearItsEnds_fasterTowardTheEdge() {
        assertEquals(0f, stripEdgeSpeed(fromLeft = 200f, fromRight = 200f, edge = 100f, fastest = 600f), 0f)
        assertEquals(-300f, stripEdgeSpeed(fromLeft = 50f, fromRight = 350f, edge = 100f, fastest = 600f), 0.01f)
        assertEquals(600f, stripEdgeSpeed(fromLeft = 400f, fromRight = 0f, edge = 100f, fastest = 600f), 0.01f)
    }
}
