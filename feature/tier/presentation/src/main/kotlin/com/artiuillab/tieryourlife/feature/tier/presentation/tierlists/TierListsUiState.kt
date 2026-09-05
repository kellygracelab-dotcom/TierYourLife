package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.opensOn
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore

sealed interface TierListsUiState {
    data object Loading : TierListsUiState

    data class Success(
        val lists: List<TierList>,
        val totalListCount: Int,
        val rankedCount: Int,
        val mode: HomeMode = HomeMode.Browsing,
        val tab: HomeTab = HomeTab.Mine,
        /** Your own boards drawn as pictures rather than as rows. */
        val asPictures: Boolean = false,
        /** Starred boards, already in the order they belong in. */
        val favourites: List<TierList> = emptyList(),
        /** True when the two groups are drawn apart, with a heading each. */
        val grouped: Boolean = false,
        val boardSort: BoardSort = BoardSort.Newest,
        val boardFilters: BoardFilters = BoardFilters(),
        val community: CommunityFeed = CommunityFeed.Loading,
        val communityCategory: ListCategory? = null,
        /** Whose lists, and in what order. */
        val communitySource: FeedSource = FeedSource.Everyone,
        val communitySort: FeedSort = FeedSource.Everyone.opensOn,
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
 * [Unknown] is not [Kept]: Firebase answers a moment after the screen appears,
 * and treating them as one flashed the footer on every start.
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
        /** Put away in this sitting, id to whether it was also reported. A quiet note rather than vanishing; gone by the next load. */
        val justHidden: Map<String, Boolean> = emptyMap(),
    ) : CommunityFeed

    /** Its own state, not an empty [Ready]: an empty feed says there is nothing, this one has to say who there is. */
    data class FollowingNobody(
        val authors: List<SuggestedAuthor> = emptyList(),
        val loading: Boolean = true,
        /** Authors followed from this screen, which it keeps showing. */
        val followed: Set<String> = emptySet(),
    ) : CommunityFeed

    data object Failed : CommunityFeed

    /**
     * Play would not vouch for this installation. Apart from [Failed]: that
     * one says check your connection and try again, and here the connection
     * is fine and trying again is what will not work.
     */
    data object Unverified : CommunityFeed
}
