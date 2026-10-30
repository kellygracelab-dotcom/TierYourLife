package com.artiuillab.tieryourlife.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.layout.FullScreenTasks
import com.artiuillab.tieryourlife.core.theme.layout.LocalFullScreenTasks
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.feature.account.presentation.navigation.accountScreen
import com.artiuillab.tieryourlife.feature.account.presentation.navigation.navigateToAccount
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.aiStudioScreen
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.navigateToAiStudio
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityHomeScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.CommunityRoute
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.allSectionsScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.authorScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.communityListScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.followingScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.moderationScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.myPublishedScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToAllSections
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToAuthor
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToCommunityList
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToFollowing
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToModeration
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToMyPublished
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToProposeSection
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToSection
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToTopic
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.proposeSectionScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.sectionScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.topicScreen
import com.artiuillab.tieryourlife.feature.play.presentation.PlayOverBoards
import com.artiuillab.tieryourlife.feature.settings.presentation.navigation.hiddenScreen
import com.artiuillab.tieryourlife.feature.settings.presentation.navigation.navigateToHidden
import com.artiuillab.tieryourlife.feature.settings.presentation.navigation.navigateToSettings
import com.artiuillab.tieryourlife.feature.settings.presentation.navigation.settingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.ADDED_ITEMS_RESULT_KEY
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.COVER_CHANGED_RESULT_KEY
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.TOPIC_CHANGED_RESULT_KEY
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.coverPickerScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToCoverPicker
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToTierDetail
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToTopicPicker
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.navigateToTrash
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.tierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.topicPickerScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.trashScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen

@Composable
fun TierYourLifeNavHost(
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    navController: NavHostController = rememberNavController(),
    handOff: HandOff? = null,
    onHandOffDone: () -> Unit = {},
    onBoardShown: (Long?) -> Unit = {},
) {
    // The rail replaces the tabs once there is room beside the content.
    // Outside the NavHost so it stays put while destinations change under it.
    // A full-screen task, the duels or the AI studio, has the window to itself.
    val tasks = remember { FullScreenTasks() }
    val entry by navController.currentBackStackEntryAsState()
    Row(Modifier.fillMaxSize()) {
        if (currentWindowShape.hasRail && !tasks.any && !railHiddenOn(entry?.destination?.route)) {
            HomeRail(
                selected = railDestinationOf(entry),
                onSelect = { destination -> navController.goTo(destination) },
                // The phone's corner button, moved. The request travels in the
                // route, so only the lists screen it opens hears it.
                onNewList = { navController.navigateToHome(community = false, makeBoard = true) },
            )
        }
        CompositionLocalProvider(LocalFullScreenTasks provides tasks) {
            NavContent(
                navController = navController,
                themeChoice = themeChoice,
                onThemeChoiceChange = onThemeChoiceChange,
                languageTag = languageTag,
                onLanguageTagChange = onLanguageTagChange,
            )
        }
    }
    HandOffDelivery(navController, handOff, onHandOffDone)
    BoardReporter(navController, onBoardShown)
}

/** What the cover handed over, opened once the phone is open: after the graph is set, so there is somewhere to go. */
@Composable
private fun HandOffDelivery(navController: NavHostController, handOff: HandOff?, onDone: () -> Unit) {
    LaunchedEffect(handOff) {
        val subject = handOff?.subject ?: return@LaunchedEffect
        when (val destination = HandOffRules.destinationOf(subject)) {
            is HandOffDestination.Board -> navController.navigateToTierDetail(destination.listId, destination.focusTierId)
            is HandOffDestination.CommunityList -> navController.navigateToCommunityList(
                CommunityRoute.CommunityList(publishedId = destination.publishedId, title = destination.title.ifEmpty { null }),
            )
            HandOffDestination.Home -> Unit
        }
        onDone()
    }
}

/** Tells the cover which board is open, so a phone shut on one opens the cover on that list. */
@Composable
private fun BoardReporter(navController: NavHostController, onBoardShown: (Long?) -> Unit) {
    val entry by navController.currentBackStackEntryAsState()
    LaunchedEffect(entry) {
        val board = entry?.takeIf { it.destination.hasRoute<Route.TierDetail>() }?.toRoute<Route.TierDetail>()
        onBoardShown(board?.tierListId)
    }
}

