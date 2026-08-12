package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiStudioScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var stateHolder: MutableState<AiStudioUiState>
    private var sentCount = 0
    private var lastFieldTextAtSend = ""

    private fun setContent(initial: AiStudioUiState) {
        sentCount = 0
        lastFieldTextAtSend = ""
        stateHolder = mutableStateOf(initial)
        composeRule.setContent {
            val state by stateHolder
            var fieldText by remember { mutableStateOf("") }
            TierYourLifeTheme {
                AiStudioScreenContent(
                    state = state,
                    listTitle = "Sci-fi films",
                    fieldText = fieldText,
                    onFieldTextChange = { fieldText = it },
                    onBack = {},
                    onSend = {
                        sentCount++
                        lastFieldTextAtSend = fieldText
                    },
                    onHintClick = { hint -> fieldText = hint },
                    onRetry = {},
                    onRegenerate = {},
                    onSaveCard = { _, _ -> },
                )
            }
        }
    }

    @Test
    fun emptyState_showsThreeHints() {
        setContent(AiStudioUiState())

        composeRule.onNodeWithTag(AiStudioTestTags.hint(0)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiStudioTestTags.hint(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiStudioTestTags.hint(2)).assertIsDisplayed()
    }

    @Test
    fun tappingAHint_fillsTheField_butDoesNotSend() {
        setContent(AiStudioUiState())

        composeRule.onNodeWithTag(AiStudioTestTags.hint(0)).performClick()

        composeRule.onNodeWithTag(AiStudioTestTags.FIELD)
            .assertTextContains("A neon-lit Tokyo street in the rain")
        composeRule.runOnIdle { assertEquals(0, sentCount) }
    }

    @Test
    fun typingAndSending_invokesOnSend_andRenderingAnExchangeShowsItsBubbleAndCard() {
        setContent(AiStudioUiState())
        composeRule.onNodeWithTag(AiStudioTestTags.FIELD).performTextInput("A red desert")
        composeRule.onNodeWithTag(AiStudioTestTags.SEND).performClick()

        composeRule.runOnIdle {
            assertEquals(1, sentCount)
            assertEquals("A red desert", lastFieldTextAtSend)
        }

        val withExchange = AiStudioUiState(
            exchanges = listOf(AiExchange(id = 1, prompt = "A red desert", phase = AiExchangePhase.Generating)),
            generating = true,
        )
        composeRule.runOnIdle { stateHolder.value = withExchange }

        composeRule.onNodeWithTag(AiStudioTestTags.bubble(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(AiStudioTestTags.result(1)).assertIsDisplayed()
    }

    @Test
    fun resultCard_whenAlreadyAdded_showsAddedLabel_andHidesActionButtons() {
        setContent(
            AiStudioUiState(
                exchanges = listOf(
                    AiExchange(
                        id = 1,
                        prompt = "A red desert",
                        phase = AiExchangePhase.Result(GeneratedCardImage(prompt = "A red desert", imageUri = "")),
                        addedItemId = 42L,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Added").assertIsDisplayed()
        composeRule.onNodeWithText("Add to list").assertDoesNotExist()
        composeRule.onNodeWithText("Regenerate").assertDoesNotExist()
    }
}
