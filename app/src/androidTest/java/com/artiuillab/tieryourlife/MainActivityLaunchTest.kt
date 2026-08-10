package com.artiuillab.tieryourlife

import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {

    @Test
    fun mainActivity_launchesBeforeActivityFieldInjectionWouldHaveRun() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                check(!activity.isFinishing)
            }
        }
    }

    @Test
    fun compactWindows_areLockedToPortrait() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            orientationFor(containerWidth = 411, containerHeight = 891, compactBreakpoint = 600),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            orientationFor(containerWidth = 891, containerHeight = 411, compactBreakpoint = 600),
        )
    }

    @Test
    fun expandedWindows_followTheSystemOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            orientationFor(containerWidth = 1280, containerHeight = 800, compactBreakpoint = 600),
        )
    }
}
