package com.artiuillab.tieryourlife.core.usage

import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageHeadersTest {

    @Test
    fun onByDefault_theVersionAndTheLocaleGoWithEveryCall() {
        val headers = UsageHeaders(FakeAppPreferences(), UsageInstall("597")) { "uk-UA" }

        assertEquals(mapOf("X-TYL-Client" to "android/597", "X-TYL-Locale" to "uk-UA"), headers.headers())
    }

    @Test
    fun statisticsOff_sendsNeither() {
        val preferences = FakeAppPreferences().apply { usageOn = false }

        assertEquals(emptyMap<String, String>(), UsageHeaders(preferences, UsageInstall("597")) { "uk-UA" }.headers())
    }
}
