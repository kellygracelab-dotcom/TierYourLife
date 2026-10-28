package com.artiuillab.tieryourlife.feature.tier.domain.model

data class Tier(
    val id: Long,
    val label: String,
    val colorLight: String,
    val colorDark: String,
    val items: List<TierItem>,
    val isPool: Boolean = false,
    val caption: String? = null,
    /** The same on every device, unlike [id]; what a saved game names a tier by. Empty where nothing keeps one. */
    val uid: String = "",
)
