package com.artiuillab.tieryourlife.feature.tier.presentation.community

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface CommunityListUiState {
    data object Loading : CommunityListUiState

    data class Success(
        val list: TierList,
        val authorName: String,
        /** True once the reader has moved something; nothing is stored either way. */
        val arranged: Boolean = false,
        val saving: Boolean = false,
    ) : CommunityListUiState

    data object Error : CommunityListUiState
}
