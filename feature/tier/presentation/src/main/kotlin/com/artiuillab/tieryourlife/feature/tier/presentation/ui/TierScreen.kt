package com.artiuillab.tieryourlife.feature.tier.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel.TierViewModel

@Composable
fun TierScreen(viewModel: TierViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    TierScreenContent(state = state)
}

@Composable
fun TierScreenContent(state: TierListUiState) {
    when (state) {
        is TierListUiState.Loading -> {
            CircularProgressIndicator()
        }

        is TierListUiState.Success -> {
            val list = state.list
            Column {
                list.tiers.forEach { tier ->
                    Text(text = tier.label)
                    Text(text = tier.items.joinToString { it.title })
                }
            }
        }

        is TierListUiState.Error -> {
            Text(text = state.message)
        }
    }
}

private val previewTierList = TierList(
    id = 1,
    title = "Favorite Movies",
    tiers = listOf(
        Tier(
            id = 1,
            label = "Masterpiece",
            color = "#FFD700",
            items = listOf(
                TierItem(id = 1, title = "The Godfather", imageUrl = ""),
                TierItem(id = 2, title = "Inception", imageUrl = ""),
            ),
        ),
        Tier(
            id = 2,
            label = "Mid",
            color = "#9E9E9E",
            items = listOf(
                TierItem(id = 3, title = "Cats (2019)", imageUrl = ""),
            ),
        ),
    ),
)

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenLoadingPreview() {
    TierYourLifeTheme {
        TierScreenContent(state = TierListUiState.Loading)
    }
}

@Preview(name = "Success", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenSuccessPreview() {
    TierYourLifeTheme {
        TierScreenContent(state = TierListUiState.Success(list = previewTierList))
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierScreenErrorPreview() {
    TierYourLifeTheme {
        TierScreenContent(state = TierListUiState.Error(message = "No connection to server"))
    }
}
