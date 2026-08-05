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
        // Ranked items across every list, unfiltered.
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
    ) : TierListsUiState

    data class Error(val message: String) : TierListsUiState
}

// Search, selection and plain browsing are mutually exclusive by construction — there is
// exactly one HomeMode at a time, so a screen can never end up both searching and
// selecting (or show the FAB in either of those two modes) by accident.
sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}
