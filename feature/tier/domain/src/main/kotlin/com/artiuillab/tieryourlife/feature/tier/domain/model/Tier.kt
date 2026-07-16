package com.artiuillab.tieryourlife.feature.tier.domain.model

data class Tier(
    val id: Long,
    val label: String,
    val color: String,
    val items: List<TierItem>
)