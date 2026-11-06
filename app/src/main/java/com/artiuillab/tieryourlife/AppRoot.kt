package com.artiuillab.tieryourlife

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.BoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalShortWindow
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.navigation.TierYourLifeContent

@Composable
fun AppRoot(
    state: AppUiState,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    onLanguageTagChange: (String?) -> Unit,
    boardLayoutMemory: BoardLayoutMemory = remember { InMemoryBoardLayoutMemory() },
) {
    val darkTheme = when (state.themeChoice) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    }

    SystemBarIconsFor(darkTheme)

    TierYourLifeTheme(darkTheme = darkTheme) {
        // Screens pad for the system bars at the top and bottom only; sideways,
        // or on a cover screen, the navigation bar moves to an edge nothing
        // padded for. Horizontal only, so nothing is padded twice; on an
        // upright phone this measures zero.
        // Test tags double as resource ids so UiAutomator, which drives the
        // baseline profile journeys, finds what the Compose tests find.
        BoxWithConstraints(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // Measured once, after the insets: the width a layout can use is
            // what is left over. A cover screen and half a phone in split view
            // measure the same; only the system knows which is which.
            val activity = LocalActivity.current
            val shape = WindowShape.of(
                width = maxWidth,
                height = maxHeight,
                shareable = activity?.isInMultiWindowMode == true,
            )
            CompositionLocalProvider(
                LocalWindowShape provides shape,
                LocalShortWindow provides WindowShape.isShort(maxHeight),
                LocalBoardLayoutMemory provides boardLayoutMemory,
            ) {
                TierYourLifeContent(
                    themeChoice = state.themeChoice,
                    onThemeChoiceChange = onThemeChoiceChange,
                    languageTag = state.languageTag,
                    onLanguageTagChange = onLanguageTagChange,
                )
            }
        }
    }
}

/**
 * The bars' icons follow the app's theme, not the phone's: Dark chosen here on
 * a light phone left dark clock and battery icons on a dark screen.
 */
@Composable
private fun SystemBarIconsFor(darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
        )
        onDispose {}
    }
}

// The scrims enableEdgeToEdge() draws under three-button navigation by default.
private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
