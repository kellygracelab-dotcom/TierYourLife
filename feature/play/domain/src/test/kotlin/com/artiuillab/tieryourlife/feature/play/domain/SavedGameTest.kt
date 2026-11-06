package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun theMonth_runsFromTheLastWrite_notFromTheStart() {
        val day = 24 * 60 * 60 * 1000L
        val playedOn = game(GameKind.Swiss, listOf(1, 2, 3)).copy(touchedAt = 20 * day)

        assertFalse(playedOn.isExpired(now = 45 * day))
        assertTrue(playedOn.isExpired(now = 50 * day + 1))
    }

    @Test
    fun aCopyThatKeptItsUids_findsItsCardsAndTiersOnRowsWrittenAfresh() {
        val saved = game(GameKind.Swiss, listOf(10, 20, 30)).copy(
            scope = listOf(5, 6),
            uids = listOf("a", "b", "c"),
            scopeUids = listOf("t-s", "t-a"),
        )
        val rows = mapOf("a" to 110L, "c" to 130L)

        val found = saved.foundBy(rows::get, mapOf("t-s" to 50L)::get)

        assertEquals(listOf(110L, -2L, 130L), found.ids)
        assertEquals(listOf(50L), found.scope)
        assertEquals(saved.history, found.history)
    }

    @Test
    fun aCopyWithoutUids_staysAsItIs() {
        val saved = game(GameKind.Hill, listOf(10, 20, 30)).copy(scope = listOf(5))

        assertEquals(saved, saved.foundBy({ 1L }, { 1L }))
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
    fun swiss_renumbered_dropsTheUidsOfTheOldOrder() {
        val saved = game(GameKind.Swiss, listOf(10, 20, 30), listOf(Outcome(0, 1, winner = 1)))
            .copy(uids = listOf("a", "b", "c"), scopeUids = emptyList())

        val resumed = saved.resumeOn(listOf(10, 30, 40)) as Resumption.Ready

        assertNull(resumed.game.uids)
        assertNull(resumed.game.scopeUids)
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
