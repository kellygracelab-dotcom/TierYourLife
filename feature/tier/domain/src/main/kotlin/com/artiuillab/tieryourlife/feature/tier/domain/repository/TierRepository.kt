package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface TierRepository {

    suspend fun getTierListById(id: Long): TierList?

    suspend fun getAllTierLists(): List<TierList>

    suspend fun createTierList(title: String): Long

    suspend fun addMovieToPool(tierListId: Long, title: String, imageUrl: String?): Long

    suspend fun moveItem(itemId: Long, toTierId: Long, toPosition: Int)
}