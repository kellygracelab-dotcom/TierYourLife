package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierList(
    val id: Long,
    val title: String,
    val tiers: List<Tier>,
    val displayMode: TierListDisplayMode = TierListDisplayMode.WRAP,
)