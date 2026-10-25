package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real navigation stores a route; reading it by name must give what toRoute() gives. */
@RunWith(AndroidJUnit4::class)
class RouteFromHandleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private inline fun <reified T : Any> entryFor(route: T): NavBackStackEntry {
        var entry: NavBackStackEntry? = null
        composeRule.setContent {
            NavHost(rememberNavController(), startDestination = route) {
                composable<T> { entry = it }
            }
        }
        composeRule.waitForIdle()
        return checkNotNull(entry)
    }

    @Test
    fun theBoardRoute_readsTheSameWithoutBundle() {
        val route = Route.TierDetail(tierListId = 42L)
        val entry = entryFor(route)

        assertEquals(entry.toRoute<Route.TierDetail>(), entry.savedStateHandle.tierDetailRoute())
        assertEquals(route, entry.savedStateHandle.tierDetailRoute())
    }
}
