package com.artiuillab.tieryourlife.feature.tier.data.local.dao

// Input carrier for the bulk pool insert; not an entity.
data class NewPoolMovie(
    val title: String,
    val imageUrl: String?,
)
