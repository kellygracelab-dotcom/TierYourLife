package com.artiuillab.tieryourlife.feature.tier.data.local.dao

data class DeletedTierListRow(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val deletedAt: Long,
    val publishedId: String?,
    val rankingOf: String?,
)
