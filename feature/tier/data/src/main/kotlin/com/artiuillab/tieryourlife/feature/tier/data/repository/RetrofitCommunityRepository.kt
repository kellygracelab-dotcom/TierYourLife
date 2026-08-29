package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.CommunityApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitCommunityRepository @Inject constructor(
    private val api: CommunityApi,
) : CommunityRepository {

    override suspend fun feed(category: ListCategory?): Result<List<PublishedListSummary>> = runCatching {
        api.feed(category?.id).lists.map { it.toSummary() }
    }

    override suspend fun open(id: String): Result<PublishedList> = runCatching {
        api.open(id).toDomain()
    }

    override suspend fun publish(list: TierList): Result<String> = try {
        val request = list.toRequest()
        val existing = list.publishedId
        Result.success(if (existing == null) api.publish(request).id else api.republish(existing, request).id)
    } catch (e: Exception) {
        Result.failure(PublishRefused(e.asPublishError()))
    }

    override suspend fun unpublish(publishedId: String): Result<Unit> = runCatching {
        api.unpublish(publishedId)
    }
}

private fun Throwable.asPublishError(): PublishError = when {
    this is HttpException -> when (code()) {
        403 -> PublishError.NotSignedIn
        409 -> PublishError.TooManyLists
        else -> PublishError.Unknown
    }

    this is IOException -> PublishError.Offline
    else -> PublishError.Unknown
}

private fun PublishedListSummaryDto.toSummary() = PublishedListSummary(
    id = id,
    title = title,
    authorName = authorName,
    category = ListCategory.fromId(category) ?: ListCategory.Other,
    itemCount = itemCount,
    coverImageUrl = coverImageUrl,
    previewImages = previewImages,
    tierColors = tierColors,
    updatedAtMillis = updatedAt,
)

private fun PublishedListDto.toDomain() = PublishedList(
    summary = PublishedListSummary(
        id = id,
        title = title,
        authorName = authorName,
        category = ListCategory.fromId(category) ?: ListCategory.Other,
        itemCount = itemCount,
        coverImageUrl = coverImageUrl,
        previewImages = previewImages,
        tierColors = tierColors,
        updatedAtMillis = updatedAt,
    ),
    tiers = tiers.mapIndexed { index, tier ->
        Tier(
            id = index.toLong(),
            label = tier.label,
            caption = tier.caption,
            colorLight = tier.colorLight,
            colorDark = tier.colorDark,
            items = emptyList(),
        )
    },
    items = items.mapIndexed { index, item ->
        TierItem(id = index.toLong(), title = item.title, imageUrl = item.imageUrl)
    },
)

// Only the tier definitions and the cards travel; where the author put them is
// theirs, and the reader ranks from scratch.
private fun TierList.toRequest() = PublishListRequestDto(
    title = title,
    category = (category ?: ListCategory.Other).id,
    coverImageUrl = coverImageUrl,
    tiers = tiers.filterNot { it.isPool }.map {
        PublishedTierDto(it.label, it.caption, it.colorLight, it.colorDark)
    },
    items = tiers.flatMap { it.items }.map { PublishedItemDto(it.title, it.imageUrl) },
)
