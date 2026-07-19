package com.artiuillab.tieryourlife.feature.tier.domain.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

interface TierRepository {

    suspend fun getTierListById(id: Long): TierList?
}