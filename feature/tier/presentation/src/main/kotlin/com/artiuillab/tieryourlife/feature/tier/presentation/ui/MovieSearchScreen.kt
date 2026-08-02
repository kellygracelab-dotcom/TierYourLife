package com.artiuillab.tieryourlife.feature.tier.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.presentation.state.MovieSearchUiState

@Composable
fun MovieSearchScreenContent(
    state: MovieSearchUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Movie title") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            Button(onClick = onSearchClick) {
                Text("Search")
            }
        }

        when (state) {
            is MovieSearchUiState.Initial -> {
                Text("Start typing to search for a movie")
            }

            is MovieSearchUiState.Loading -> {
                CircularProgressIndicator()
            }

            is MovieSearchUiState.Empty -> {
                Text("No results for \"${state.query}\"")
            }

            is MovieSearchUiState.Success -> {
                MovieResultsList(items = state.items)
            }

            is MovieSearchUiState.Error -> {
                Text(state.message)
            }
        }
    }
}

@Composable
private fun MovieResultsList(items: List<TierItem>) {
    LazyColumn {
        items(items) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = item.title,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private val previewMovies = listOf(
    TierItem(id = 1, title = "The Godfather", imageUrl = null),
    TierItem(id = 2, title = "Inception", imageUrl = null),
)

@Preview(name = "Initial", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenInitialPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Initial,
            query = "",
            onQueryChange = {},
            onSearchClick = {},
        )
    }
}

@Preview(name = "Loading", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenLoadingPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Loading,
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
        )
    }
}

@Preview(name = "Success", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenSuccessPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Success(items = previewMovies),
            query = "Godfather",
            onQueryChange = {},
            onSearchClick = {},
        )
    }
}

@Preview(name = "Empty", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenEmptyPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Empty(query = "zzxxccvv"),
            query = "zzxxccvv",
            onQueryChange = {},
            onSearchClick = {},
        )
    }
}

@Preview(name = "Error", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
fun MovieSearchScreenErrorPreview() {
    TierYourLifeTheme {
        MovieSearchScreenContent(
            state = MovieSearchUiState.Error(message = "No connection to server"),
            query = "Interstellar",
            onQueryChange = {},
            onSearchClick = {},
        )
    }
}
