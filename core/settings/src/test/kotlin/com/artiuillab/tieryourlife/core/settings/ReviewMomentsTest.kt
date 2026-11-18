package com.artiuillab.tieryourlife.core.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Play's review card: the first finished list from five cards, not in the first ten minutes, once per install. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReviewMomentsTest {

    private val installed = 1_800_000_000_000L
    private val later = installed + OnceReviewMoments.FIRST_MINUTES_MS

    private fun moments(preferences: AppPreferences = FakeAppPreferences(), now: Long = later, offered: Boolean = true) =
        OnceReviewMoments(preferences, installedAt = { installed }, now = { now }, offered = offered)

    private suspend fun OnceReviewMoments.asked(): Boolean = withTimeoutOrNull(200) { asks.first() } != null

    @Test
    fun theFirstFinishedList_asksOnce_andTheNextOnesStayQuiet() {
        runBlocking {
            val preferences = FakeAppPreferences()
            val moments = moments(preferences)

            moments.listFinished(5)
            moments.listFinished(25)

            assertEquals(Unit, withTimeoutOrNull(1_000) { moments.asks.first() })
            assertFalse(moments.asked())
            assertTrue(preferences.reviewAsked)
        }
    }

    @Test
    fun fewerThanFiveCards_isNotAList() {
        runBlocking {
            val preferences = FakeAppPreferences()
            val moments = moments(preferences)

            moments.listFinished(4)

            assertFalse(moments.asked())
            assertFalse(preferences.reviewAsked)
        }
    }

    @Test
    fun inTheFirstTenMinutes_nothingIsAsked_andTheChanceIsNotSpent() {
        runBlocking {
            val preferences = FakeAppPreferences()

            val soon = moments(preferences, now = later - 1)
            soon.listFinished(10)
            assertFalse(soon.asked())
            assertFalse(preferences.reviewAsked)

            val afterwards = moments(preferences)
            afterwards.listFinished(10)
            assertTrue(afterwards.asked())
        }
    }

    @Test
    fun onceAskedOnThisPhone_aLaterStartDoesNotAskAgain() {
        runBlocking {
            val moments = moments(FakeAppPreferences().apply { reviewAsked = true })

            moments.listFinished(10)

            assertFalse(moments.asked())
        }
    }

    @Test
    fun whileTheSwitchIsOff_nothingIsAsked_andNothingIsSpent() {
        runBlocking {
            val preferences = FakeAppPreferences()
            val moments = moments(preferences, offered = false)

            moments.listFinished(10)

            assertNull(withTimeoutOrNull(200) { moments.asks.first() })
            assertFalse(preferences.reviewAsked)
            assertFalse(Features.REVIEW_CARD_OFFERED)
        }
    }

    @Test
    fun theAskIsThePhones_soForgettingThePersonKeepsIt() {
        val preferences = SharedPreferencesAppPreferences(ApplicationProvider.getApplicationContext())
        assertFalse(preferences.reviewAsked())

        preferences.markReviewAsked()
        preferences.forgetThePerson()

        assertTrue(SharedPreferencesAppPreferences(ApplicationProvider.getApplicationContext()).reviewAsked())
    }
}
