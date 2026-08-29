package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface CommunityRepository {

    /**
     * The published feed. [query] is a prefix match on the title, [author] the
     * uid whose lists to show; both narrow, neither is required.
     */
    suspend fun feed(
        category: ListCategory? = null,
        query: String? = null,
        author: String? = null,
    ): Result<List<PublishedListSummary>>

    suspend fun open(id: String): Result<PublishedList>

    /**
     * Publishes a snapshot and answers with the id it was stored under. Passing
     * the previous id replaces that snapshot instead of adding another.
     */
    suspend fun publish(list: TierList): Result<String>

    suspend fun unpublish(publishedId: String): Result<Unit>
}
