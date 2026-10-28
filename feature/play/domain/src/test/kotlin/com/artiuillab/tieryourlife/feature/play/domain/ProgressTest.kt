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
        assertEquals(5, estimatedMinutes(136))
        assertEquals(1, estimatedMinutes(33))
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
}
