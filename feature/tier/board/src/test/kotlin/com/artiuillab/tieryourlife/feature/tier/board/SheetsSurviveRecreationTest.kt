package com.artiuillab.tieryourlife.feature.tier.board

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.common.Hsl
import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierColorSelection
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.TierColorSelectionSaver
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Turning the phone recreates the activity; a sheet left open stays open, with what was typed in it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class SheetsSurviveRecreationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showBoard() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                TierYourLifeTheme { TierDetailScreenContent(state = TierDetailUiState.Success(previewTierList)) }
            }
        }
        composeRule.waitForIdle()
    }

    private fun turnThePhone() {
        composeRule.activityRule.scenario.recreate()
        showBoard()
    }

    @Test
    fun theTierEditor_keepsItsCaptionAndColour() {
        showBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tierBand(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextInput("Great")
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(5)).performClick()

        turnThePhone()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_SHEET).assertExists()
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).assertTextContains("Great")
        composeRule.onNodeWithTag(TierDetailTestTags.tierEditorPresetSwatch(5)).assertIsSelected()
    }

    @Test
    fun theMoveSheet_staysOpen() {
        showBoard()
        composeRule.onNodeWithTag(TierDetailTestTags.tile(1)).performTouchInput { doubleClick() }
        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET).assertExists()

        turnThePhone()

        composeRule.onNodeWithTag(TierDetailTestTags.MOVE_SHEET).assertExists()
    }

    @Test
    fun listSettings_staysOpen() {
        showBoard()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_SCREEN).assertExists()

        turnThePhone()

        composeRule.onNodeWithTag(TierDetailTestTags.LIST_SETTINGS_SCREEN).assertExists()
    }

    @Test
    fun aCustomColour_comesBackExactly() {
        val custom = TierColorSelection.Custom(light = Hsl(281.3f, 0.42f, 0.33f), dark = Hsl(287.9f, 0.55f, 0.77f))
        val saved = with(TierColorSelectionSaver) { SaverScope { true }.save(custom) }

        assertEquals(custom, TierColorSelectionSaver.restore(saved!!))
    }
}
