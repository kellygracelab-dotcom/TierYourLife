package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierWithItems
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

// Overview-only mapping — tiers are intentionally not loaded here (list screens
// only need id/title; full tier/item graph is fetched by id via getTierListById).
internal fun TierListEntity.toDomain(): TierList = TierList(
    id = id,
    title = title,
    tiers = emptyList(),
)

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
    colorLight = tier.colorLight,
    colorDark = tier.colorDark,
    items = items
        .sortedBy { it.position }
        .map { it.toDomain() },
    isPool = tier.isPool,
)

private fun TierItemEntity.toDomain(): TierItem = TierItem(
    id = id,
    title = title,
    imageUrl = imageUrl,
)
