package com.artiuillab.tieryourlife.feature.tier.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** QA 484 N2: an edit reached the website only once the board was left. */
@OptIn(ExperimentalCoroutinesApi::class)
class SendWhenQuietTest {

    @Test
    fun anEdit_isSentOnceTheBoardHasBeenQuietAFewSeconds() = runTest {
        val changes = MutableSharedFlow<Unit>()
        val sent = mutableListOf<Long>()
        val job = launch { sendWhenQuiet(changes, quietMs = 4_000, longestMs = 20_000, now = { currentTime }) { sent += currentTime } }
        runCurrent()

        changes.emit(Unit)
        advanceTimeBy(2_000)
        changes.emit(Unit)
        advanceTimeBy(3_999)
        assertEquals(emptyList<Long>(), sent)
        advanceTimeBy(2)
        assertEquals(listOf(6_000L), sent)

        advanceTimeBy(60_000)
        assertEquals(1, sent.size)
        job.cancel()
    }

    @Test
    fun aStretchThatNeverStops_isSentAtTheLongestWait() = runTest {
        val changes = MutableSharedFlow<Unit>()
        val sent = mutableListOf<Long>()
        val job = launch { sendWhenQuiet(changes, quietMs = 4_000, longestMs = 20_000, now = { currentTime }) { sent += currentTime } }
        runCurrent()

        repeat(15) {
            changes.emit(Unit)
            advanceTimeBy(2_000)
        }

        assertEquals(listOf(20_000L), sent)
        job.cancel()
    }
}
