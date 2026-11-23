package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
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

    // Danylo, 23.11: there was no way to see how a topic is proposed.
    @Test
    fun proposeATopic_isALineInSight_andOpensTheNameField() {
        var started = 0
        composeRule.setContent {
            TierYourLifeTheme {
                TopicPickerScreenContent(
                    state = TopicPickerUiState(section = "film-tv", loaded = true),
                    onBack = {},
                    onDone = {},
                    proposing = ProposingCalls(onStart = { started++ }),
                )
            }
        }

        composeRule.onNodeWithTag(TopicPickerTestTags.PROPOSE).assertIsDisplayed().performClick()
        assertEquals(1, started)
    }

    @Test
    fun theNameField_sendsNothingUntilANameIsTyped() {
        composeRule.setContent {
            TierYourLifeTheme {
                TopicPickerScreenContent(state = TopicPickerUiState(section = "film-tv", loaded = true, proposal = ""), onBack = {}, onDone = {})
            }
        }
        composeRule.onNodeWithTag(TopicPickerTestTags.PROPOSAL).assertIsDisplayed()
        composeRule.onNodeWithTag(TopicPickerTestTags.PROPOSAL_SEND).assertIsNotEnabled()
    }

    @Test
    fun noSectionYet_theOnlyLineLeadsToTheSections() {
        var asked = 0
        composeRule.setContent {
            TierYourLifeTheme {
                TopicPickerScreenContent(state = TopicPickerUiState(loaded = true), onBack = {}, onDone = {}, onChooseSection = { asked++ })
            }
        }

        composeRule.onNodeWithTag(TopicPickerTestTags.QUERY).assertDoesNotExist()
        composeRule.onNodeWithTag(TopicPickerTestTags.PROPOSE).assertDoesNotExist()
        composeRule.onNodeWithTag(TopicPickerTestTags.CHOOSE_SECTION).assertIsDisplayed().performClick()
        assertEquals(1, asked)
    }
}
