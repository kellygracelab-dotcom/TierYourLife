package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

fun NavGraphBuilder.tierListsScreen(
    onTierListClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
) {
    composable<Route.TierLists> {
        TierListsScreen(
            onTierListClick = onTierListClick,
            onSettingsClick = onSettingsClick,
            onNewListCreated = onNewListCreated,
        )
    }
}

const val ADDED_ITEMS_RESULT_KEY = "ai_added_item_ids"

fun NavGraphBuilder.tierDetailScreen(
    onBack: () -> Unit,
    onOpenAiStudio: (tierListId: Long, listTitle: String) -> Unit,
) {
    composable<Route.TierDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.TierDetail>()
        val addedItemIds by backStackEntry.savedStateHandle
            .getStateFlow<List<Long>>(ADDED_ITEMS_RESULT_KEY, emptyList())
            .collectAsStateWithLifecycle()
        TierDetailScreen(
            onBack = onBack,
            onOpenAiStudio = { listTitle -> onOpenAiStudio(route.tierListId, listTitle) },
            addedItemIds = addedItemIds,
            onAddedItemConsumed = { backStackEntry.savedStateHandle[ADDED_ITEMS_RESULT_KEY] = ArrayList<Long>() },
        )
    }
}

fun NavGraphBuilder.settingsScreen(
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onAccountClick: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
) {
    composable<Route.Settings> {
        SettingsScreen(
            onBack = onBack,
            onTrashClick = onTrashClick,
            onAccountClick = onAccountClick,
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
    }
}

fun NavGraphBuilder.trashScreen(onBack: () -> Unit) {
    composable<Route.Trash> {
        TrashScreen(onBack = onBack)
    }
}

fun NavController.navigateToTierDetail(id: Long) {
    navigate(Route.TierDetail(id))
}

fun NavController.navigateToSettings() {
    navigate(Route.Settings)
}

fun NavController.navigateToTrash() {
    navigate(Route.Trash)
}
