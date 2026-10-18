package com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation

import androidx.lifecycle.SavedStateHandle
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

/**
 * The route read straight from the handle, by property name, as Navigation
 * stores it. toRoute() would do the same through Android's Bundle, which
 * keeps a view model off the JVM. RouteFromHandleTest holds the two equal.
 */
internal fun SavedStateHandle.aiStudioRoute() = AiStudioRoute(
    tierListId = required(AiStudioRoute::tierListId.name),
    listTitle = required(AiStudioRoute::listTitle.name),
)

private fun <T : Any> SavedStateHandle.required(key: String): T =
    checkNotNull(get<T>(key)) { "Route argument $key is missing" }
