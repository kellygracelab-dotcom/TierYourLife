package com.artiuillab.tieryourlife.feature.community.presentation.topic

import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

/**
 * One topic and its lists. The name and section arrive with the route, so
 * the header is right before the server answers; [topic] fills the counts in.
 */
data class TopicUiState(
    val name: String,
    val section: ListCategory?,
    val topic: Topic? = null,
    val feed: CommunityFeed = CommunityFeed.Loading,
    val sort: FeedSort = FeedSort.Popular,
    /** True once the server would not say more about the topic; the lists may still have come. */
    val headerFailed: Boolean = false,
)
