package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository

class FakeTierRepository : TierRepository {

    override suspend fun getTierListById(id: Long): TierList = tierList

    private val tierList = TierList(
        id = 1,
        title = "Favorite Movies",
        tiers = listOf(
            Tier(
                id = 1,
                label = "Masterpiece",
                color = "#FFD700",
                items = listOf(
                    TierItem(id = 1, title = "The Godfather", imageUrl = ""),
                    TierItem(id = 2, title = "Inception", imageUrl = ""),
                ),
            ),
            Tier(
                id = 2,
                label = "Solid",
                color = "#4CAF50",
                items = listOf(
                    TierItem(id = 3, title = "The Matrix", imageUrl = ""),
                ),
            ),
            Tier(
                id = 3,
                label = "Mid",
                color = "#9E9E9E",
                items = listOf(
                    TierItem(id = 4, title = "Cats (2019)", imageUrl = ""),
                ),
            ),
        ),
    )
}
