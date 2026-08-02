package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import javax.inject.Inject

class RoomTierRepository @Inject constructor(
    private val dao: TierDao,
) : TierRepository {

    override suspend fun getTierListById(id: Long): TierList? {
        return dao.getTierListWithTiers(id)?.toDomain()
    }

    override suspend fun getAllTierLists(): List<TierList> {
        return dao.getAllTierLists().map { it.toDomain() }
    }

    override suspend fun createTierList(title: String): Long {
        return dao.createTierListWithDefaultTier(title = title)
    }

    override suspend fun addMovieToPool(
        tierListId: Long,
        title: String,
        imageUrl: String?
    ): Long {
        return dao.addMovieToPool(tierListId, title, imageUrl)
    }
}
