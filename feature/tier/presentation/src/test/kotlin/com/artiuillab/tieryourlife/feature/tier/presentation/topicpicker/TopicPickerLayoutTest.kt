package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The snackbar host fills the screen; it must not push the bar, the field and the list off it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class TopicPickerLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theBarTheFieldAndTheChoices_areOnScreen() {
        composeRule.setContent {
            TierYourLifeTheme { TopicPickerScreenContent(state = TopicPickerUiState(), onBack = {}, onDone = {}) }
        }

        composeRule.onNodeWithTag(TopicPickerTestTags.BACK).assertIsDisplayed()
        composeRule.onNodeWithTag(TopicPickerTestTags.DONE).assertIsDisplayed()
        composeRule.onNodeWithTag(TopicPickerTestTags.QUERY).assertIsDisplayed()
        composeRule.onNodeWithTag(TopicPickerTestTags.NONE).assertIsDisplayed()
    }
}
