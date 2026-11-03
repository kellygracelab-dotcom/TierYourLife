package com.artiuillab.tieryourlife.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * The same journeys with and without the profile, so the gain is measured:
 *
 * ./gradlew :baselineprofile:connectedBenchmarkFastAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
 *
 * An emulator also needs ...androidx.benchmark.suppressErrors=EMULATOR, and its
 * numbers only compare with each other.
 */
@RunWith(Parameterized::class)
@LargeTest
class AppBenchmarks(private val compilation: CompilationMode) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStart() = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.COLD,
        iterations = STARTUP_ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        waitForLists()
    }

    @Test
    fun boardScroll() = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        iterations = FRAME_ITERATIONS,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            waitForLists()
            ensureBoard()
        },
    ) {
        openBoardAndScroll()
    }

    @Test
    fun communityBrowse() = rule.measureRepeated(
        packageName = targetAppId,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        iterations = FRAME_ITERATIONS,
        setupBlock = {
            killProcess()
            startActivityAndWait()
            waitForLists()
        },
    ) {
        browseCommunity()
        openCommunityList()
        openSearch()
    }

    companion object {
        private const val STARTUP_ITERATIONS = 10
        private const val FRAME_ITERATIONS = 5

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun modes(): List<CompilationMode> = listOf(
            CompilationMode.None(),
            CompilationMode.Partial(BaselineProfileMode.Require),
        )
    }
}
