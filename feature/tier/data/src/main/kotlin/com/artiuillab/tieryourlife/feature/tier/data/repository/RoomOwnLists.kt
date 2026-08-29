package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import javax.inject.Inject

class RoomOwnLists @Inject constructor(
    private val dao: TierDao,
) : OwnLists {

    override suspend fun publishedCount(): Int = dao.countPublishedLists()

    override suspend fun cardImages(limit: Int): List<String> = dao.cardImageUrls(limit)
}
