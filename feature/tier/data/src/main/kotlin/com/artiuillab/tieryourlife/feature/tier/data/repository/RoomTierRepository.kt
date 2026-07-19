package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.data.mapper.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

class RoomTierRepository(
    private val dao: TierDao,
) : TierRepository {

    override suspend fun getTierListById(id: Long): TierList? {
        return dao.getTierListWithTiers(id)?.toDomain()
    }
}