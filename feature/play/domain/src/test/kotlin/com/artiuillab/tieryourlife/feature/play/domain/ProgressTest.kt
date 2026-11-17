package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test
    fun theEstimates_areTheOnesTheDesignQuotes() {
        assertEquals(136, estimatedDuels(GameKind.Swiss, 34))
        assertEquals(33, estimatedDuels(GameKind.Hill, 34))
        assertEquals(33, estimatedDuels(GameKind.Bracket, 34))
        assertEquals(9, estimatedMinutes(136))
        assertEquals(2, estimatedMinutes(33))
        assertEquals(1, estimatedMinutes(2))
    }

    @Test
    fun aSwissGame_saysWhichRoundItIsIn() {
        val spec = GameSpec(GameKind.Swiss, 6, seed = 1)
        assertEquals(1, progress(spec, emptyList()).round)
        val history = mutableListOf<Outcome>()
        repeat(3) {
            val (first, second) = nextPair(spec, history)!!
            history += Outcome(first, second, winner = first)
        }
        assertEquals(2, progress(spec, history).round)
        assertEquals(3, progress(spec, history).played)
    }

    @Test
    fun refining_countsItsOwnDuels_afterTheMainGame() {
        val spec = GameSpec(GameKind.Hill, 5, seed = 3)
        val history = mutableListOf<Outcome>()
        while (true) {
            val (first, second) = nextPair(spec, history) ?: break
            history += Outcome(first, second, winner = second)
        }
        val before = progress(spec, history)
        assertTrue(before.mainOver)
        assertEquals(0, before.refineTotal)

        val refining = spec.copy(refine = true)
        val total = refineDuels(spec.count, history)
        assertEquals(total, progress(refining, history).refineTotal)
        val (first, second) = nextPair(refining, history)!!
        history += Outcome(first, second, winner = first)
        assertEquals(1, progress(refining, history).refineAsked)
        assertEquals(4, progress(refining, history).played)
        assertFalse(total == 0)
    }

    @Test
    fun smart_isAboutTwoAndAHalfDuelsACard_neverMoreThanLosersStay() {
        assertEquals(4, estimatedDuels(GameKind.Smart, 3))
        assertEquals(15, estimatedDuels(GameKind.Smart, 7))
        assertEquals(100, estimatedDuels(GameKind.Smart, 40))
        assertEquals(375, estimatedDuels(GameKind.Smart, 150))
    }

    @Test
    fun smart_plansWhatItWillAsk_andKeepsNoRounds() {
        val spec = GameSpec(GameKind.Smart, 6, seed = 1, tiers = 3)
        val history = listOf(Outcome(0, 1, winner = 0), Outcome(2, 3, winner = null))

        val progress = progress(spec, history)

        assertEquals(2, progress.played)
        assertEquals("the first round of three and the second", 6, progress.planned)
        assertEquals(0, progress.round)
    }

    @Test
    fun theWholeGamesCount_atTheStart_isWhatThePickerPromises() {
        for (kind in GameKind.entries) {
            for (count in listOf(3, 6, 25, 34, 91)) {
                val spec = GameSpec(kind, count, seed = 7, tiers = 5)
                assertEquals("$kind over $count", estimatedDuels(kind, count), expectedDuels(spec, emptyList()))
            }
        }
    }

    @Test
    fun duringAGame_theCountIsOfTheWholeGame_andNoDuelPassesIt() {
        for (kind in GameKind.entries) {
            val spec = GameSpec(kind, 25, seed = 3, tiers = 5)
            val history = mutableListOf<Outcome>()
            val totals = mutableSetOf<Int>()
            while (true) {
                val (first, second) = nextPair(spec, history) ?: break
                val total = expectedDuels(spec, history)
                assertTrue("$kind: duel ${history.size + 1} of $total", history.size + 1 <= total)
                assertTrue("$kind never promises fewer than the picker", total >= estimatedDuels(kind, 25))
                totals += total
                // The better card is the one listed first in the deck; a few close calls go the other way.
                history += Outcome(first, second, winner = if (history.size % 7 == 3) maxOf(first, second) else minOf(first, second))
            }
            assertEquals(history.size, expectedDuels(spec, history))
            if (!kind.isRough) assertEquals("$kind keeps one exact count", setOf(estimatedDuels(kind, 25)), totals)
        }
    }

    @Test
    fun smart_overTwentyFiveCards_saysSixtyThree_notItsFirstRound() {
        val spec = GameSpec(GameKind.Smart, 25, seed = 1, tiers = 5)
        assertEquals(24, progress(spec, emptyList()).planned)
        assertEquals(63, expectedDuels(spec, emptyList()))
    }
}
