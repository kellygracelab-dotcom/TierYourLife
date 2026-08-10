package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierDetailUiState {
    data object Loading : TierDetailUiState
    data class Success(val list: TierList) : TierDetailUiState
    data object Error : TierDetailUiState
}
