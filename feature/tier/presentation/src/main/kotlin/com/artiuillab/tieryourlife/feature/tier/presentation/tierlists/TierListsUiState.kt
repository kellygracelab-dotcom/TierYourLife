package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        val lists: List<TierList>,
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
        val tab: HomeTab = HomeTab.Mine,
        val community: CommunityFeed = CommunityFeed.Loading,
        val communityCategory: ListCategory? = null,
    ) : TierListsUiState

    data object Error : TierListsUiState
}

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}

enum class HomeTab { Mine, Community }

sealed interface CommunityFeed {
    data object Loading : CommunityFeed
    data class Ready(
        val lists: List<PublishedListSummary>,
        val canLoadMore: Boolean = false,
        val loadingMore: Boolean = false,
        /**
         * Put away in this sitting, id to whether it was also reported.
         * Their cards become a quiet note rather than vanishing: silence
         * reads as "deleted", which is not what happened. The notes are
         * gone by the next load, so the feed keeps no scars.
         */
        val justHidden: Map<String, Boolean> = emptyMap(),
    ) : CommunityFeed
    data object Failed : CommunityFeed
}
