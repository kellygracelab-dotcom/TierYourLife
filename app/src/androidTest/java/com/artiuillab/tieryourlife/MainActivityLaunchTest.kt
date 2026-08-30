package com.artiuillab.tieryourlife

import android.content.pm.ActivityInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {

    // 411x891 is a phone upright, 891x411 the same phone on its side, and
    // 360x880 a folding phone closed. All three are asked to come back upright.
    @Test
    fun aCompactWindow_isAskedToStayUpright() {
        listOf(411 to 891, 891 to 411, 360 to 880).forEach { (width, height) ->
            assertEquals(
                "$width x $height should be portrait",
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                orientationFor(containerWidth = width, containerHeight = height, compactBreakpoint = 600),
            )
        }
    }

    // 1280x800 is a tablet on its side, 800x1280 the same tablet upright, and
    // 600x600 the smallest window that stops being compact.
    @Test
    fun aWindowWithRoomInBothDirections_turnsFreely() {
        listOf(1280 to 800, 800 to 1280, 600 to 600).forEach { (width, height) ->
            assertEquals(
                "$width x $height should be unspecified",
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                orientationFor(containerWidth = width, containerHeight = height, compactBreakpoint = 600),
            )
        }
    }

    @Test
    fun mainActivity_launchesBeforeActivityFieldInjectionWouldHaveRun() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                check(!activity.isFinishing)
            }
        }
    }
}
