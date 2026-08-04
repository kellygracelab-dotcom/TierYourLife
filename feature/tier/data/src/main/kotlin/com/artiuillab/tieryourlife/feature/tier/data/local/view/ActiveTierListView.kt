package com.artiuillab.tieryourlife.feature.tier.data.local.view

import androidx.room.DatabaseView

// Read queries go through this view so the soft-delete filter physically cannot be
// forgotten in a new query: rows in trash simply do not exist here.
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
