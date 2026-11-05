package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A tier's name stops at 80 characters and its caption at 60, as on the site, with a count shown once either is near. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TierEditorLimitsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun show() {
        composeRule.setContent {
            TierYourLifeTheme { TierEditorSheetContent(onDismiss = {}, onSave = { _, _, _, _ -> }) }
        }
    }

    @Test
    fun aShortName_showsNoCount() {
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextReplacement("S")

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_COUNTER, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aNameNearTheLimit_isCounted() {
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextReplacement("x".repeat(70))

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_COUNTER, useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("70/80")
    }

    @Test
    fun aLongerName_isCutAtTheLimit() {
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_FIELD).performTextReplacement("x".repeat(95))

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_LABEL_COUNTER, useUnmergedTree = true).assertTextEquals("80/80")
    }

    @Test
    fun aLongerCaption_isCutAtItsLimit() {
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextReplacement("y".repeat(49))
        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_COUNTER, useUnmergedTree = true).assertDoesNotExist()

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_FIELD).performTextReplacement("y".repeat(75))

        composeRule.onNodeWithTag(TierDetailTestTags.TIER_EDITOR_CAPTION_COUNTER, useUnmergedTree = true).assertTextEquals("60/60")
    }
}
