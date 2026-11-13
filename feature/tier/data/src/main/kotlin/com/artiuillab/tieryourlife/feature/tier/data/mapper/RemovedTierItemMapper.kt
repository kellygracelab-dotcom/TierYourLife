package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.domain.model.RemovedTierItem

internal fun TierItemEntity.toRemoved(tierListId: Long): RemovedTierItem = RemovedTierItem(
    id = id,
    uid = uid,
    tierListId = tierListId,
    tierId = tierId,
    position = position,
    title = title,
    imageUrl = imageUrl,
    source = source.toTierItemSource(),
    credit = credit,
    attribution = attribution,
    sourceIndex = sourceIndex,
)

/** The same id and uid as before: a synced board, a saved game and the web all know the card by them. */
internal fun RemovedTierItem.toEntity(): TierItemEntity = TierItemEntity(
    id = id,
    tierId = tierId,
    position = position,
    title = title,
    imageUrl = imageUrl,
    source = source.name,
    uid = uid,
    credit = credit,
    sourceIndex = sourceIndex,
    attribution = attribution,
)
