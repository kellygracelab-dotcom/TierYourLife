package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NamingDialogContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(prompt: String) {
        composeRule.setContent {
            TierYourLifeTheme {
                NamingDialogContent(
                    imageUri = "",
                    prompt = prompt,
                    onDismiss = {},
                    onSave = {},
                )
            }
        }
    }

    @Test
    fun namingDialog_prefillsTheNameField_withATitleSuggestedFromThePrompt_andEnablesAdd() {
        setContent("A neon-lit Tokyo street in the rain")

        composeRule.onNodeWithTag(AiStudioTestTags.NAME_FIELD)
            .assertTextContains("Neon-lit Tokyo street in the rain")
        composeRule.onNodeWithTag(AiStudioTestTags.ADD).assertIsEnabled()
    }

    @Test
    fun tappingClear_emptiesTheNameField_andDisablesAdd() {
        setContent("A neon-lit Tokyo street in the rain")

        composeRule.onNodeWithTag(AiStudioTestTags.CLEAR_NAME).performClick()

        composeRule.onNodeWithTag(AiStudioTestTags.NAME_FIELD)
            .assert(!hasText("Neon-lit Tokyo street in the rain", substring = true))
        composeRule.onNodeWithTag(AiStudioTestTags.ADD).assertIsNotEnabled()
    }

    @Test
    fun clearButton_isHidden_whenThePromptYieldsNoSuggestedTitle() {
        setContent("   ")

        composeRule.onNodeWithTag(AiStudioTestTags.CLEAR_NAME).assertDoesNotExist()
        composeRule.onNodeWithTag(AiStudioTestTags.ADD).assertIsNotEnabled()
    }
}
