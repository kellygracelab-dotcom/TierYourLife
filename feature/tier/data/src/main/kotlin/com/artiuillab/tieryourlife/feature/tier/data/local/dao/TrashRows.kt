package com.artiuillab.tieryourlife.feature.tier.data.local.dao


data class DeletedTierListRow(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val deletedAt: Long,
)

data class DeletedTierItemRow(
    val id: Long,
    val title: String,
    val listTitle: String,
    val wasInPool: Boolean,
    val deletedAt: Long,
    val imageUrl: String?,
)
