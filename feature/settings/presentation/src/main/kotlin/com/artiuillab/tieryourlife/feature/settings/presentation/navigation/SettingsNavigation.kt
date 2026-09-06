package com.artiuillab.tieryourlife.feature.settings.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.feature.settings.presentation.HiddenScreen
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsScreen

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onModerationClick: () -> Unit,
    onAccountClick: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
) {
    composable<SettingsRoute.Settings> {
        SettingsScreen(
            onBack = onBack,
            onTrashClick = onTrashClick,
            onHiddenClick = onHiddenClick,
            onModerationClick = onModerationClick,
            onAccountClick = onAccountClick,
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
    }
}

fun NavController.navigateToSettings() {
    navigate(SettingsRoute.Settings)
}

fun NavGraphBuilder.hiddenScreen(onBack: () -> Unit) {
    composable<SettingsRoute.Hidden> {
        HiddenScreen(onBack = onBack)
    }
}

fun NavController.navigateToHidden() {
    navigate(SettingsRoute.Hidden)
}
