package com.artiuillab.tieryourlife.feature.tier.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker.CoverPickerScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker.TopicPickerScreen
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashScreen

const val ADDED_ITEMS_RESULT_KEY = "ai_added_item_ids"

/** Set by the cover picker on its way out, so the board behind it reads its row again. */
const val COVER_CHANGED_RESULT_KEY = "cover_changed"

/** The same, from the topic picker. */
const val TOPIC_CHANGED_RESULT_KEY = "topic_changed"

fun NavGraphBuilder.tierDetailScreen(
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit = {},
    onOpenAiStudio: (tierListId: Long, listTitle: String) -> Unit,
    onChooseCover: (tierListId: Long) -> Unit = {},
    onChooseTopic: (tierListId: Long) -> Unit = {},
) {
    composable<Route.TierDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<Route.TierDetail>()
        val addedItemIds by backStackEntry.savedStateHandle
            .getStateFlow<List<Long>>(ADDED_ITEMS_RESULT_KEY, emptyList())
            .collectAsStateWithLifecycle()
        val coverChanged by backStackEntry.savedStateHandle
            .getStateFlow(COVER_CHANGED_RESULT_KEY, false)
            .collectAsStateWithLifecycle()
        val topicChanged by backStackEntry.savedStateHandle
            .getStateFlow(TOPIC_CHANGED_RESULT_KEY, false)
            .collectAsStateWithLifecycle()
        TierDetailScreen(
            onBack = onBack,
            onOpenAiStudio = { listTitle -> onOpenAiStudio(route.tierListId, listTitle) },
            onOpenList = onOpenList,
            onChooseCover = { onChooseCover(route.tierListId) },
            addedItemIds = addedItemIds,
            onAddedItemConsumed = { backStackEntry.savedStateHandle[ADDED_ITEMS_RESULT_KEY] = ArrayList<Long>() },
            coverChanged = coverChanged,
            onCoverChangeConsumed = { backStackEntry.savedStateHandle[COVER_CHANGED_RESULT_KEY] = false },
            onChooseTopic = { onChooseTopic(route.tierListId) },
            topicChanged = topicChanged,
            onTopicChangeConsumed = { backStackEntry.savedStateHandle[TOPIC_CHANGED_RESULT_KEY] = false },
        )
    }
}

fun NavGraphBuilder.coverPickerScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    composable<Route.CoverPicker> {
        CoverPickerScreen(onBack = onBack, onSaved = onSaved)
    }
}

fun NavGraphBuilder.topicPickerScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    composable<Route.TopicPicker> {
        TopicPickerScreen(onBack = onBack, onSaved = onSaved)
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

fun NavController.navigateToCoverPicker(tierListId: Long) {
    navigate(Route.CoverPicker(tierListId))
}

fun NavController.navigateToTopicPicker(tierListId: Long) {
    navigate(Route.TopicPicker(tierListId))
}

fun NavController.navigateToTrash() {
    navigate(Route.Trash)
}
