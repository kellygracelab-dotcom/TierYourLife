package com.artiuillab.tieryourlife.feature.tier.domain.model

data class Tier(
    val id: Long,
    val label: String,
    val colorLight: String,
    val colorDark: String,
    val items: List<TierItem>,
    val isPool: Boolean = false,
    val caption: String? = null,
)
