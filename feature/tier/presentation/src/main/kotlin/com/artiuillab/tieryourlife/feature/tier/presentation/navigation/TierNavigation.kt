package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.community.AuthorScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.community.CommunityListScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

fun NavGraphBuilder.communityListScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    composable<Route.CommunityList> {
        CommunityListScreen(onBack = onBack, onSaved = onSaved)
    }
}

fun NavController.navigateToCommunityList(publishedId: String) {
    navigate(Route.CommunityList(publishedId))
}

fun NavGraphBuilder.authorScreen(
    onBack: () -> Unit,
    onOpenList: (String) -> Unit,
) {
    composable<Route.Author> {
        AuthorScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToAuthor(uid: String, name: String, photoUrl: String?) {
    navigate(Route.Author(uid, name, photoUrl))
}

fun NavGraphBuilder.tierListsScreen(
    onTierListClick: (Long) -> Unit,
    onCommunityListClick: (String) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
    onSettingsClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
) {
    composable<Route.TierLists> {
        TierListsScreen(
            onTierListClick = onTierListClick,
            onCommunityListClick = onCommunityListClick,
            onAuthorClick = onAuthorClick,
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
