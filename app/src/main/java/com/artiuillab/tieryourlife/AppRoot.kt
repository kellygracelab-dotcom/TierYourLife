package com.artiuillab.tieryourlife

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.core.theme.layout.WindowShape
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverScreen
import com.artiuillab.tieryourlife.navigation.TierYourLifeNavHost

@Composable
fun AppRoot(
    state: AppUiState,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    onLanguageTagChange: (String?) -> Unit,
) {
    val darkTheme = when (state.themeChoice) {
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    }

    TierYourLifeTheme(darkTheme = darkTheme) {
        // The app draws under the system bars and pads for them itself, screen
        // by screen -- but only ever at the top and the bottom, which is where
        // they are on an upright phone. Turn the window sideways, or open a
        // folding phone's cover screen, and the navigation bar moves to an edge
        // nothing was padding for and lands on top of the content.
        //
        // Horizontal only: the top and bottom are already somebody's job, and
        // doing them again here would pad everything twice. On an upright phone
        // this measures zero, which is why it has been invisible.
        BoxWithConstraints(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // Measured once, here, and read wherever it is needed. Measured
            // after the insets rather than before, because the width a layout
            // can actually use is what is left over -- and on a folded phone
            // in landscape that is a different answer.
            // A cover screen and half a phone in split view measure the same;
            // only the system knows which is which.
            val activity = LocalActivity.current
            val shape = WindowShape.of(
                width = maxWidth,
                height = maxHeight,
                shareable = activity?.isInMultiWindowMode == true,
            )
            CompositionLocalProvider(LocalWindowShape provides shape) {
                // A cover screen is not a very small phone; it is a surface
                // that can show a board and cannot be used to build one. It
                // gets its own screen rather than the app with most of its
                // controls refusing.
                if (shape.isGlanceable) {
                    CoverScreen()
                } else {
                    TierYourLifeNavHost(
                        themeChoice = state.themeChoice,
                        onThemeChoiceChange = onThemeChoiceChange,
                        languageTag = state.languageTag,
                        onLanguageTagChange = onLanguageTagChange,
                    )
                }
            }
        }
    }
}
