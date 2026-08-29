package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PublishedLists
import javax.inject.Inject

class RoomPublishedLists @Inject constructor(
    private val dao: TierDao,
) : PublishedLists {

    override suspend fun count(): Int = dao.countPublishedLists()
}
