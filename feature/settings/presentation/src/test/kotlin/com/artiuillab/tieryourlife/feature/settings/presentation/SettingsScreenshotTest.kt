package com.artiuillab.tieryourlife.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings sideways: one slim bar on the reading column, no way back where the rail is the way. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w891dp-h411dp-land-xhdpi")
class SettingsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            val window = LocalWindowInfo.current.containerSize
            val (width, height) = with(LocalDensity.current) { window.width.toDp() to window.height.toDp() }
            TierYourLifeTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalWindowShape provides WindowShape.of(width, height),
                    LocalShortWindow provides WindowShape.isShort(height),
                ) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png", roborazziOptions = OPTIONS)
    }

    @Composable
    private fun Settings() = SettingsScreenContent(
        account = Account.SignedIn(email = "danylo@example.test", photoUrl = null, displayName = "danylo"),
        credits = 12,
        onAccountClick = {},
        versionName = "1.4.0",
        themeChoice = ThemeChoice.SYSTEM,
        onThemeChoiceChange = {},
        languageTag = null,
        onLanguageTagChange = {},
        trashCount = 4,
        onBack = {},
        onTrashClick = {},
        onHiddenClick = {},
        onModerationClick = {},
        onExportClick = {},
    )

    @Test
    fun settings_phoneLandscape() = capture("settings_landscape") { Settings() }

    @Test
    fun settings_phoneLandscape_dark() = capture("settings_landscape_dark", dark = true) { Settings() }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xhdpi")
    fun settings_phonePortrait() = capture("settings_phone_portrait") { Settings() }

    private companion object {
        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
