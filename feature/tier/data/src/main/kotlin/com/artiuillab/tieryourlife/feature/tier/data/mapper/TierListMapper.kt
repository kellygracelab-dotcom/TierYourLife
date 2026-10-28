package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierWithItems
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode

internal fun TierListEntity.toDomain(): TierList = TierList(
    id = id,
    title = title,
    tiers = emptyList(),
    displayMode = displayMode.toDisplayMode(),
    publishedId = publishedId,
    publishedFingerprint = publishedFingerprint,
    authorName = authorName,
    category = category,
    anonymous = anonymous,
    coverImageUrl = coverImageUrl,
    coverImageUrls = coverImageUrls.toCoverImageUrls(),
    topicId = topicId,
    topicName = topicName,
    newTopicName = newTopicName,
    takenFrom = takenFrom,
    rankingOf = rankingOf,
    arrivedFrom = arrivedFrom,
    editedAt = editedAt,
    favouritedAt = favouritedAt,
)

internal fun TierListWithTiers.toDomain(): TierList = TierList(
    id = tierList.id,
    title = tierList.title,
    tiers = tiers
        .sortedBy { it.tier.position }
        .map { it.toDomain() },
    displayMode = tierList.displayMode.toDisplayMode(),
    publishedId = tierList.publishedId,
    publishedFingerprint = tierList.publishedFingerprint,
    authorName = tierList.authorName,
    category = tierList.category,
    anonymous = tierList.anonymous,
    coverImageUrl = tierList.coverImageUrl,
    coverImageUrls = tierList.coverImageUrls.toCoverImageUrls(),
    topicId = tierList.topicId,
    topicName = tierList.topicName,
    newTopicName = tierList.newTopicName,
    takenFrom = tierList.takenFrom,
    rankingOf = tierList.rankingOf,
    arrivedFrom = tierList.arrivedFrom,
    editedAt = tierList.editedAt,
    favouritedAt = tierList.favouritedAt,
)

private fun String.toDisplayMode(): TierListDisplayMode =
    TierListDisplayMode.entries.firstOrNull { it.name == this } ?: TierListDisplayMode.WRAP

private fun TierWithItems.toDomain(): Tier = Tier(
    id = tier.id,
    label = tier.label,
    colorLight = tier.colorLight,
    colorDark = tier.colorDark,
    items = items
        .sortedBy { it.position }
        .map { it.toDomain() },
    isPool = tier.isPool,
    caption = tier.caption,
)

private fun TierItemEntity.toDomain(): TierItem = TierItem(
    id = id,
    title = title,
    imageUrl = imageUrl,
    source = source.toTierItemSource(),
)

private fun String.toTierItemSource(): TierItemSource =
    TierItemSource.entries.firstOrNull { it.name == this } ?: TierItemSource.MANUAL

private const val SMALLEST_COLLAGE = 2

/** One address per line: no URL has a line break in it, and a line needs no parser. */
internal fun List<String>.toStoredCoverImageUrls(): String? =
    takeIf { it.size >= SMALLEST_COLLAGE }?.joinToString("\n")

internal fun String?.toCoverImageUrls(): List<String> = this?.lines()?.filter { it.isNotBlank() }.orEmpty()
