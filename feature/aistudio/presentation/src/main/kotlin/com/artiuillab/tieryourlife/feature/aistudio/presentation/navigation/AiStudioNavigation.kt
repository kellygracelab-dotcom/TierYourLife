package com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioScreen
import kotlinx.serialization.Serializable

@Serializable
data class AiStudioRoute(val tierListId: Long, val listTitle: String)

fun NavController.navigateToAiStudio(tierListId: Long, listTitle: String) {
    navigate(AiStudioRoute(tierListId, listTitle))
}

fun NavGraphBuilder.aiStudioScreen(
    onBack: (addedItemIds: List<Long>) -> Unit,
) {
    composable<AiStudioRoute> {
        AiStudioScreen(
            onBack = onBack,
        )
    }
}
