package com.artiuillab.tieryourlife.feature.tier.board

import android.content.res.Configuration
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.publishErrorText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PublishLimitTextTest {
    private val until = Instant.parse("2026-11-15T09:05:00Z").toEpochMilli()

    @Test fun theWeeklyMessageUsesTheInterfaceLocaleAndDeviceTimezoneWithoutSuggestingDeletion() {
        val before = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"))
            val english = text(PublishError.WeeklyLimit(until), "en-US")
            val russian = text(PublishError.WeeklyLimit(until), "ru")
            assertTrue(english.contains("11:05"))
            assertTrue(english.contains("Nov 15"))
            assertTrue(russian.contains("11:05"))
            assertTrue(russian.contains("нояб"))
            assertFalse(english.contains(text(PublishError.TooManyLists, "en-US")))
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertTrue(text(PublishError.WeeklyLimit(until), "en-US").contains("9:05"))
        } finally {
            TimeZone.setDefault(before)
        }
    }

    @Test fun theOldLimitWithoutADateKeepsItsPreviousCopy() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals(resources.getString(R.string.list_settings_public_too_many), publishErrorText(PublishError.TooManyLists, resources, Locale.getDefault()))
    }

    private fun text(error: PublishError, tag: String): String {
        val app = RuntimeEnvironment.getApplication()
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(app.resources.configuration).apply { setLocale(locale) }
        return publishErrorText(error, app.createConfigurationContext(configuration).resources, locale)
    }
}
