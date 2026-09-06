package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

const val ADDED_ITEMS_RESULT_KEY = "ai_added_item_ids"

fun NavGraphBuilder.tierDetailScreen(
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit = {},
    onOpenAiStudio: (tierListId: Long, listTitle: String) -> Unit,
) {
    composable<Route.TierDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.TierDetail>()
        val addedItemIds by backStackEntry.savedStateHandle
            .getStateFlow<List<Long>>(ADDED_ITEMS_RESULT_KEY, emptyList())
            .collectAsStateWithLifecycle()
        TierDetailScreen(
            onBack = onBack,
            onOpenAiStudio = { listTitle -> onOpenAiStudio(route.tierListId, listTitle) },
            onOpenList = onOpenList,
            addedItemIds = addedItemIds,
            onAddedItemConsumed = { backStackEntry.savedStateHandle[ADDED_ITEMS_RESULT_KEY] = ArrayList<Long>() },
        )
    }
}

fun NavGraphBuilder.trashScreen(onBack: () -> Unit) {
    composable<Route.Trash> {
        TrashScreen(onBack = onBack)
    }
}

fun NavController.navigateToTierDetail(id: Long) {
    navigate(Route.TierDetail(id))
}

fun NavController.navigateToTrash() {
    navigate(Route.Trash)
}
