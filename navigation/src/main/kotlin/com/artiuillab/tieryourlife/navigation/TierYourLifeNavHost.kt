package com.artiuillab.tieryourlife.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
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
    // Default cross-fades look like a flash because destinations share the same surface.
    NavHost(
        navController = navController,
        startDestination = Route.TierLists,
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
        moderationScreen(onBack = { navController.popBackStack() })
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
