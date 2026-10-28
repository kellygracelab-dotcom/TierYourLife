package com.artiuillab.tieryourlife.feature.tier.domain.model

data class TierItem(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val source: TierItemSource = TierItemSource.MANUAL,
    /** The same on every device, unlike [id]; what a saved game names a card by. Empty where nothing keeps one. */
    val uid: String = "",
)
