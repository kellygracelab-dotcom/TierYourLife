package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.TierDao
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import javax.inject.Inject

class RoomOwnLists @Inject constructor(
    private val dao: TierDao,
    private val ownership: BoardOwnership,
) : OwnLists {

    override suspend fun publishedCount(): Int = dao.countPublishedLists(ownership.current())

    override suspend fun boardCount(): Int = dao.countBoards(ownership.current())

    override suspend fun cardImages(limit: Int): List<String> = dao.cardImageUrls(ownership.current(), limit)
}
