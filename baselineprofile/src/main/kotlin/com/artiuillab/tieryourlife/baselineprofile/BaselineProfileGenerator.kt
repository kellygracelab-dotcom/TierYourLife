package com.artiuillab.tieryourlife.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes app/src/main/generated/baselineProfiles. Run on an emulator or phone
 * with API 33+ (or a rooted one), then commit what it writes:
 *
 * ./gradlew :app:generateBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = targetAppId, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForLists()
    }

    @Test
    fun journeys() = rule.collect(packageName = targetAppId) {
        pressHome()
        startActivityAndWait()
        waitForLists()
        ensureBoard()
        openBoardAndScroll()
        browseCommunity()
        openCommunityList()
        openSearch()
        openCatalogueSearch()
    }
}
