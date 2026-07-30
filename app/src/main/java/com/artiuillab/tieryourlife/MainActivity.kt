package com.artiuillab.tieryourlife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.MovieSearchScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.TierListsScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.ui.TierScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Route.TierLists) {
                composable<Route.TierLists> {
                    TierListsScreen(
                        onTierListClick = { id -> navController.navigate(Route.TierDetail(id)) },
                    )
                }
                composable<Route.TierDetail> {
                    TierScreen()
                }
                composable<Route.MovieSearch> {
                    MovieSearchScreen()
                }
            }
        }
    }
}