package com.artiuillab.tieryourlife.feature.settings.presentation.components

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.artiuillab.tieryourlife.core.settings.AppLinks
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Rate, share and write to us: each row does its own one thing, and the letter says no more than the versions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp")
class AboutRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun eachRowCallsItsOwnAction() {
        val taps = mutableListOf<String>()
        composeRule.setContent {
            TierYourLifeTheme {
                Column { AboutRows(onRate = { taps += "rate" }, onShare = { taps += "share" }, onFeedback = { taps += "feedback" }) }
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.RATE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.SHARE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.FEEDBACK_ROW).performClick()

        assertEquals(listOf("rate", "share", "feedback"), taps)
    }

    @Test
    fun rating_opensTheAppsPageInPlay() {
        rateTheApp(app)

        val opened = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertEquals(AppLinks.PLAY_APP, opened.dataString)
        assertEquals("com.android.vending", opened.`package`)
    }

    @Test
    fun sharing_sendsTheSite_andNotThePlayPageBeforeProduction() {
        shareTheApp(app)

        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        val text = send.getStringExtra(Intent.EXTRA_TEXT)!!
        assertTrue(text, AppLinks.SITE in text && "play.google.com" !in text)
    }

    @Test
    fun theLetter_goesToTheDeveloper_withTheVersionsAndNothingElse() {
        assertTrue(writeToUs(app, "1.0 (575)"))

        val mail = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_SENDTO, mail.action)
        assertEquals("mailto:", mail.dataString)
        assertArrayEquals(arrayOf(AppLinks.FEEDBACK_EMAIL), mail.getStringArrayExtra(Intent.EXTRA_EMAIL))
        val subject = mail.getStringExtra(Intent.EXTRA_SUBJECT)!!
        assertTrue(subject, "1.0 (575)" in subject && androidVersion() in subject)
        assertEquals(setOf(Intent.EXTRA_EMAIL, Intent.EXTRA_SUBJECT), mail.extras!!.keySet())
    }

    @Test
    fun androidVersion_isTheReleaseAndTheApiLevel() {
        assertEquals("14 (API 34)", androidVersion("14", 34))
    }
}
