package com.artiuillab.tieryourlife.feature.tier.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity

data class TierWithItems(
    @Embedded
    val tier: TierEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tierId",
    )
    val items: List<TierItemEntity>,
)
