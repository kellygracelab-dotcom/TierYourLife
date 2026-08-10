package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        val lists: List<TierList>,
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
    ) : TierListsUiState

    data object Error : TierListsUiState
}

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}
