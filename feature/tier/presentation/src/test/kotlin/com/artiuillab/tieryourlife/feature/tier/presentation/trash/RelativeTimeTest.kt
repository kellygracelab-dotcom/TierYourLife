package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_700_000_000_000L

    @Test
    fun underOneMinute_readsAsJustNow() {
        assertEquals(
            RelativeTime.JustNow,
            relativeTimeFrom(deletedAtMillis = now - 30_000, nowMillis = now),
        )
    }

    @Test
    fun underOneHour_readsInMinutes() {
        assertEquals(
            RelativeTime.Minutes(5),
            relativeTimeFrom(deletedAtMillis = now - 5 * 60_000, nowMillis = now),
        )
    }

    @Test
    fun underOneDay_readsInHours() {
        assertEquals(
            RelativeTime.Hours(2),
            relativeTimeFrom(deletedAtMillis = now - 2 * 3_600_000, nowMillis = now),
        )
    }

    @Test
    fun betweenOneAndTwoDays_readsAsYesterday() {
        assertEquals(
            RelativeTime.Yesterday,
            relativeTimeFrom(deletedAtMillis = now - 30 * 3_600_000, nowMillis = now),
        )
    }

    @Test
    fun overTwoDays_readsInDays() {
        assertEquals(
            RelativeTime.Days(4),
            relativeTimeFrom(deletedAtMillis = now - 4 * 86_400_000L, nowMillis = now),
        )
    }

    @Test
    fun deletedInTheFuture_readsAsJustNow() {
        assertEquals(
            RelativeTime.JustNow,
            relativeTimeFrom(deletedAtMillis = now + 86_400_000L, nowMillis = now),
        )
    }
}
