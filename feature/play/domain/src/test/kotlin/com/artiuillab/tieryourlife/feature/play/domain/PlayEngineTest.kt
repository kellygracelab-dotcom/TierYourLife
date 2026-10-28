package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayEngineTest {

    @Test
    fun theShuffleIsAPermutation_andTheSameSeedGivesTheSameOne() {
        val order = shuffled(20, seed = 11)
        assertEquals((0 until 20).toList(), order.sorted())
        assertEquals(order, shuffled(20, seed = 11))
    }

    @Test
    fun aChain_isRankedInItsOrder() {
        val standing = standing(4, listOf(Duel(0, 1), Duel(1, 2), Duel(2, 3)))
        assertEquals(listOf(0, 1, 2, 3), standing.order)
    }

    @Test
    fun anUnbeatenCard_hasAFiniteStrength() {
        val standing = standing(2, List(10) { Duel(1, 0) })
        assertTrue(standing.logStrength[1].isFinite())
        assertEquals(listOf(1, 0), standing.order)
    }

    @Test
    fun withNoDuels_everythingSitsInTheFirstTier_inTheOriginalOrder() {
        val tiers = tiers(standing(3, emptyList()), tierCount = 4)
        assertEquals(listOf(listOf(0, 1, 2), emptyList(), emptyList(), emptyList()), tiers)
    }

    @Test
    fun twoClearGroups_goToTheTopAndTheBottom_leavingTheMiddleEmpty() {
        val duels = (0..2).flatMap { top -> (3..5).map { bottom -> Duel(top, bottom) } }
        val tiers = tiers(standing(6, duels), tierCount = 5)
        assertEquals(setOf(0, 1, 2), tiers.first().toSet())
        assertEquals(setOf(3, 4, 5), tiers.last().toSet())
        assertTrue(tiers.subList(1, 4).all { it.isEmpty() })
    }

    @Test
    fun everyCardLandsInExactlyOneTier() {
        val history = play(gameCases.first { it.spec.kind == GameKind.Swiss && it.spec.count == 9 })
        val placed = result(GameSpec(GameKind.Swiss, 9, seed = 99, refine = true), history, tierCount = 5).flatten()
        assertEquals((0 until 9).toList(), placed.sorted())
    }

    @Test
    fun aHill_asksOneDuelPerChallenger_andTheWinnerStays() {
        val spec = GameSpec(GameKind.Hill, 5, seed = 2)
        val order = shuffled(5, seed = 2)
        val first = nextPair(spec, emptyList())
        assertEquals(order[0] to order[1], first)
        val second = nextPair(spec, listOf(Outcome(order[0], order[1], winner = order[1])))
        assertEquals(order[1] to order[2], second)
        assertEquals(4, play(GameCase("", spec, 5, "lowerWins")).size)
    }

    @Test
    fun aBracket_ofSeven_takesSixDuels() {
        assertEquals(6, play(GameCase("", GameSpec(GameKind.Bracket, 7, seed = 1), 5, "higherWins")).size)
    }

    @Test
    fun aSkip_isNoEvidence_butTheGameGoesOn() {
        val spec = GameSpec(GameKind.Hill, 3, seed = 4)
        val (a, b) = nextPair(spec, emptyList())!!
        val skipped = listOf(Outcome(a, b, winner = null))
        assertEquals(minOf(a, b), nextPair(spec, skipped)!!.first)
        assertTrue(skipped.duels().isEmpty())
    }

    @Test
    fun swiss_neverRepeatsAPairWhileANewOneIsFree() {
        val history = play(GameCase("", GameSpec(GameKind.Swiss, 8, seed = 8), 5, "lowerWins"))
        val pairs = history.map { setOf(it.first, it.second) }
        assertEquals(pairs.size, pairs.toSet().size)
        // A card the greedy pairing cannot place without a repeat sits that round out.
        assertTrue(history.size in swissRounds(8) * 3..swissRounds(8) * 4)
    }

    @Test
    fun swissRounds_areCeilLog2PlusTwo() {
        assertEquals(listOf(3, 4, 4, 5, 5, 8), listOf(2, 3, 4, 5, 8, 64).map(::swissRounds))
    }

    @Test
    fun refining_asksNeighboursWhoHaveNotMet_atMostHalfTheCards() {
        val spec = GameSpec(GameKind.Hill, 6, seed = 6)
        val main = play(GameCase("", spec, 5, "lowerWins"))
        val refined = play(GameCase("", spec.copy(refine = true), 5, "lowerWins"))
        val extra = refined.drop(main.size)
        assertTrue(extra.size in 1..3)
        val metBefore = main.map { setOf(it.first, it.second) }.toSet()
        assertTrue(extra.none { setOf(it.first, it.second) in metBefore })
        assertNull(nextPair(spec.copy(refine = true), refined))
    }

    @Test
    fun oneCard_isNoGame() {
        assertNull(nextPair(GameSpec(GameKind.Bracket, 1, seed = 0), emptyList()))
    }
}
