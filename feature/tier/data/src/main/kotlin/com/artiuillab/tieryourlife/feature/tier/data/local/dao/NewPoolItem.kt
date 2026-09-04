package com.artiuillab.tieryourlife.feature.tier.data.local.dao

data class NewPoolItem(
    val title: String,
    val imageUrl: String?,
)

data class NewTemplateTier(
    val label: String,
    val caption: String?,
    val colorLight: String,
    val colorDark: String,
    /** The cards ranked into this tier, in order. Empty for a tier nobody has filled. */
    val items: List<NewPoolItem> = emptyList(),
)
