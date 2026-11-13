package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeCommunityRepositoryForDetail(
    private val publishResult: Result<Published> = Result.success(Published("published-1", "sent")),
    /** What the server says of any topic it is asked about. */
    private val topicStatus: TopicStatus = TopicStatus.Pending,
    private val publicationKind: PublicationKind? = null,
    /** What opening any published list finds; unset, opening fails. */
    private val original: PublishedList? = null,
) : FakeCommunityRepository() {
    val published = mutableListOf<TierList>()

    // The same, as a flow to wait on: the screen's state need not change when a publish lands.
    val sent = MutableStateFlow<List<TierList>>(emptyList())
    val unpublished = mutableListOf<String>()
    val withdrawn = mutableListOf<String>()

    override suspend fun classifyPublication(list: TierList): Result<PublicationKind> = Result.success(
        publicationKind ?: publishResult.getOrNull()?.takeIf { it.ranking }?.let { PublicationKind.Ranking(it.id, 1) } ?: PublicationKind.NewList(),
    )

    val opened = mutableListOf<String>()

    override suspend fun open(id: String): Result<PublishedList> {
        opened += id
        return original?.let { Result.success(it) } ?: super.open(id)
    }

    override suspend fun withdrawRanking(listId: String): Result<Unit> {
        withdrawn += listId
        return Result.success(Unit)
    }

    override suspend fun topic(id: String): Result<Topic> = Result.success(
        Topic(id = id, name = "Oscars 2025", section = ListCategory.FilmTv.id.takeIf { topicStatus == TopicStatus.Approved }, status = topicStatus),
    )

    override suspend fun publish(list: TierList): Result<Published> {
        published += list
        sent.update { it + list }
        return publishResult
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> {
        unpublished += publishedId
        return Result.success(Unit)
    }
}
