package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierWithItems
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal fun TierListWithTiers.toDomain(): TierList = TierList(
    id = tierList.id,
    title = tierList.title,
    tiers = tiers
        .sortedBy { it.tier.position }
        .map { it.toDomain() },
)

private fun TierWithItems.toDomain(): Tier = Tier(
    id = tier.id,
    label = tier.label,
    color = tier.color,
    items = items
        .sortedBy { it.position }
        .map { it.toDomain() },
)

private fun TierItemEntity.toDomain(): TierItem = TierItem(
    id = id,
    title = title,
    imageUrl = imageUrl,
)
