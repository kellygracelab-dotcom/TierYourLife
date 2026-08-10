package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

sealed interface TrashUiState {
    data object Loading : TrashUiState
    data class Success(val entries: List<TrashEntry>) : TrashUiState
    data object Error : TrashUiState
}
