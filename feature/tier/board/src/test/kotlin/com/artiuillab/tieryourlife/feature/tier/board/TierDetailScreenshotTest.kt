package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The board as it looks, compared with the pictures beside the tests. A change
 * that moves a pixel on purpose re-records them (`recordRoborazziDebug`); one
 * that moves them by accident fails `verifyRoborazziDebug` in CI.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TierDetailScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean, state: TierDetailUiState) {
        composeRule.setContent {
            TierYourLifeTheme(darkTheme = dark) {
                TierDetailScreenContent(state = state)
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun aBoard_light() = capture("board_light", dark = false, TierDetailUiState.Success(previewTierList))

    @Test
    fun aBoard_dark() = capture("board_dark", dark = true, TierDetailUiState.Success(previewTierList))

    @Test
    fun anEmptyBoard_light() = capture(
        "board_empty_light",
        dark = false,
        TierDetailUiState.Success(previewTierList.copy(tiers = previewTierList.tiers.map { it.copy(items = emptyList()) })),
    )

    @Test
    fun aBoardThatFailedToLoad_light() = capture("board_error_light", dark = false, TierDetailUiState.Error)

    private companion object {
        // Text antialiasing differs a little between the Windows that records
        // and the Linux that verifies; a real change moves far more than this.
        val OPTIONS = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F),
        )
    }
}
