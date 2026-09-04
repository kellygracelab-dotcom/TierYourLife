package com.artiuillab.tieryourlife.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.feature.account.presentation.navigation.accountScreen
import com.artiuillab.tieryourlife.feature.account.presentation.navigation.navigateToAccount
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.aiStudioScreen
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.navigateToAiStudio
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.ADDED_ITEMS_RESULT_KEY
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.authorScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.communityListScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.hiddenScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.moderationScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.myPublishedScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToAuthor
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToCommunityList
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToHidden
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToModeration
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToMyPublished
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToSettings
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToTierDetail
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToTrash
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.settingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.tierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.tierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.trashScreen

@Composable
fun TierYourLifeNavHost(
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Once there is room beside the content rather than under it, the rail is
    // the app's navigation and the tabs are gone. Outside the NavHost so it
    // stays put while destinations change under it -- a rail that redrew on
    // every navigation would be a rail that flickers.
    Row(Modifier.fillMaxSize()) {
        if (currentWindowShape.hasRail) {
            val entry by navController.currentBackStackEntryAsState()
            HomeRail(
                selected = railDestinationOf(entry),
                onSelect = { destination -> navController.goTo(destination) },
                // The rail's button is the phone's, moved. It asks by
                // navigating, and the request travels in the route, so the
                // lists screen this opens is the only one that hears it.
                onNewList = { navController.navigateToHome(community = false, makeBoard = true) },
            )
        }
        NavContent(
            navController = navController,
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
    }
}

/**
 * Which rail item is lit. Null on everything else, because a rail with nothing
 * selected is honest about being somewhere the rail did not take you.
 */
private fun railDestinationOf(entry: NavBackStackEntry?): RailDestination? {
    val route = entry?.destination?.route
    // Read only when the route is the one that carries it: toRoute on any
    // other destination throws, and a board is not a place to crash.
    val onCommunity = route?.contains("TierLists") == true &&
        runCatching { entry.toRoute<Route.TierLists>().community }.getOrDefault(false)
    return railDestinationFor(route, onCommunity)
}

/**
 * The rule on its own, with the back stack left out so it can be argued with
 * in a unit test. Type-safe routes serialise to their class name plus
 * arguments, which is why this matches on the name rather than the string.
 */
internal fun railDestinationFor(route: String?, onCommunity: Boolean): RailDestination? = when {
    route == null -> null
    route.contains("TierLists") -> if (onCommunity) RailDestination.Community else RailDestination.Lists
    route.contains("Settings") -> RailDestination.Settings
    else -> null
}

private fun NavHostController.goTo(destination: RailDestination) {
    when (destination) {
        RailDestination.Lists -> navigateToHome(community = false)
        RailDestination.Community -> navigateToHome(community = true)
        RailDestination.Settings -> navigateToSettings()
    }
}

/**
 * Top-level destinations replace each other rather than piling up: tapping
 * Community then Lists then Community should not leave three screens of back
 * stack behind.
 */
private fun NavHostController.navigateToHome(community: Boolean, makeBoard: Boolean = false) {
    navigate(Route.TierLists(community, makeBoard)) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun NavContent(
    navController: NavHostController,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
) {
    // Default cross-fades look like a flash because destinations share the same surface.
    NavHost(
        navController = navController,
        startDestination = Route.TierLists(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        tierListsScreen(
            onTierListClick = { id -> navController.navigateToTierDetail(id) },
            onCommunityListClick = { id -> navController.navigateToCommunityList(id) },
            onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
            onSettingsClick = { navController.navigateToSettings() },
            onSignInClick = { navController.navigateToAccount() },
            onNewListCreated = { id -> navController.navigateToTierDetail(id) },
        )
        tierDetailScreen(
            onBack = { navController.popBackStack() },
            // Replaces the board rather than stacking on it: the column beside
            // it is a way of switching boards, not of going deeper.
            onOpenList = { id ->
                navController.navigate(Route.TierDetail(id)) {
                    popUpTo<Route.TierDetail> { inclusive = true }
                }
            },
            onOpenAiStudio = { tierListId, listTitle -> navController.navigateToAiStudio(tierListId, listTitle) },
        )
        settingsScreen(
            onBack = { navController.popBackStack() },
            onTrashClick = { navController.navigateToTrash() },
            onHiddenClick = { navController.navigateToHidden() },
            onModerationClick = { navController.navigateToModeration() },
            onAccountClick = { navController.navigateToAccount() },
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
        trashScreen(onBack = { navController.popBackStack() })
        hiddenScreen(onBack = { navController.popBackStack() })
        moderationScreen(
            onBack = { navController.popBackStack() },
            // Only reachable on a window too narrow to stand the board beside
            // the queue. Where there is room, the pane shows it without going
            // anywhere.
            onOpenList = { id -> navController.navigateToCommunityList(id) },
        )
        accountScreen(
            onClose = { navController.popBackStack() },
            onOpenPublished = { navController.navigateToMyPublished() },
        )
        myPublishedScreen(
            onBack = { navController.popBackStack() },
            onOpen = { id -> navController.navigateToCommunityList(id) },
        )
        authorScreen(
            onBack = { navController.popBackStack() },
            onOpenList = { id -> navController.navigateToCommunityList(id) },
        )
        communityListScreen(
            onBack = { navController.popBackStack() },
            onSaved = { id ->
                navController.popBackStack()
                navController.navigateToTierDetail(id)
            },
            onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
        )
        aiStudioScreen(
            onBack = { ids ->
                if (ids.isNotEmpty()) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_ITEMS_RESULT_KEY, ArrayList(ids))
                }
                navController.popBackStack()
            },
        )
    }
}
