package com.artiuillab.tieryourlife.core.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** The batch as proxy CONTRACT.md, Usage statistics, reads it. */
class UsageBatchTest {

    private val today = LocalDate.of(2026, 11, 20)
    private val firstDay = LocalDate.of(2026, 11, 11).toEpochDay()

    @Test
    fun theFirstDayEver_isGap31_withTheFirstLaunch() {
        val batch = batchOf(Ledger(firstDay = today.toEpochDay(), stepsWaiting = setOf("first_launch")), today, "597", "uk-UA")!!

        assertEquals(UsageBatch.Active(gap = 31, sinceFirst = 0, firstWeek = "2026-W47"), batch.active)
        assertEquals(listOf("first_launch"), batch.steps)
        assertEquals("uk-UA", batch.locale)
    }

    @Test
    fun aReturn_saysTheDaysSinceTheLastAndSinceTheFirst() {
        val batch = batchOf(Ledger(firstDay = firstDay, lastActiveDay = today.toEpochDay() - 3), today, "597", null)!!

        assertEquals(UsageBatch.Active(gap = 3, sinceFirst = 9, firstWeek = "2026-W46"), batch.active)
    }

    @Test
    fun aGapLongerThanThirtyDays_is31() {
        val batch = batchOf(Ledger(firstDay = firstDay - 100, lastActiveDay = today.toEpochDay() - 45), today, "597", null)!!

        assertEquals(31, batch.active!!.gap)
    }

    @Test
    fun activeGoesOnceADay() {
        val ledger = Ledger(firstDay = firstDay, lastActiveDay = today.toEpochDay(), counts = mapOf("share.image" to 1))

        val batch = batchOf(ledger, today, "597", null)!!

        assertNull(batch.active)
        assertEquals(mapOf("share.image" to 1), batch.features)
    }

    @Test
    fun nothingNew_sendsNothing() {
        assertNull(batchOf(Ledger(firstDay = firstDay, lastActiveDay = today.toEpochDay()), today, "597", null))
    }

    @Test
    fun stepsGoInTheContractsOrder() {
        val ledger = Ledger(firstDay = firstDay, lastActiveDay = today.toEpochDay(), stepsWaiting = setOf("published", "first_board", "open_list"))

        assertEquals(listOf("open_list", "first_board", "published"), batchOf(ledger, today, "597", null)!!.steps)
    }

    @Test
    fun aCountPastWhatOneBatchCarries_waitsForTheNext() {
        val ledger = Ledger(firstDay = firstDay, lastActiveDay = today.toEpochDay(), counts = mapOf("session" to 250))

        val batch = batchOf(ledger, today, "597", null)!!

        assertEquals(200, batch.features.getValue("session"))
        assertEquals(mapOf("session" to 50), ledger.after(batch, today).counts)
    }

    @Test
    fun whatWasRecordedDuringTheSend_staysForTheNextBatch() {
        val ledger = Ledger(firstDay = firstDay, stepsWaiting = setOf("first_launch"), counts = mapOf("share.image" to 1), sessions = 1)
        val batch = batchOf(ledger, today, "597", null)!!
        val meanwhile = ledger.counted("share.image").copy(sessions = 2).stepped(UsageStep.FirstBoard, on = true)

        val left = meanwhile.after(batch, today)

        assertEquals(today.toEpochDay(), left.lastActiveDay)
        assertEquals(mapOf("share.image" to 1), left.counts)
        assertEquals(1, left.sessions)
        assertEquals(setOf("first_board"), left.stepsWaiting)
        assertEquals(setOf("first_launch"), left.stepsDone)
    }

    @Test
    fun aStepIsSentOncePerInstall() {
        val sent = Ledger(firstDay = firstDay, stepsDone = setOf("first_board"))

        assertEquals(sent, sent.stepped(UsageStep.FirstBoard, on = true))
    }

    @Test
    fun aStepTakenWhileOff_isSpent_andNeverSentLater() {
        val off = Ledger(firstDay = firstDay).stepped(UsageStep.FirstBoard, on = false)

        assertTrue(off.stepsWaiting.isEmpty())
        assertEquals(off, off.stepped(UsageStep.FirstBoard, on = true))
    }

    @Test
    fun turnedOff_dropsWhatWaited_butKeepsTheDays() {
        val ledger = Ledger(
            firstDay = firstDay,
            lastActiveDay = firstDay + 2,
            stepsWaiting = setOf("first_card"),
            counts = mapOf("session" to 3),
            sessions = 3,
            crashedSessions = 1,
            crashes = listOf(UsageCrash("java.lang.IllegalStateException", "Board.kt:1 x")),
        )

        assertEquals(Ledger(firstDay = firstDay, lastActiveDay = firstDay + 2, stepsDone = setOf("first_card")), ledger.dropped())
    }

    @Test
    fun theWeekOfANewYear_isTheIsoOne() {
        assertEquals("2026-W53", isoWeekOf(LocalDate.of(2027, 1, 1)))
        assertEquals("2026-W01", isoWeekOf(LocalDate.of(2025, 12, 29)))
    }

    @Test
    fun aDay_isTheUtcDay_whateverTheLocalClockSays() {
        // 00:30 on the 21st in Kyiv is still the 20th in UTC; 23:30 UTC is the 20th even where it is the 21st.
        assertEquals(LocalDate.of(2026, 11, 20), utcDay(OffsetDateTime.parse("2026-11-21T00:30:00+02:00").toInstant().toEpochMilli()))
        assertEquals(LocalDate.of(2026, 11, 20), utcDay(Instant.parse("2026-11-20T23:30:00Z").toEpochMilli()))
        assertEquals(LocalDate.of(2026, 11, 21), utcDay(Instant.parse("2026-11-21T00:00:00Z").toEpochMilli()))
    }

    @Test
    fun featureNames_areTheContracts() {
        assertEquals("game.losers.done", UsageFeature.game(GameMode.Losers, done = true).wire)
        assertEquals("picture.pick.tmdb", UsageFeature.pickedFrom("TMDB").wire)
        assertEquals("picture.pick.other", UsageFeature.pickedFrom("flickr").wire)
        assertEquals("picture.pick.other", UsageFeature.pickedFrom(null).wire)
        assertEquals(listOf("1", "2-3", "2-3", "4-10", "4-10", "11+"), listOf(0, 1, 2, 3, 9, 10).map { UsageFeature.pickedAt(it).wire.substringAfterLast('.') })
    }

    @Test
    fun aCrash_isItsRootCausesTypeAndTopLine_withoutTheMessage() {
        val root = IllegalStateException("Ann's secret board")
        root.stackTrace = arrayOf(StackTraceElement("com.x.BoardScreen", "render", "BoardScreen.kt", 120))
        val crash = crashOf(RuntimeException("wrapper", root))

        assertEquals(UsageCrash("java.lang.IllegalStateException", "BoardScreen.kt:120 render"), crash)
    }
}
