package com.artiuillab.tieryourlife.feature.community.presentation.list

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenTimingTest {

    private var clock = 0L
    private val timing = OpenTiming { clock }

    @Test
    fun aSlowOpen_saysWhatTheWaitWasSpentOn_once() = runBlocking {
        timing.phase("server") { clock += 6_200 }
        timing.phase("own copy") { clock += 300 }

        assertEquals("List open took 6500 ms: server 6200 ms, own copy 300 ms", timing.shown())
        assertNull(timing.shown())
    }

    @Test
    fun aQuickOpen_saysNothing() = runBlocking {
        timing.phase("server") { clock += 1_200 }

        assertNull(timing.shown())
    }

    private fun failAfter(millis: Long): Nothing {
        clock += millis
        error("offline")
    }

    @Test
    fun aPhaseThatFails_isStillCounted() = runBlocking {
        runCatching { timing.phase("server") { failAfter(7_000) } }

        assertEquals("List open took 7000 ms: server 7000 ms", timing.shown())
    }
}
