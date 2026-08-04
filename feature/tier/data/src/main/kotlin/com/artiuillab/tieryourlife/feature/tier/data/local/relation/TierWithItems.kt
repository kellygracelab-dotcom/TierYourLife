package com.artiuillab.tieryourlife.feature.tier.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.view.ActiveTierItemView

data class TierWithItems(
    @Embedded
    val tier: TierEntity,
    // Fetched from the active-items view: soft-deleted items never appear in the graph.
    @Relation(
        parentColumn = "id",
        entityColumn = "tierId",
        entity = ActiveTierItemView::class,
    )
    val items: List<TierItemEntity>,
)
