package com.artiuillab.tieryourlife.feature.tier.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.state.TierListsUiState
import com.artiuillab.tieryourlife.feature.tier.presentation.viewmodel.TierListsViewModel

@Composable
fun TierListsScreen(
    onTierListClick: (Long) -> Unit,
    viewModel: TierListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var newListTitle by remember { mutableStateOf("") }

    TierListsScreenContent(
        state = state,
        newListTitle = newListTitle,
        onNewListTitleChange = { newListTitle = it },
        onCreateClick = {
            viewModel.createTierList(newListTitle)
            newListTitle = ""
        },
        onTierListClick = onTierListClick,
    )
}

@Composable
fun TierListsScreenContent(
    state: TierListsUiState,
    newListTitle: String,
    onNewListTitleChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onTierListClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newListTitle,
                onValueChange = onNewListTitleChange,
                label = { Text("New list title") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            Button(onClick = onCreateClick) {
                Text("Create")
            }
        }

        when (state) {
            is TierListsUiState.Loading -> {
                CircularProgressIndicator()
            }

            is TierListsUiState.Success -> {
                if (state.lists.isEmpty()) {
                    Text("No tier lists yet — create your first one above")
                } else {
                    TierListsList(lists = state.lists, onTierListClick = onTierListClick)
                }
            }

            is TierListsUiState.Error -> {
                Text(state.message)
            }
        }
    }
}

@Composable
private fun TierListsList(lists: List<TierList>, onTierListClick: (Long) -> Unit) {
    LazyColumn {
        items(lists) { list ->
            Text(
                text = list.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTierListClick(list.id) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

private val previewLists = listOf(
    TierList(id = 1, title = "Favorite Movies", tiers = emptyList()),
    TierList(id = 2, title = "Best Games", tiers = emptyList()),
)

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierListsScreenLoadingPreview() {
    TierYourLifeTheme {
        TierListsScreenContent(
            state = TierListsUiState.Loading,
            newListTitle = "",
            onNewListTitleChange = {},
            onCreateClick = {},
            onTierListClick = {},
        )
    }
}

@Preview(name = "Success", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierListsScreenSuccessPreview() {
    TierYourLifeTheme {
        TierListsScreenContent(
            state = TierListsUiState.Success(lists = previewLists),
            newListTitle = "",
            onNewListTitleChange = {},
            onCreateClick = {},
            onTierListClick = {},
        )
    }
}

@Preview(name = "Empty", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierListsScreenEmptyPreview() {
    TierYourLifeTheme {
        TierListsScreenContent(
            state = TierListsUiState.Success(lists = emptyList()),
            newListTitle = "",
            onNewListTitleChange = {},
            onCreateClick = {},
            onTierListClick = {},
        )
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun TierListsScreenErrorPreview() {
    TierYourLifeTheme {
        TierListsScreenContent(
            state = TierListsUiState.Error(message = "No connection to server"),
            newListTitle = "",
            onNewListTitleChange = {},
            onCreateClick = {},
            onTierListClick = {},
        )
    }
}
