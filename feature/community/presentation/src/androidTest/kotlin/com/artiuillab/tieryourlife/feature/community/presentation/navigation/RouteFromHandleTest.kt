package com.artiuillab.tieryourlife.feature.community.presentation.navigation

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
    fun aPublishedListRoute_readsTheSameWithoutBundle() {
        val route = CommunityRoute.CommunityList(publishedId = "abc")
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.CommunityList>(), entry.savedStateHandle.communityListRoute())
        assertEquals(route, entry.savedStateHandle.communityListRoute())
    }

    @Test
    fun aPublishedListRouteWithWhatTheCardShowed_readsTheSameWithoutBundle() {
        val route = CommunityRoute.CommunityList(
            publishedId = "abc",
            title = "Every A24 film",
            authorName = "Olena M.",
            itemCount = 12,
            tierColors = "#B03A32,#C06A25",
        )
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.CommunityList>(), entry.savedStateHandle.communityListRoute())
        assertEquals(route, entry.savedStateHandle.communityListRoute())
    }

    @Test
    fun anAuthorRouteWithAFace_readsTheSameWithoutBundle() {
        val route = CommunityRoute.Author(authorUid = "u1", authorName = "Ann", authorPhotoUrl = "https://example.test/a.jpg")
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.Author>(), entry.savedStateHandle.authorRoute())
        assertEquals(route, entry.savedStateHandle.authorRoute())
    }

    @Test
    fun aTopicRouteWithWhatTheChipShowed_readsTheSameWithoutBundle() {
        val route = CommunityRoute.Topic(topicId = "t1", name = "Pokémon", section = "games")
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.Topic>(), entry.savedStateHandle.topicRoute())
        assertEquals(route, entry.savedStateHandle.topicRoute())
    }

    @Test
    fun aTopicRouteByIdAlone_readsTheSameWithoutBundle() {
        val route = CommunityRoute.Topic(topicId = "t1")
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.Topic>(), entry.savedStateHandle.topicRoute())
        assertEquals(route, entry.savedStateHandle.topicRoute())
    }

    @Test
    fun anAuthorRouteWithoutAFace_readsTheSameWithoutBundle() {
        val route = CommunityRoute.Author(authorUid = "u1", authorName = "Ann")
        val entry = entryFor(route)

        assertEquals(entry.toRoute<CommunityRoute.Author>(), entry.savedStateHandle.authorRoute())
        assertEquals(route, entry.savedStateHandle.authorRoute())
    }
}
