package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch.MovieSearchScreenContent
import com.artiuillab.tieryourlife.feature.tier.presentation.moviesearch.MovieSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddMovieSheet(
    onDismiss: () -> Unit,
    onMovieSelected: (title: String, imageUrl: String?) -> Unit,
    searchViewModel: MovieSearchViewModel = hiltViewModel(),
) {
    val searchState by searchViewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        MovieSearchScreenContent(
            state = searchState,
            query = query,
            onQueryChange = { query = it },
            onSearchClick = { searchViewModel.search(query) },
            onItemClick = { item -> onMovieSelected(item.title, item.imageUrl) },
        )
    }
}
