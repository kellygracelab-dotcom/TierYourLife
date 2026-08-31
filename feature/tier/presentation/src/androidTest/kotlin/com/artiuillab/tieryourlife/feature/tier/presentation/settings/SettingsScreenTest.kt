package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.LanguageOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
    fun languageSheet_listsEveryLanguageOption() {
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

    // Eleven of them, and on a tablet this is a dialog with a ceiling rather
    // than a sheet with the rest of the screen under it. The last language has
    // to be reachable, not merely drawn past the bottom edge.
    @Test
    fun languageSheet_reachesItsLastOption() {
        setScreen(languageTag = null)
        composeRule.onNodeWithTag(SettingsTestTags.LANGUAGE_ROW).performClick()

        composeRule.onNodeWithTag(SettingsTestTags.languageOption("ar"))
            .performScrollTo()
            .assertIsDisplayed()
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

    @Test
    fun themeControl_atDefaultFontScale_laysTheThreeChoicesOutSideBySide() {
        setScreen(languageTag = null)

        val light = composeRule.onNodeWithTag(SettingsTestTags.THEME_LIGHT).getUnclippedBoundsInRoot()
        val dark = composeRule.onNodeWithTag(SettingsTestTags.THEME_DARK).getUnclippedBoundsInRoot()

        assertEquals(light.top, dark.top)
        assertTrue("dark should sit to the right of light", dark.left > light.left)
    }

    // Stacking is the answer to labels that no longer fit across the width, so
    // the width has to be part of the question. A tablet at double scale still
    // has room for all three side by side, and is right to keep them there.
    @Test
    fun themeControl_atDoubleFontScale_stacksTheThreeChoices() {
        setScreen(languageTag = null, fontScale = 2f, width = PHONE_WIDTH)

        val light = composeRule.onNodeWithTag(SettingsTestTags.THEME_LIGHT).getUnclippedBoundsInRoot()
        val dark = composeRule.onNodeWithTag(SettingsTestTags.THEME_DARK).getUnclippedBoundsInRoot()
        val system = composeRule.onNodeWithTag(SettingsTestTags.THEME_SYSTEM).getUnclippedBoundsInRoot()

        assertEquals(light.left, dark.left)
        assertTrue("dark should sit below light", dark.top > light.top)
        assertTrue("system should sit below dark", system.top > dark.top)
        composeRule.onNodeWithText(string(R.string.theme_system)).performScrollTo().assertIsDisplayed()
    }

    private fun setScreen(
        languageTag: String?,
        onLanguageTagChange: (String?) -> Unit = {},
        fontScale: Float = 1f,
        width: Dp = Dp.Unspecified,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
            Box(if (width == Dp.Unspecified) Modifier else Modifier.width(width)) {
            TierYourLifeTheme {
                SettingsScreenContent(
                    account = Account.Guest,
                    credits = null,
                    onAccountClick = {},
                    versionName = "1.0",
                    themeChoice = ThemeChoice.SYSTEM,
                    onThemeChoiceChange = {},
                    languageTag = languageTag,
                    onLanguageTagChange = onLanguageTagChange,
                    trashCount = 0,
                    onBack = {},
                    onTrashClick = {},
            onHiddenClick = {},
            onModerationClick = {},
                    onExportClick = {},
                )
            }
            }
            }
        }
    }

    private companion object {
        val PHONE_WIDTH = 360.dp
    }

    private fun string(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
