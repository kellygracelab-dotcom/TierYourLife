package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.Composable
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverScreen

/** A cover screen gets its own screen rather than the app with most of its controls refusing. */
@Composable
fun TierYourLifeContent(
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
) {
    if (LocalWindowShape.current.isGlanceable) {
        CoverScreen()
    } else {
        TierYourLifeNavHost(
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
    }
}
