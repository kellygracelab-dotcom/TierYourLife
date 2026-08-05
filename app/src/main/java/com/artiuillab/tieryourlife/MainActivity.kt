package com.artiuillab.tieryourlife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.domain.repository.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Not a ViewModel: AppPreferences is domain-layer, plain-Kotlin, synchronous —
    // exactly the shape that doesn't need one (docs/design-spec-home.md, section 7).
    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            // Seeded from the persisted choice once; every later change updates this
            // state and persists in the same place (onThemeChoiceChange below), so the
            // two never drift apart.
            var themeChoice by rememberSaveable { mutableStateOf(appPreferences.themeChoice()) }
            val darkTheme = when (themeChoice) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM -> systemDarkTheme
            }
            val onThemeChoiceChange: (ThemeChoice) -> Unit = { choice ->
                themeChoice = choice
                appPreferences.setThemeChoice(choice)
            }
            val navController = rememberNavController()

            TierYourLifeTheme(darkTheme = darkTheme) {
                NavHost(navController = navController, startDestination = Route.TierLists) {
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
                            themeChoice = themeChoice,
                            onThemeChoiceChange = onThemeChoiceChange,
                        )
                    }
                    composable<Route.Trash> {
                        TrashScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
