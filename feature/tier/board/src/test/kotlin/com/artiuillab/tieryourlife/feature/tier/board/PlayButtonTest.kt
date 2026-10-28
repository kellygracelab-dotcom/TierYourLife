package com.artiuillab.tieryourlife.feature.tier.board

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class PlayButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun play_standsFirstInTheBar_andCarriesADotWhileAGameIsSaved() {
        var asked = 0
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(previewTierList),
                    actions = TierDetailActions(onPlay = { asked++ }, playSaved = true),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Play").performClick()

        assertEquals(1, asked)
        composeRule.onRoot().captureRoboImage(
            "src/test/screenshots/board_play_saved.png",
            roborazziOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F)),
        )
    }

    @Test
    fun belowThreeCards_playIsOff_andSaysWhy() {
        val two = previewTierList.copy(
            tiers = previewTierList.tiers.mapIndexed { at, tier -> if (at == 0) tier else tier.copy(items = emptyList()) },
        )
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(state = TierDetailUiState.Success(two), actions = TierDetailActions(onPlay = {}))
            }
        }

        composeRule.onNodeWithContentDescription("Play needs at least 3 cards").assertIsNotEnabled()
    }

    @Test
    fun aScreenWithoutPlay_hasNoButton() {
        composeRule.setContent {
            TierYourLifeTheme { TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList)) }
        }

        assertEquals(0, composeRule.onAllNodes(hasTestTag(TierDetailTestTags.PLAY)).fetchSemanticsNodes().size)
    }

    @Test
    fun onSomeoneElsesList_playStandsBeforeTheOverflow() {
        composeRule.setContent {
            TierYourLifeTheme {
                TierDetailScreenContent(
                    state = TierDetailUiState.Success(previewTierList),
                    actions = TierDetailActions(onPlay = {}),
                    readOnly = true,
                    onReaderMoreClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(TierDetailTestTags.PLAY).assertIsEnabled()
    }
}
