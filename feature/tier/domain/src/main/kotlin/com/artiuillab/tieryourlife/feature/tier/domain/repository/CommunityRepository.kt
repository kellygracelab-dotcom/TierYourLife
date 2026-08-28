package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface CommunityRepository {

    suspend fun feed(): Result<List<PublishedListSummary>>

    suspend fun open(id: String): Result<PublishedList>

    /**
     * Publishes a snapshot and answers with the id it was stored under. Passing
     * the previous id replaces that snapshot instead of adding another.
     */
    suspend fun publish(list: TierList): Result<String>

    suspend fun unpublish(publishedId: String): Result<Unit>
}