/** Null off the rail's own destinations: a rail with nothing lit is honest about being somewhere it did not take you. */
private fun railDestinationOf(entry: NavBackStackEntry?): RailDestination? {
    val route = entry?.destination?.route
    // Read only when the route is the one that carries it: toRoute on any
    // other destination throws, and a board is not a place to crash.
    val onCommunity = route?.contains("TierLists") == true &&
        runCatching { entry.toRoute<Route.TierLists>().community }.getOrDefault(false)
    return railDestinationFor(route, onCommunity)
}

/** The rule on its own, so it can be unit-tested. A type-safe route serialises to its class name plus arguments; only the last name counts, so a sibling route in the same file cannot light the wrong item. */
internal fun railDestinationFor(route: String?, onCommunity: Boolean): RailDestination? =
    when (route?.substringBefore('?')?.substringBefore('/')?.substringAfterLast('.')) {
        null -> null
        "TierLists" -> if (onCommunity) RailDestination.Community else RailDestination.Lists
        "Settings" -> RailDestination.Settings
        else -> null
    }

/** Destinations that are one task from end to end and want every pixel of a short window for it. */
internal fun railHiddenOn(route: String?): Boolean =
    route?.substringBefore('?')?.substringBefore('/')?.substringAfterLast('.') == "AiStudioRoute"

private fun NavHostController.goTo(destination: RailDestination) {
    when (destination) {
        RailDestination.Lists -> navigateToHome(community = false)
        RailDestination.Community -> navigateToHome(community = true)
        RailDestination.Settings -> navigateToSettings()
    }
}

/** The two halves of the home screen are joined here rather than in a feature: they are on their way into two, and only this module will see both. */
private fun NavGraphBuilder.homeScreen(navController: NavHostController) {
    composable<Route.TierLists> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.TierLists>()
        HomeScreen(
            startOnCommunity = route.community,
            mine = { tabs, arrival ->
                TierListsScreen(
                    makeBoard = route.makeBoard,
                    onTierListClick = { id -> navController.navigateToTierDetail(id) },
                    onSettingsClick = { navController.navigateToSettings() },
                    onSignInClick = { navController.navigateToAccount() },
                    onNewListCreated = { id -> navController.navigateToTierDetail(id) },
                    tabs = tabs,
                    arrival = arrival,
                )
            },
            community = { tabs ->
                CommunityHomeScreen(
                    onOpenList = { id -> navController.navigateToCommunityList(id) },
                    onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
                    onSettingsClick = { navController.navigateToSettings() },
                    onManageFollowing = { navController.navigateToFollowing() },
                    onOpenSection = { route -> navController.navigateToSection(route) },
                    tabs = tabs,
                    onOpenAllSections = { navController.navigateToAllSections() },
                )
            },
        )
    }
}

private const val PUSH_MILLIS = 260
private const val POP_MILLIS = 340

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTopLevels(): Boolean =
    isTopLevel(initialState) && isTopLevel(targetState)

private fun isTopLevel(entry: NavBackStackEntry): Boolean = railDestinationFor(entry.destination.route, false) != null

/** The next screen slides in whole over the one that stays put. Nothing fades: two translucent screens over a dark window made a frame darker than either, which read as a flash. */
private fun pushIn(): EnterTransition =
    slideInHorizontally(tween(PUSH_MILLIS, easing = FastOutSlowInEasing)) { width -> width }

/** Kept exactly as it is for the whole push, and only then let go. */
private fun holdBeneath(millis: Int): ExitTransition = fadeOut(tween(millis), targetAlpha = 1f)

/** Longer than the push and eased at both ends: a quick exit read as the screen being yanked away. */
private fun popOut(): ExitTransition =
    slideOutHorizontally(tween(POP_MILLIS, easing = EaseInOutCubic)) { width -> width }

