package com.artiuillab.tieryourlife.feature.tier.domain.model

data class PoolItemDraft(
    val title: String,
    val imageUrl: String?,
    /** The picture is on a site that may not keep it: copy it onto the phone rather than keep the address. */
    val keepCopy: Boolean = false,
)
