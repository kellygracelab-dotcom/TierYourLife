package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.feature.tier.domain.model.MovieSearchResult
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolMovieDraft
import com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch.MovieSearchScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch.MovieSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddMovieSheet(
    listTitle: String,
    onDismiss: () -> Unit,
    onMoviesConfirmed: (List<PoolMovieDraft>) -> Unit,
    searchViewModel: MovieSearchViewModel = hiltViewModel(),
) {
    val searchState by searchViewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    // Owned here, one level above MovieSearchScreenContent, so it's unaffected by that
    // composable cycling through Initial/Loading/Success/Empty/Error on every search —
    // this composable itself is mounted once for the sheet's whole lifetime, not
    // recreated per state. A Map, not a Set<Long>: confirming needs each marked item's
    // title/imageUrl to build its PoolMovieDraft, and a later search's result list no
    // longer contains the earlier marked item to read that data back from.
    var selectedItems by remember { mutableStateOf<Map<Long, MovieSearchResult>>(emptyMap()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        MovieSearchScreenContent(
            state = searchState,
            query = query,
            onQueryChange = {
                query = it
                searchViewModel.onQueryChange(it)
            },
            onSearchClick = { searchViewModel.search(query) },
            onClose = onDismiss,
            listTitle = listTitle,
            selectedIds = selectedItems.keys,
            onToggleSelection = { item ->
                selectedItems = if (selectedItems.containsKey(item.tmdbId)) {
                    selectedItems - item.tmdbId
                } else {
                    selectedItems + (item.tmdbId to item)
                }
            },
            onConfirmSelection = {
                onMoviesConfirmed(selectedItems.values.map { PoolMovieDraft(title = it.title, imageUrl = it.imageUrl) })
            },
        )
    }
}
