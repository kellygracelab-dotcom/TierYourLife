package com.artiuillab.tieryourlife.feature.tier.presentation.state

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierListUiState {
    data object Loading : TierListUiState
    data class Success(val list: TierList) : TierListUiState
    data class Error(val message: String) : TierListUiState
}