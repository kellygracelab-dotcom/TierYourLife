package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore

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
        val localOnly: LocalOnly = LocalOnly.Unknown,
        val restoringPictures: PictureRestore.Progress = PictureRestore.Progress.Idle,
        /** The board whose two versions have not been mentioned yet, if there is one. */
        val conflict: TierList? = null,
    ) : TierListsUiState

    data object Error : TierListsUiState
}

sealed interface HomeMode {
    data object Browsing : HomeMode
    data class Searching(val query: String) : HomeMode
    data class Selecting(val selectedIds: Set<Long>) : HomeMode
}

enum class HomeTab { Mine, Community }

/**
 * Where these boards are kept, as far as the list screen is concerned.
 *
 * [Unknown] is not the same as [Kept]: Firebase answers a moment after the
 * screen appears, and treating the two as one made the footer line flash on
 * every start for somebody who is signed in.
 */
sealed interface LocalOnly {
    data object Unknown : LocalOnly

    /** Signed in. Nothing is said, because nothing is wrong. */
    data object Kept : LocalOnly

    /** A guest with boards. The footer always; the card once. */
    data class Here(val offerSignIn: Boolean) : LocalOnly
}

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
