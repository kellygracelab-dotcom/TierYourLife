package com.artiuillab.tieryourlife

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Two manifest attributes that a reasonable tidy-up would change, where the change compiles,
// passes every other test, and breaks the app silently. Asserted here so the manifest cannot
// drift without something failing.
@RunWith(AndroidJUnit4::class)
class ManifestContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // Dropping any one of the three brings back the activity restart on a language change,
    // and with it the blink. locale|layoutDirection alone — what most guides suggest — is not
    // enough: the layout-direction bits live inside screenLayout, so the system reports that
    // as changed too and restarts on the flag that was left out.
    @Test
    fun mainActivity_handlesEveryConfigurationChangeALanguageSwitchReports() {
        val required = ActivityInfo.CONFIG_LOCALE or
            ActivityInfo.CONFIG_LAYOUT_DIRECTION or
            ActivityInfo.CONFIG_SCREEN_LAYOUT

        val declared = context.packageManager
            .getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
            .configChanges

        assertEquals(
            "MainActivity must declare locale, layoutDirection and screenLayout together",
            required,
            declared and required,
        )
    }

    // Arabic is one of the eleven languages offered, so the layout has to mirror for it.
    // The icons mirror separately, per icon, in VectorIcon — see VectorIconMirroringTest.
    @Test
    fun theApp_supportsRightToLeft_becauseItShipsARightToLeftLanguage() {
        val flags = context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .flags

        assertEquals(
            ApplicationInfo.FLAG_SUPPORTS_RTL,
            flags and ApplicationInfo.FLAG_SUPPORTS_RTL,
        )
    }
}
