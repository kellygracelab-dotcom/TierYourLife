package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierList(
    val id: Long,
    val title: String,
    val tiers: List<Tier>,
    val displayMode: TierListDisplayMode = TierListDisplayMode.WRAP,
    /** Set once this list has been published; the id the server keeps it under. */
    val publishedId: String? = null,
    /** Set on a copy taken from someone else's published list. */
    val authorName: String? = null,
)
