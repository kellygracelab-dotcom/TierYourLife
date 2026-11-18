package com.artiuillab.tieryourlife.feature.settings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.tips.InMemoryTipMemory
import com.artiuillab.tieryourlife.core.theme.tips.LocalTipMemory
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings → How it works (artboard 50): four lines, and the newcomer's tips back on a tap. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-port-xhdpi")
class HowItWorksTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun open(memory: InMemoryTipMemory, dark: Boolean = false) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTipMemory provides memory) {
                TierYourLifeTheme(darkTheme = dark) {
                    SettingsScreenContent(
                        account = Account.Guest,
                        credits = null,
                        onAccountClick = {},
                        versionName = "1.4.0",
                        themeChoice = ThemeChoice.SYSTEM,
                        onThemeChoiceChange = {},
                        languageTag = null,
                        onLanguageTagChange = {},
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
        composeRule.onNodeWithTag(SettingsTestTags.HOW_IT_WORKS_ROW).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun showTheTipsAgain_bringsThemBack_andSaysSo() {
        val memory = InMemoryTipMemory(off = true, seen = setOf(Tip.Place, Tip.Play))
        open(memory)

        composeRule.onNodeWithTag(SettingsTestTags.HOW_IT_WORKS_SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.TIPS_AGAIN).performClick()
        composeRule.waitForIdle()

        assertFalse(memory.off)
        assertFalse(memory.seen(Tip.Place))
        composeRule.onNodeWithTag(SettingsTestTags.HOW_IT_WORKS_SHEET).assertDoesNotExist()
        composeRule.onNodeWithText("The tips will show again.").assertIsDisplayed()
    }

    @Test
    fun close_leavesTheTipsAsTheyWere() {
        val memory = InMemoryTipMemory(off = true)
        open(memory)

        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        assertTrue(memory.off)
        composeRule.onNodeWithTag(SettingsTestTags.HOW_IT_WORKS_SHEET).assertDoesNotExist()
    }

    @Test
    fun sheet_light() = capture("how_it_works_light")

    @Test
    fun sheet_dark() = capture("how_it_works_dark", dark = true)

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-port-mdpi")
    fun sheet_onTheSmallestPhone() = capture("how_it_works_320x640")

    @OptIn(ExperimentalRoborazziApi::class)
    private fun capture(name: String, dark: Boolean = false) {
        open(InMemoryTipMemory(), dark)
        composeRule.onNodeWithTag(SettingsTestTags.HOW_IT_WORKS_SHEET).assertIsDisplayed()
        captureScreenRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
