package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution

data class NewPoolItem(
    val title: String,
    val imageUrl: String?,
    val credit: String? = null,
    val sourceIndex: Int? = null,
    val attribution: PictureAttribution? = null,
)

data class NewTemplateTier(
    val label: String,
    val caption: String?,
    val colorLight: String,
    val colorDark: String,
    /** The cards ranked into this tier, in order. Empty for a tier nobody has filled. */
    val items: List<NewPoolItem> = emptyList(),
)
