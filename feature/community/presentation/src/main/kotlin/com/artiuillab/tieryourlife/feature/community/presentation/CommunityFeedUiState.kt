package com.artiuillab.tieryourlife.feature.community.presentation

import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.opensOn
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

data class CommunityFeedUiState(
    val feed: CommunityFeed = CommunityFeed.Loading,
    val category: ListCategory? = null,
    /** Whose lists, and in what order. */
    val source: FeedSource = FeedSource.Everyone,
    val sort: FeedSort = FeedSource.Everyone.opensOn,
    /** Non-null while the search bar is up; the feed is then the server's answer to it. */
    val query: String? = null,
)

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
