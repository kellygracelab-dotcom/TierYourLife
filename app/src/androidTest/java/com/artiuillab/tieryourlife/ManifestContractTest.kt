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

@RunWith(AndroidJUnit4::class)
class ManifestContractTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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

    // Upright is right for a phone and wrong for a tablet, so the activity
    // asks per window size, which the manifest must leave room for.
    @Test
    fun mainActivity_declaresNoOrientation_soTheWindowCanDecide() {
        val declared = context.packageManager
            .getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
            .screenOrientation

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, declared)
    }

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
