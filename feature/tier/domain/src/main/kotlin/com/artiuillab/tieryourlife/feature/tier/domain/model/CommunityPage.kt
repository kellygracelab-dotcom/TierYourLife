package com.artiuillab.tieryourlife.feature.tier.domain.model

/**
 * One page of the community feed. [nextCursor] is what to ask for to get the
 * page after this one, or null when there is nothing more.
 */
data class CommunityPage(
    val lists: List<PublishedListSummary>,
    val nextCursor: String? = null,
    /**
     * The feed of people you follow, from somebody who follows nobody. Not the
     * same as an empty page: there is something useful to say about it, and
     * "no lists" on its own says the wrong thing.
     */
    val followingNobody: Boolean = false,
)
