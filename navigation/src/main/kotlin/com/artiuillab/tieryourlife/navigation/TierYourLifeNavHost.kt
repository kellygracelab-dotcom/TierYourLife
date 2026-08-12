package com.artiuillab.tieryourlife.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.aiStudioScreen
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.navigateToAiStudio
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.ADDED_ITEM_RESULT_KEY
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
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
            onSettingsClick = { navController.navigateToSettings() },
            onNewListCreated = { id -> navController.navigateToTierDetail(id, startInTitleEdit = true) },
        )
        tierDetailScreen(
            onBack = { navController.popBackStack() },
            onOpenAiStudio = { tierListId, listTitle -> navController.navigateToAiStudio(tierListId, listTitle) },
        )
        settingsScreen(
            onBack = { navController.popBackStack() },
            onTrashClick = { navController.navigateToTrash() },
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
        )
        trashScreen(onBack = { navController.popBackStack() })
        aiStudioScreen(
            onBack = { navController.popBackStack() },
            onCardAdded = { itemId ->
                navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_ITEM_RESULT_KEY, itemId)
                navController.popBackStack()
            },
        )
    }
}
