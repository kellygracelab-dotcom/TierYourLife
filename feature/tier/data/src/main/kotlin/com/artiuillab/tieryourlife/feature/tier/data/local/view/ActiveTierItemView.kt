package com.artiuillab.tieryourlife.feature.tier.data.local.view

import androidx.room.DatabaseView

@DatabaseView(
    viewName = "active_tier_items",
    value = "SELECT * FROM tier_items WHERE deletedAt IS NULL",
)
data class ActiveTierItemView(
    val id: Long,
    val tierId: Long,
    val position: Int,
    val title: String,
    val imageUrl: String?,
    val deletedAt: Long?,
)
