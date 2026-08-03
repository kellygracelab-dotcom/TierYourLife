package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierListsUiState {
    data object Loading : TierListsUiState
    data class Success(val lists: List<TierList>) : TierListsUiState
    data class Error(val message: String) : TierListsUiState
}
