package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardArrangementTest {

    @Test
    fun theEngineReadsTiersTopToBottom_thenThePool_whereverThePoolIsListed() {
        val board = board(POOL to listOf(7L, 8L), 1L to listOf(1L, 2L), 2L to listOf(3L))

        assertEquals(listOf(1L, 2L, 3L, 7L, 8L), board.cardsIn(null).map { it.id })
        assertEquals(listOf(3L, 7L, 8L), board.cardsIn(listOf(POOL, 2L)).map { it.id })
    }

    @Test
    fun playedCardsGoWhereTheResultSays_andTheRestStayPut() {
        val board = board(1L to listOf(1L, 2L), 2L to listOf(3L, 4L), POOL to listOf(5L, 6L))

        val arranged = board.arrangedBy(mapOf(1L to listOf(6L, 3L), 2L to listOf(5L)))

        assertEquals(listOf(1L to listOf(1L, 2L, 6L, 3L), 2L to listOf(4L, 5L), POOL to emptyList()), arranged.ids())
    }

    @Test
    fun aCardOrTierTheBoardNoLongerHas_isPassedOver() {
        val board = board(1L to listOf(1L), POOL to listOf(2L))

        val arranged = board.arrangedBy(mapOf(1L to listOf(2L, 40L), 50L to listOf(1L)))

        assertEquals(listOf(1L to listOf(1L, 2L), POOL to emptyList()), arranged.ids())
    }

    @Test
    fun theMovesBetweenTwoBoards_turnOneIntoTheOther() {
        val from = board(1L to listOf(1L, 2L, 3L), 2L to listOf(4L), POOL to listOf(5L, 6L, 7L))
        val to = board(1L to listOf(6L, 1L), 2L to listOf(3L, 5L, 2L, 4L), POOL to listOf(7L))

        val moves = movesBetween(from, to)
        val replayed = moves.fold(from) { board, move -> board.withItemMoved(move.itemId, move.tierId, move.position) }

        assertEquals(to.ids(), replayed.ids())
    }

    @Test
    fun aCardAlreadyInItsPlace_isNotMoved() {
        val from = board(1L to listOf(1L, 2L), POOL to listOf(3L))
        val to = board(1L to listOf(1L, 2L, 3L), POOL to emptyList())

        assertEquals(listOf(Move(3L, 1L, 2)), movesBetween(from, to))
        assertTrue(movesBetween(from, from).isEmpty())
    }

    @Test
    fun undoingPutsEveryCardBack_keepingOnesAddedSince_andForgettingOnesDeleted() {
        val before = board(1L to listOf(1L, 2L), 2L to listOf(3L), POOL to listOf(4L))
        val now = board(1L to listOf(4L, 3L, 9L), 2L to listOf(1L), POOL to emptyList())

        val restored = now.restoredTo(before)

        assertEquals(listOf(1L to listOf(1L, 9L), 2L to listOf(3L), POOL to listOf(4L)), restored.ids())
    }
}
