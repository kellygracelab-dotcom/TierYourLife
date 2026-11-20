package com.artiuillab.tieryourlife.core.usage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class UsageReporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preferences = FakeAppPreferences()
    private val api = FakeApi()
    private var today = LocalDate.of(2026, 11, 20)
    private val firstDay = today.toEpochDay()
    private var clock = 0L

    private fun TestScope.reporter(first: Ledger = Ledger(firstDay = firstDay, stepsWaiting = setOf("first_launch"))) = UsageReporter(
        store = LedgerStore(context.getSharedPreferences("usage-test", Context.MODE_PRIVATE), first = { first }),
        preferences = preferences,
        api = api,
        install = UsageInstall("597"),
        today = { today },
        scope = this,
        now = { clock },
    )

    /** Long enough away for the next start to be a session of its own. */
    private fun away() {
        clock += 31 * 60 * 1000L
    }

    @Test
    fun aSession_goesUpWhenItEnds_once() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        usage.step(UsageStep.FirstBoard)
        usage.count(UsageFeature.BoardTier)

        usage.sessionEnded()
        advanceUntilIdle()
        usage.sessionEnded()
        advanceUntilIdle()

        val batch = api.sent.single()
        assertEquals("597", batch.version)
        assertEquals(31, batch.active!!.gap)
        assertEquals(listOf("first_launch", "first_board"), batch.steps)
        assertEquals(mapOf("session" to 1, "board.tier" to 1), batch.features)
        assertEquals(1, batch.sessions)
    }

    @Test
    fun aTurnedScreen_isTheSameSession() = runTest {
        val usage = reporter()

        usage.sessionStarted()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()

        assertEquals(1, api.sent.single().sessions)
    }

    @Test
    fun aShortTripOut_isTheSameSession() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        usage.sessionEnded()
        clock += 5 * 60 * 1000L

        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()

        assertEquals(1, api.sent.sumOf { it.sessions })
    }

    @Test
    fun offline_theCountsWait_andGoNextTime() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        api.offline = true
        usage.sessionEnded()
        advanceUntilIdle()

        api.offline = false
        away()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()

        val batch = api.sent.single()
        assertEquals(2, batch.sessions)
        assertEquals(listOf("first_launch"), batch.steps)
    }

    @Test
    fun theNextDay_isActiveAgain_withItsGap() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()

        today = today.plusDays(1)
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()

        assertEquals(UsageBatch.Active(gap = 1, sinceFirst = 1, firstWeek = "2026-W47"), api.sent.last().active)
        assertTrue(api.sent.last().steps.isEmpty())
    }

    @Test
    fun tooManyToday_nothingMoreUntilTomorrow() = runTest {
        val usage = reporter()
        api.answer = 429
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()
        api.answer = 204

        away()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()
        assertEquals(1, api.tries)

        today = today.plusDays(1)
        away()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()
        assertEquals(3, api.sent.single().sessions)
    }

    @Test
    fun turnedOff_nothingIsKeptOrSent_andWhatWaitedIsDropped() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        usage.count(UsageFeature.ShareImage)
        preferences.setSendUsage(false)
        usage.step(UsageStep.Shared)
        usage.count(UsageFeature.ShareLink)

        usage.sessionEnded()
        advanceUntilIdle()
        assertEquals(0, api.tries)

        preferences.setSendUsage(true)
        usage.step(UsageStep.Shared)
        away()
        usage.sessionStarted()
        usage.sessionEnded()
        advanceUntilIdle()
        val batch = api.sent.single()
        assertTrue(batch.steps.isEmpty())
        assertEquals(mapOf("session" to 1), batch.features)
    }

    // Android learns of a crash at the next start: the session that fell is counted then.
    @Test
    fun aCrash_goesWithTheNextStart() = runTest {
        val usage = reporter()
        usage.sessionStarted()
        val error = IllegalStateException("whatever the person typed")
        error.stackTrace = arrayOf(StackTraceElement("com.x.Board", "drop", "Board.kt", 7))
        usage.crashed(error)

        val next = reporter()
        next.send()
        advanceUntilIdle()

        val batch = api.sent.single()
        assertEquals(1, batch.sessions)
        assertEquals(1, batch.crashedSessions)
        assertEquals(listOf(UsageCrash("java.lang.IllegalStateException", "Board.kt:7 drop")), batch.crashes)
    }

    @Test
    fun aCrashBeforeAnyScreen_isASessionThatCrashed() = runTest {
        reporter().crashed(IllegalStateException())

        val next = reporter()
        next.send()
        advanceUntilIdle()

        assertEquals(1, api.sent.single().sessions)
        assertEquals(1, api.sent.single().crashedSessions)
    }

    @Test
    fun anInstallFromBeforeTheStatistics_sendsNoSteps() = runTest {
        val usage = reporter(first = Ledger(firstDay = firstDay - 40, stepsDone = UsageStep.entries.mapTo(mutableSetOf()) { it.wire }))
        usage.step(UsageStep.FirstBoard)
        usage.sessionStarted()

        usage.sessionEnded()
        advanceUntilIdle()

        val batch = api.sent.single()
        assertTrue(batch.steps.isEmpty())
        assertEquals(40, batch.active!!.sinceFirst)
        assertNull(api.sent.single().crashes.firstOrNull())
    }

    private class FakeApi : UsageApi {
        val sent = mutableListOf<UsageBatch>()
        var tries = 0
        var offline = false
        var answer = 204

        override suspend fun send(batch: UsageBatch): Response<Unit> {
            tries++
            if (offline) throw IOException("offline")
            if (answer != 204) return Response.error(answer, "{}".toResponseBody())
            sent += batch
            return Response.success(204, Unit)
        }
    }
}
