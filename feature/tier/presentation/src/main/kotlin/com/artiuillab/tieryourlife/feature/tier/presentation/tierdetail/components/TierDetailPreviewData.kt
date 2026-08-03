package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal val previewTierList = TierList(
    id = 1,
    title = "Sci-fi films",
    tiers = listOf(
        Tier(
            id = 1,
            label = "S",
            colorLight = "#B03A32",
            colorDark = "#F1948C",
            items = listOf(
                TierItem(id = 1, title = "Interstellar", imageUrl = null),
                TierItem(id = 2, title = "Arrival", imageUrl = null),
            ),
        ),
        Tier(
            id = 2,
            label = "A",
            colorLight = "#C06A25",
            colorDark = "#E9A867",
            items = listOf(
                TierItem(id = 3, title = "Blade Runner 2049", imageUrl = null),
                TierItem(id = 4, title = "Dune", imageUrl = null),
            ),
        ),
        Tier(
            id = 3,
            label = "B",
            colorLight = "#A98B1F",
            colorDark = "#D8C05A",
            items = listOf(TierItem(id = 5, title = "Inception", imageUrl = null)),
        ),
        Tier(
            id = 4,
            label = "C",
            colorLight = "#3F7F55",
            colorDark = "#7FC393",
            items = listOf(TierItem(id = 6, title = "Enemy", imageUrl = null)),
        ),
        Tier(
            id = 5,
            label = "D",
            colorLight = "#3C6E99",
            colorDark = "#86B8DE",
            items = listOf(TierItem(id = 7, title = "First Man", imageUrl = null)),
        ),
        Tier(
            id = 6,
            label = "Pool",
            colorLight = "#DAD7E0",
            colorDark = "#46464F",
            items = listOf(
                TierItem(id = 8, title = "Ex Machina", imageUrl = null),
                TierItem(id = 9, title = "Annihilation", imageUrl = null),
                TierItem(id = 10, title = "Solaris", imageUrl = null),
                TierItem(id = 11, title = "Gattaca", imageUrl = null),
                TierItem(id = 12, title = "Moon", imageUrl = null),
            ),
            isPool = true,
        ),
    ),
)
