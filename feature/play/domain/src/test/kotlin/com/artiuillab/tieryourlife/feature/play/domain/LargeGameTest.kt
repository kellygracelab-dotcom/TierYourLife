package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class LargeGameTest {
    @Test
    fun oddAndLargeGamesFinishWithEveryParticipant_andResumeTheSameNextPair() {
        for (kind in GameKind.entries) {
            for (count in listOf(65, 91, 128, 129)) {
                val spec = GameSpec(kind, count, seed = 7, refine = true)
                val ids = (0 until count).map { "card-$it" }
                var history = emptyList<Outcome>()
                while (true) {
                    val pair = nextPair(spec, history) ?: break
                    assertTrue("$kind $count must terminate", history.size < count * 20)
                    if (history.size == count / 2) {
                        val wire = SharedGame(spec, ids, history, progress(spec, history).round, 1_800_000_000_000L)
                        val restored = SharedGameJson.decode(SharedGameJson.encode(wire))!!
                        assertEquals(ids, restored.ids)
                        assertEquals(history, restored.history)
                        assertEquals(pair, nextPair(restored.spec, restored.history))
                        history = restored.history
                    }
                    history = history + Outcome(pair.first, pair.second, pair.first)
                }
                assertNull(nextPair(spec, history))
                val result = result(spec, history, 5).flatten()
                assertEquals(count, result.size)
                assertEquals((0 until count).toSet(), result.toSet())
            }
        }
    }

    @Test
    fun maximumCloudBoardConstructionAndResultRemainBounded() {
        val elapsed = measureNanoTime {
            val spec = GameSpec(GameKind.Swiss, 2000, seed = 11)
            assertTrue(nextPair(spec, emptyList()) != null)
            val outcomes = List(MAX_SHARED_GAME_HISTORY) { at -> Outcome(at % 2000, (at + 1) % 2000, at % 2000) }
            val standing = standing(2000, outcomes.duels())
            val placed = tiers(standing, 19).flatten()
            assertEquals((0 until 2000).toSet(), placed.toSet())
            assertEquals(2000, placed.size)
        }
        println("2000-card construction + 14000-outcome standing + 19 tiers: ${elapsed / 1_000_000} ms")
        assertTrue("Large-game construction/result exceeded 30 seconds", elapsed < 30_000_000_000L)
    }
}
