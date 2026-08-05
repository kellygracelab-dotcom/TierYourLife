package com.artiuillab.tieryourlife.feature.tier.domain.model

// A found entry not yet added to any list's pool; used as bulk-add input, not a persisted item.
data class PoolItemDraft(
    val title: String,
    val imageUrl: String?,
)
