package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal class FakeCommunityRepositoryForDetail(
    private val publishResult: Result<Published> = Result.success(Published("published-1", "sent")),
) : FakeCommunityRepository() {
    val published = mutableListOf<TierList>()
    val unpublished = mutableListOf<String>()

    override suspend fun publish(list: TierList): Result<Published> {
        published += list
        return publishResult
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        unpublished += publishedId
        return Result.success(Unit)
    }
}
