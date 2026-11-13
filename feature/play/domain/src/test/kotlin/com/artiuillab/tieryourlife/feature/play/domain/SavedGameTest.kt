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
        assertEquals(listOf(50L, MISSING_TIER - 1), found.scope)
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
    fun everyMode_keepsItsExactCohortWhenNewCardsArrive_andRejectsMissingParticipants() {
        GameKind.entries.forEach { kind ->
            for (count in listOf(3, 16, 65, 91, 128, 257)) {
                val ids = (1L..count.toLong()).toList()
                val saved = game(kind, ids, listOf(Outcome(0, 1, 0)))
                assertEquals(Resumption.Ready(saved), saved.resumeOn(ids.reversed() + 9999L))
                assertEquals(Resumption.Changed, saved.resumeOn(ids.dropLast(1) + 9999L))
            }
        }
    }

    @Test
    fun aGameWhoseAreaLostATier_cannotResume_butARenamedOneStillCan() {
        val saved = game(GameKind.Swiss, listOf(1, 2, 3)).copy(scope = listOf(7L), keepTier = true)

        assertEquals(Resumption.Ready(saved), saved.resumeOn(listOf(1, 2, 3), tiers = listOf(7L, 8L)))
        assertEquals(Resumption.Changed, saved.resumeOn(listOf(1, 2, 3), tiers = listOf(8L)))
    }

    @Test
    fun aCorruptCohortCannotResume() {
        val saved = game(GameKind.Swiss, listOf(1, 2, 3))
        assertEquals(Resumption.Changed, saved.copy(ids = listOf(1, 1, 3)).resumeOn(listOf(1, 2, 3)))
        assertEquals(Resumption.Changed, saved.copy(spec = saved.spec.copy(count = 4)).resumeOn(listOf(1, 2, 3, 4)))
    }
}
