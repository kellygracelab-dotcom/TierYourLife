package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class CrossClientLargeGameTest {
    @Test
    fun `Webs ninety one card game resumes at the same pair and returns the next answer`() {
        val raw = requireNotNull(javaClass.getResourceAsStream("/web-to-android-91.json")).bufferedReader().use { it.readText() }
        val game = SharedGameJson.decode(raw)
        assertNotNull(game)
        game!!
        assertEquals(91, game.ids.size)
        assertEquals(53, game.history.size)
        assertEquals(2, game.round)
        assertEquals(39 to 40, nextPair(game.spec, game.history))
        val history = game.history + Outcome(39, 40, 39)
        val returned = game.copy(history = history, round = progress(game.spec, history).round)
        assertEquals(43 to 44, nextPair(returned.spec, history))
        assertEquals(game.ids, returned.ids)
        assertEquals(returned, SharedGameJson.decode(SharedGameJson.encode(returned)))
        System.getenv("PLAY_CROSS_OUTPUT")?.let { File(it).writeText(SharedGameJson.encode(returned)) }
    }
}
