package com.artiuillab.tieryourlife.feature.settings.presentation

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.ReadyColours
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.settings.presentation.components.colourNames
import com.artiuillab.tieryourlife.feature.settings.presentation.components.shownColour
import com.artiuillab.tieryourlife.feature.settings.presentation.components.startHueOf
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings → Appearance → Colour (artboards 58–59): a tap recolours at once; one's own colour waits for Done. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h891dp-port-xhdpi")
class ColourTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<Pair<String, Int?>>()

    /** The screen as the app has it: drawn in the colour it shows as chosen. */
    private fun open(start: ColourSetting? = null, dark: Boolean = false) {
        composeRule.setContent {
            var setting by remember { mutableStateOf(start) }
            TierYourLifeTheme(darkTheme = dark, colour = setting?.let { ReadyColours.colourOf(it.name, it.hue) } ?: com.artiuillab.tieryourlife.core.theme.color.ThemeColour.Lavender) {
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
                    colour = setting,
                    onColourChoose = { name, hue ->
                        chosen += name to hue
                        setting = ColourSetting(name, hue ?: setting?.hue ?: 265, chosen.size.toLong())
                    },
                )
            }
        }
        composeRule.onNodeWithTag(SettingsTestTags.COLOUR_ROW).performScrollTo()
        composeRule.waitForIdle()
    }

    @Test
    fun aReadyColour_isChosenAtOnce() {
        open()

        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.MATCHA)).performClick()

        assertEquals(listOf(ReadyColours.MATCHA to null), chosen)
        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.MATCHA)).assertIsSelected()
    }

    @Test
    fun oneOwnColour_waitsForDone() {
        open()

        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.CUSTOM)).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.HUE_SLIDER).performSemanticsAction(SemanticsActions.SetProgress) { it(300f) }
        assertTrue(chosen.isEmpty())
        composeRule.onNodeWithTag(SettingsTestTags.COLOUR_DONE).performClick()

        assertEquals(listOf(ReadyColours.CUSTOM to 300), chosen)
        composeRule.onNodeWithTag(SettingsTestTags.YOUR_COLOUR_SHEET).assertDoesNotExist()
    }

    @Test
    fun cancel_changesNothing() {
        open(ColourSetting(ReadyColours.SKY, 212, 1))

        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.CUSTOM)).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.COLOUR_CANCEL).performClick()

        assertTrue(chosen.isEmpty())
        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.SKY)).assertIsSelected()
    }

    @Test
    fun thePhonesColours_onlyFromAndroid12_andAnUnknownName_showsLavender() {
        assertFalse(ReadyColours.PHONE in colourNames(Build.VERSION_CODES.R))
        assertTrue(ReadyColours.PHONE in colourNames(Build.VERSION_CODES.S))
        assertEquals(ReadyColours.LAVENDER, shownColour(ColourSetting(ReadyColours.PHONE, 0, 1), Build.VERSION_CODES.R))
        assertEquals(ReadyColours.LAVENDER, shownColour(ColourSetting("rose", 0, 1)))
        assertEquals(ReadyColours.LAVENDER, shownColour(null))
    }

    // QA D3, 19.11: one's own 264, then Sky, then "Your own" opens on 264 again, not on Sky's.
    @Test
    fun yourColour_opensOnOnesOwnHue_andOnTheThemesOnlyWhenThereNeverWasOne() {
        assertEquals(264, startHueOf(ColourSetting(ReadyColours.SKY, 264, 1)))
        assertEquals(300, startHueOf(ColourSetting(ReadyColours.CUSTOM, 300, 1)))
        assertEquals(136, startHueOf(ColourSetting(ReadyColours.MATCHA, null, 1)))
        assertEquals(265, startHueOf(ColourSetting(ReadyColours.LAVENDER, null, 1)))
        assertEquals(265, startHueOf(null))
    }

    @Test
    fun settings_ownColour_dark() = capture("colour_settings_own_dark", ColourSetting(ReadyColours.CUSTOM, 330, 1), dark = true)

    @Test
    fun settings_matcha() = capture("colour_settings_matcha", ColourSetting(ReadyColours.MATCHA, 125, 1))

    @Test
    fun settings_peach_dark() = capture("colour_settings_peach_dark", ColourSetting(ReadyColours.PEACH, 42, 1), dark = true)

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-port-mdpi")
    fun settings_sky_onTheSmallestPhone() = capture("colour_settings_sky_320x640", ColourSetting(ReadyColours.SKY, 212, 1))

    @Test
    fun yourColour_sheet() = captureSheet("colour_your_colour", dark = false)

    @Test
    fun yourColour_sheet_dark() = captureSheet("colour_your_colour_dark", dark = true)

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-port-mdpi")
    fun yourColour_sheet_onTheSmallestPhone() = captureSheet("colour_your_colour_320x640", dark = false)

    private fun capture(name: String, setting: ColourSetting, dark: Boolean = false) {
        open(setting, dark)
        composeRule.onNodeWithTag(SettingsTestTags.COLOUR_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.COLOUR_ROW).captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun captureSheet(name: String, dark: Boolean) {
        open(ColourSetting(ReadyColours.CUSTOM, 330, 1), dark)
        composeRule.onNodeWithTag(SettingsTestTags.colour(ReadyColours.CUSTOM)).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.YOUR_COLOUR_SHEET).assertIsDisplayed()
        captureScreenRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
