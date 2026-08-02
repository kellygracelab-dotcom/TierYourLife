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
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.tierlists.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.TierScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
            val navController = rememberNavController()

            TierYourLifeTheme(darkTheme = darkTheme) {
                NavHost(navController = navController, startDestination = Route.TierLists) {
                    composable<Route.TierLists> {
                        TierListsScreen(
                            onTierListClick = { id -> navController.navigate(Route.TierDetail(id)) },
                            onToggleTheme = { darkTheme = !darkTheme },
                        )
                    }
                    composable<Route.TierDetail> {
                        TierScreen()
                    }
                }
            }
        }
    }
}
