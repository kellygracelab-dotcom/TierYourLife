package com.artiuillab.tieryourlife.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.theme.ui.CoverSurface
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CoverCommunityScreen
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.navigateToSection
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.sectionScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverHandOffSubject
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverHandOffWait
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverListPane
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.CoverListsPane
import com.artiuillab.tieryourlife.feature.tier.presentation.cover.coverListsState
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
private data object CoverLists

@Serializable
private data class CoverList(val listId: Long)

@Serializable
private data object CoverCommunity

/**
 * The app on the Flip's cover (§1): your lists, a list's overview, Community
 * and a section, moved through by taps and vertical scrolling only, so
 * nothing argues with the cover's own sideways swipe. Anything more is
 * handed to the main screen.
 *
 * [boardOnMain] is the board the main screen had open when the phone was
 * shut: the cover opens on that list, with your lists one step back.
 */
@Composable
internal fun CoverHost(
    handOff: HandOff?,
    onHandOff: (CoverHandOffSubject) -> Unit,
    onCancelHandOff: () -> Unit,
    boardOnMain: Long?,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    val navController = rememberNavController()
    val lists = coverListsState()
    var lastOpened by rememberSaveable { mutableStateOf(boardOnMain) }
    var openedFromMain by rememberSaveable { mutableStateOf(false) }

    CoverSurface {
        // Hidden from a screen reader while the wait stands over it, as it is from a finger.
        NavHost(
            navController = navController,
            startDestination = CoverLists,
            modifier = if (handOff != null) Modifier.clearAndSetSemantics {} else Modifier,
        ) {
            composable<CoverLists> {
                CoverListsPane(
                    state = lists,
                    lastOpenedId = lastOpened,
                    onOpen = { id ->
                        lastOpened = id
                        navController.navigate(CoverList(id))
                    },
                    onCommunity = { navController.navigate(CoverCommunity) },
                    onHandOff = onHandOff,
                )
            }
            composable<CoverList> { entry ->
                CoverListPane(
                    state = lists,
                    listId = entry.toRoute<CoverList>().listId,
                    onBack = { navController.popBackStack() },
                    onOpenList = { id ->
                        lastOpened = id
                        navController.openInstead(id)
                    },
                    onHandOff = onHandOff,
                )
            }
            composable<CoverCommunity> {
                CoverCommunityScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSection = { route -> navController.navigateToSection(route) },
                )
            }
            sectionScreen(
                onBack = { navController.popBackStack() },
                onOpenList = { route -> onHandOff(CoverHandOffSubject.CommunityList(route.publishedId, route.title.orEmpty())) },
                // The cover's rows show no faces to tap.
                onAuthorClick = { _, _, _ -> },
            )
        }
        if (handOff != null) {
            BackHandler(onBack = onCancelHandOff)
            LaunchedEffect(handOff) {
                delay(HandOffRules.remainingMillis(handOff, nowMillis()))
                onCancelHandOff()
            }
            CoverHandOffWait(subject = handOff.subject, onCancel = onCancelHandOff)
        }
    }

    // Once, on the way in: a phone shut on a board opens the cover on that list.
    LaunchedEffect(Unit) {
        if (!openedFromMain && boardOnMain != null) {
            openedFromMain = true
            navController.navigate(CoverList(boardOnMain))
        }
    }
}

/** The next list takes this one's place, so back still leads to your lists rather than through every list seen. */
private fun NavHostController.openInstead(listId: Long) {
    navigate(CoverList(listId)) {
        popUpTo<CoverList> { inclusive = true }
    }
}
