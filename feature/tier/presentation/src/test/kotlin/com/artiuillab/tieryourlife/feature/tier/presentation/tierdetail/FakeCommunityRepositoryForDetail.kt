package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal class FakeCommunityRepositoryForDetail(
    private val publishResult: Result<Published> = Result.success(Published("published-1", "sent")),
    /** What the server says of any topic it is asked about. */
    private val topicStatus: TopicStatus = TopicStatus.Pending,
) : FakeCommunityRepository() {
    val published = mutableListOf<TierList>()
    val unpublished = mutableListOf<String>()
    val withdrawn = mutableListOf<String>()

    override suspend fun withdrawRanking(listId: String): Result<Unit> {
        withdrawn += listId
        return Result.success(Unit)
    }

    override suspend fun topic(id: String): Result<Topic> = Result.success(
        Topic(id = id, name = "Oscars 2025", section = ListCategory.FilmTv.takeIf { topicStatus == TopicStatus.Approved }, status = topicStatus),
    )

    override suspend fun publish(list: TierList): Result<Published> {
        published += list
        return publishResult
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        unpublished += publishedId
        return Result.success(Unit)
    }
}
