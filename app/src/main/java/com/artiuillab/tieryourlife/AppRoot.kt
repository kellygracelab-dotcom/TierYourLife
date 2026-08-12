package com.artiuillab.tieryourlife

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
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
        TierYourLifeNavHost(
            themeChoice = state.themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = state.languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
    }
}
