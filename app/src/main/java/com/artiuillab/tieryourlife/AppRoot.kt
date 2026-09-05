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
        // Screens pad for the system bars at the top and bottom only; sideways,
        // or on a cover screen, the navigation bar moves to an edge nothing
        // padded for. Horizontal only, so nothing is padded twice; on an
        // upright phone this measures zero.
        BoxWithConstraints(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
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
            CompositionLocalProvider(LocalWindowShape provides shape) {
                // A cover screen gets its own screen rather than the app with
                // most of its controls refusing.
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
