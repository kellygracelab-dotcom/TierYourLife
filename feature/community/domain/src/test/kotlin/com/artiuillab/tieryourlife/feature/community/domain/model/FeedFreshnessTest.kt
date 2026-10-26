package com.artiuillab.tieryourlife.feature.community.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The thresholds the screens open, refresh and apologise by. */
class FeedFreshnessTest {

    private val minute = 60_000L

    @Test
    fun aFeedIsStaleAfterFiveMinutes() {
        assertFalse(FeedFreshness.isStale(fetchedAtMillis = 0, nowMillis = 5 * minute))
        assertTrue(FeedFreshness.isStale(fetchedAtMillis = 0, nowMillis = 5 * minute + 1))
    }

    @Test
    fun theOfflineStripNeedsAFailureAndAnHourOldFeed() {
        assertFalse(FeedFreshness.showsOfflineStrip(lastAttemptFailed = false, fetchedAtMillis = 0, nowMillis = 120 * minute))
        assertFalse(FeedFreshness.showsOfflineStrip(lastAttemptFailed = true, fetchedAtMillis = 0, nowMillis = 60 * minute))
        assertTrue(FeedFreshness.showsOfflineStrip(lastAttemptFailed = true, fetchedAtMillis = 0, nowMillis = 60 * minute + 1))
    }

    @Test
    fun retriesComeTenSecondsApartAtMost() {
        assertTrue(FeedFreshness.mayRetry(lastRetryMillis = null, nowMillis = 0))
        assertFalse(FeedFreshness.mayRetry(lastRetryMillis = 0, nowMillis = 9_999))
        assertTrue(FeedFreshness.mayRetry(lastRetryMillis = 0, nowMillis = 10_000))
    }

    @Test
    fun aSectionAndItsTopicAreKeptApart() {
        assertEquals("section:games", FeedKeys.section("games"))
        assertEquals("section:games:topic:t1", FeedKeys.section("games", "t1"))
    }
}
