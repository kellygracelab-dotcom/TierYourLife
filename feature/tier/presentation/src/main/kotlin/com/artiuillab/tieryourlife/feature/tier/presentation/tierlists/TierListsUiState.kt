package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        // Already filtered by the search query, if any — the composable never filters.
        val lists: List<TierList>,
        // Every list, unfiltered — the summary line needs this even while a search
        // query has narrowed `lists` down to a subset.
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
    ) : TierListsUiState

    data class Error(val message: String) : TierListsUiState
}

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}
