package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameTest {

    private fun game(kind: GameKind, ids: List<Long>, history: List<Outcome> = emptyList()) = SavedGame(
        spec = GameSpec(kind, ids.size, seed = 5),
        ids = ids,
        history = history,
        round = 1,
        startedAt = 0,
    )

    @Test
    fun aGameIsKeptForThirtyDays_andNotADayMore() {
        val day = 24 * 60 * 60 * 1000L
        val saved = game(GameKind.Swiss, listOf(1, 2, 3))
        assertFalse(saved.isExpired(now = 30 * day))
        assertTrue(saved.isExpired(now = 30 * day + 1))
    }

    @Test
    fun overTheSameCards_everyGameGoesOnAsItWas() {
        GameKind.entries.forEach { kind ->
            val saved = game(kind, listOf(10, 20, 30), listOf(Outcome(0, 1, winner = 1)))
            assertEquals(Resumption.Ready(saved), saved.resumeOn(listOf(30, 10, 20)))
        }
    }

    @Test
    fun aHillOrBracket_whoseCardsChanged_startsAgain() {
        listOf(GameKind.Hill, GameKind.Bracket).forEach { kind ->
            val saved = game(kind, listOf(10, 20, 30))
            assertEquals(Resumption.Changed, saved.resumeOn(listOf(10, 20)))
            assertEquals(Resumption.Changed, saved.resumeOn(listOf(10, 20, 30, 40)))
        }
    }

    @Test
    fun swiss_dropsWhatADeletedCardPlayed_andRenumbersTheRest() {
        val saved = game(
            GameKind.Swiss,
            listOf(10, 20, 30, 40),
            listOf(Outcome(0, 1, winner = 0), Outcome(2, 3, winner = 3), Outcome(1, 3, winner = null)),
        )

        val resumed = saved.resumeOn(listOf(10, 30, 40)) as Resumption.Ready

        assertEquals(listOf(10L, 30L, 40L), resumed.game.ids)
        assertEquals(3, resumed.game.spec.count)
        assertEquals(listOf(Outcome(1, 2, winner = 2)), resumed.game.history)
        assertEquals(0, resumed.joined)
    }

    @Test
    fun swiss_letsNewCardsInBehindTheOnesItHad() {
        val saved = game(GameKind.Swiss, listOf(10, 20, 30), listOf(Outcome(0, 1, winner = 1)))

        val resumed = saved.resumeOn(listOf(5, 10, 20, 30, 40)) as Resumption.Ready

        assertEquals(listOf(10L, 20L, 30L, 5L, 40L), resumed.game.ids)
        assertEquals(listOf(Outcome(0, 1, winner = 1)), resumed.game.history)
        assertEquals(2, resumed.joined)
    }

    @Test
    fun swiss_neverGrowsPastSixtyFourCards() {
        val ids = (1L..60L).toList()
        val saved = game(GameKind.Swiss, ids)

        val resumed = saved.resumeOn(ids + (100L..110L)) as Resumption.Ready

        assertEquals(MAX_PLAY_CARDS, resumed.game.ids.size)
        assertEquals(4, resumed.joined)
    }

    @Test
    fun swiss_withFewerThanTwoCardsLeft_startsAgain() {
        assertEquals(Resumption.Changed, game(GameKind.Swiss, listOf(1, 2, 3)).resumeOn(listOf(2)))
    }
}
