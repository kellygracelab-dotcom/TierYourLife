package com.artiuillab.tieryourlife.feature.tier.data.local.view

import androidx.room.DatabaseView

@DatabaseView(
    viewName = "active_tier_lists",
    value = "SELECT * FROM tier_lists WHERE deletedAt IS NULL",
)
data class ActiveTierListView(
    val id: Long,
    val title: String,
    val deletedAt: Long?,
    val displayMode: String,
)
