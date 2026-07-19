package com.artiuillab.tieryourlife.feature.tier.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

data class TierListWithTiers(
    @Embedded
    val tierList: TierListEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tierListId",
        entity = TierEntity::class,
    )
    val tiers: List<TierWithItems>,
)
