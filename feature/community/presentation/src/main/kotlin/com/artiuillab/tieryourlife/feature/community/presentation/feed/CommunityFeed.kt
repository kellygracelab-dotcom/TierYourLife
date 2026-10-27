package com.artiuillab.tieryourlife.feature.community.presentation.feed

import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic

sealed interface CommunityFeed {
    data object Loading : CommunityFeed
    data class Ready(
        val lists: List<PublishedListSummary>,
        val canLoadMore: Boolean = false,
        val loadingMore: Boolean = false,
        /** Put away in this sitting, id to whether it was also reported. A quiet note rather than vanishing; gone by the next load. */
        val justHidden: Map<String, Boolean> = emptyMap(),
        /** Who a following feed is made of; empty for everybody's feed. */
        val followed: List<FollowedAuthor> = emptyList(),
        /** And which topics; the strip shows both. */
        val followedTopics: List<Topic> = emptyList(),
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
