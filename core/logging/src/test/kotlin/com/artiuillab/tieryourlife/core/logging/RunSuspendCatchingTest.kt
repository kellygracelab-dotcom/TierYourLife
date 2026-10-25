package com.artiuillab.tieryourlife.core.logging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RunSuspendCatchingTest {

    @Test
    fun `an answer comes back as success`() {
        val result = runBlocking { runSuspendCatching { 42 } }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `an ordinary failure comes back as failure`() {
        val result = runBlocking { runSuspendCatching { throw IOException("offline") } }

        assertTrue(result.exceptionOrNull() is IOException)
    }

    // The case plain runCatching got wrong: the job was cancelled, and the code
    // after it carried on as if a request had failed.
    @Test
    fun `a cancelled call stays cancelled instead of becoming a failure`() = runBlocking {
        val reached = CompletableDeferred<Result<Unit>>()
        val job = launch {
            reached.complete(runSuspendCatching { awaitCancellation() })
        }
        yield()

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertTrue("nothing after the call should have run", !reached.isCompleted)
    }

    @Test
    fun `a cancellation thrown inside surfaces as is`() {
        val thrown = runCatching {
            runBlocking { runSuspendCatching { throw CancellationException("stop") } }
        }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
    }
}