/** Top-level destinations replace each other rather than piling up. */
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
    // One set of rules for every screen: the next one slides in a little and
    // fades up over the one that stays beneath; back is the same, shorter.
    // Siblings at the top level only fade: nothing is above or below them.
    NavHost(
        navController = navController,
        startDestination = Route.TierLists(),
        enterTransition = { if (betweenTopLevels()) fadeIn(tween(PUSH_MILLIS)) else pushIn() },
        exitTransition = { if (betweenTopLevels()) fadeOut(tween(PUSH_MILLIS)) else holdBeneath(PUSH_MILLIS) },
        // The screen underneath is already whole; it has nothing to animate.
        popEnterTransition = { if (betweenTopLevels()) fadeIn(tween(POP_MILLIS)) else EnterTransition.None },
        popExitTransition = { if (betweenTopLevels()) fadeOut(tween(POP_MILLIS)) else popOut() },
    ) {
        homeScreen(navController)
        tierDetailScreen(
            onBack = { navController.popBackStack() },
            // Replaces the board rather than stacking: the column beside it
            // switches boards, it does not go deeper.
            onOpenList = { id ->
                navController.navigate(Route.TierDetail(id)) {
                    popUpTo<Route.TierDetail> { inclusive = true }
                }
            },
            onOpenAiStudio = { tierListId, listTitle -> navController.navigateToAiStudio(tierListId, listTitle) },
            onChooseCover = { id -> navController.navigateToCoverPicker(id) },
            onChooseTopic = { id -> navController.navigateToTopicPicker(id) },
            onOpenPublishedList = { id -> navController.navigateToCommunityList(id) },
            play = PlayOverBoards,
            onProposeSection = { fileUnder -> navController.navigateToProposeSection(fileUnder) },
        )
        coverPickerScreen(
            onBack = { navController.popBackStack() },
            // The board behind it reads its own row again rather than being told what changed.
            onSaved = {
                navController.previousBackStackEntry?.savedStateHandle?.set(COVER_CHANGED_RESULT_KEY, true)
                navController.popBackStack()
            },
        )
        topicPickerScreen(
            onBack = { navController.popBackStack() },
            onSaved = {
                navController.previousBackStackEntry?.savedStateHandle?.set(TOPIC_CHANGED_RESULT_KEY, true)
                navController.popBackStack()
            },
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
            // Only reachable on a window too narrow for the pane beside the queue.
            onOpenList = { id -> navController.navigateToCommunityList(id) },
        )
        accountScreen(
            onClose = { navController.popBackStack() },
            onOpenPublished = { navController.navigateToMyPublished() },
        )
        myPublishedScreen(
            onBack = { navController.popBackStack() },
            onOpen = { route -> navController.navigateToCommunityList(route) },
        )
        authorScreen(
            onBack = { navController.popBackStack() },
            onOpenList = { route -> navController.navigateToCommunityList(route) },
        )
        sectionScreen(
            onBack = { navController.popBackStack() },
            onOpenList = { route -> navController.navigateToCommunityList(route) },
            onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
        )
        allSectionsScreen(
            onBack = { navController.popBackStack() },
            onOpenSection = { route -> navController.navigateToSection(route) },
            onProposeSection = { navController.navigateToProposeSection() },
        )
        proposeSectionScreen(
            onClose = { navController.popBackStack() },
            // The form goes; a guest comes back to it signed in by opening it again.
            onSignIn = {
                navController.popBackStack()
                navController.navigateToAccount()
            },
            onOpenSection = { route ->
                navController.popBackStack()
                navController.navigateToSection(route)
            },
        )
        followingScreen(
            onBack = { navController.popBackStack() },
            onOpenAuthor = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
            onOpenTopic = { route -> navController.navigateToTopic(route) },
            onOpenSection = { route -> navController.navigateToSection(route) },
        )
        topicScreen(
            onBack = { navController.popBackStack() },
            onOpenList = { route -> navController.navigateToCommunityList(route) },
            onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
        )
        communityListScreen(
            onBack = { navController.popBackStack() },
            onOpenList = { id -> navController.navigateToCommunityList(id) },
            // The copy takes the published list's place: one move, and back
            // leads to where the list was found.
            onOpenSaved = { id ->
                navController.navigate(Route.TierDetail(id)) {
                    popUpTo<CommunityRoute.CommunityList> { inclusive = true }
                }
            },
            onAuthorClick = { uid, name, photoUrl -> navController.navigateToAuthor(uid, name, photoUrl) },
            play = PlayOverBoards,
            onSignIn = { navController.navigateToAccount() },
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
