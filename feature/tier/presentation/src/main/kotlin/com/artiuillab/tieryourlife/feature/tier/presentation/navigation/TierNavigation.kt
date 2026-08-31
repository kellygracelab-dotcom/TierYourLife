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
import com.artiuillab.tieryourlife.feature.tier.presentation.community.MyPublishedScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.HiddenScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.ModerationScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

fun NavGraphBuilder.communityListScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onAuthorClick: (uid: String, name: String, photoUrl: String?) -> Unit,
) {
    composable<Route.CommunityList> {
        CommunityListScreen(onBack = onBack, onSaved = onSaved, onAuthorClick = onAuthorClick)
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
    onSignInClick: () -> Unit,
    onNewListCreated: (Long) -> Unit,
) {
    composable<Route.TierLists> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.TierLists>()
        TierListsScreen(
            startOnCommunity = route.community,
            makeBoard = route.makeBoard,
            onTierListClick = onTierListClick,
            onCommunityListClick = onCommunityListClick,
            onAuthorClick = onAuthorClick,
            onSettingsClick = onSettingsClick,
            onSignInClick = onSignInClick,
            onNewListCreated = onNewListCreated,
        )
    }
}

const val ADDED_ITEMS_RESULT_KEY = "ai_added_item_ids"

fun NavGraphBuilder.tierDetailScreen(
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit = {},
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
            onOpenList = onOpenList,
            addedItemIds = addedItemIds,
            onAddedItemConsumed = { backStackEntry.savedStateHandle[ADDED_ITEMS_RESULT_KEY] = ArrayList<Long>() },
        )
    }
}

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
    composable<Route.Settings> {
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

fun NavGraphBuilder.myPublishedScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    composable<Route.MyPublished> {
        MyPublishedScreen(onBack = onBack, onOpen = onOpen)
    }
}

fun NavController.navigateToMyPublished() {
    navigate(Route.MyPublished)
}

fun NavGraphBuilder.moderationScreen(onBack: () -> Unit, onOpenList: (String) -> Unit) {
    composable<Route.Moderation> {
        ModerationScreen(onBack = onBack, onOpenList = onOpenList)
    }
}

fun NavController.navigateToModeration() {
    navigate(Route.Moderation)
}

fun NavGraphBuilder.hiddenScreen(onBack: () -> Unit) {
    composable<Route.Hidden> {
        HiddenScreen(onBack = onBack)
    }
}

fun NavController.navigateToHidden() {
    navigate(Route.Hidden)
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
