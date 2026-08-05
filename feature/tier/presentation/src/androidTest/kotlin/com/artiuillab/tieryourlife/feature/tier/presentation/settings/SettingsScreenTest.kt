package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Covers the Language row and its bottom sheet added in docs/design-spec-home.md,
// section 7, subsection "2 - Language". The row sits between Theme and Trash — that
// ordering is exercised indirectly by every other Settings test still passing, since
// SettingsScreenContent lays the column out in one fixed sequence.
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun languageRow_showsCurrentLanguageNativeName_asItsSubtitle() {
        setScreen(languageTag = "uk")

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_language)).assertIsDisplayed()
        composeRule.onNodeWithText("Українська").assertIsDisplayed()
    }

    @Test
    fun languageRow_defaultTag_showsEnglishAsSubtitle() {
        // No stored tag and no androidTest-configured device locale override: the
        // instrumentation target runs in English, so "follow system" resolves to the
        // English/"Default" option (docs/design-spec-home.md, section 7).
        setScreen(languageTag = null)

        composeRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun languageRow_click_opensBottomSheet() {
        setScreen(languageTag = null)

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_SHEET).assertDoesNotExist()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_SHEET).assertIsDisplayed()
    }

    @Test
    fun languageSheet_listsAllElevenLanguageOptions() {
        setScreen(languageTag = null)
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()

        assertEquals(11, LanguageOptions.size)
        LanguageOptions.forEach { option ->
            composeRule.onNodeWithTag(SettingsTestTags.languageOption(option.persistTag)).assertIsDisplayed()
        }
    }

    @Test
    fun languageSheet_marksTheStoredTagsRowSelected_andNoOtherRow() {
        setScreen(languageTag = "ja")
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()

        composeRule.onNodeWithTag(SettingsTestTags.languageOption("ja")).assertIsSelected()
        composeRule.onNodeWithTag(SettingsTestTags.languageOption(null)).assertIsNotSelected()
        composeRule.onNodeWithTag(SettingsTestTags.languageOption("ru")).assertIsNotSelected()
    }

    @Test
    fun languageSheet_selectingALanguage_reportsItsTag_andClosesTheSheet() {
        var reportedTag: String? = "unset"
        setScreen(languageTag = null, onLanguageTagChange = { reportedTag = it })

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.languageOption("ru")).performClick()

        composeRule.runOnIdle { assertEquals("ru", reportedTag) }
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_SHEET).assertDoesNotExist()
    }

    @Test
    fun languageSheet_selectingTheDefaultRow_reportsNullTag() {
        var reportedTag: String? = "unset"
        setScreen(languageTag = "de", onLanguageTagChange = { reportedTag = it })

        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.languageOption(null)).performClick()

        composeRule.runOnIdle { assertEquals(null, reportedTag) }
    }

    private fun setScreen(
        languageTag: String?,
        onLanguageTagChange: (String?) -> Unit = {},
    ) {
        composeRule.setContent {
            TierYourLifeTheme {
                SettingsScreenContent(
                    themeChoice = ThemeChoice.SYSTEM,
                    onThemeChoiceChange = {},
                    languageTag = languageTag,
                    onLanguageTagChange = onLanguageTagChange,
                    trashCount = 0,
                    onBack = {},
                    onTrashClick = {},
                    onExportClick = {},
                )
            }
        }
    }

    private fun string(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
