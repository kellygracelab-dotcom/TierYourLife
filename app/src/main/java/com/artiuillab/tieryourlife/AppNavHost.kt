package com.artiuillab.tieryourlife

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

@Composable
fun AppNavHost(
    state: AppUiState,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    onLanguageTagChange: (String?) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Transitions are off deliberately: every destination fills the screen with the same
    // surface colour, so the default cross-fade has nothing to show — it reads as a blink.
    // Restoring the defaults brings back exactly the flicker this replaced.
    NavHost(
        navController = navController,
        startDestination = Route.TierLists,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<Route.TierLists> {
            TierListsScreen(
                onTierListClick = { id -> navController.navigate(Route.TierDetail(id)) },
                onSettingsClick = { navController.navigate(Route.Settings) },
                onNewListCreated = { id ->
                    navController.navigate(Route.TierDetail(id, startInTitleEdit = true))
                },
            )
        }
        composable<Route.TierDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.TierDetail>()
            TierDetailScreen(
                onBack = { navController.popBackStack() },
                startInTitleEdit = route.startInTitleEdit,
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onTrashClick = { navController.navigate(Route.Trash) },
                themeChoice = state.themeChoice,
                onThemeChoiceChange = onThemeChoiceChange,
                languageTag = state.languageTag,
                onLanguageTagChange = onLanguageTagChange,
            )
        }
        composable<Route.Trash> {
            TrashScreen(onBack = { navController.popBackStack() })
        }
    }
}
