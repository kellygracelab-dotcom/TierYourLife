package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

private val previewProfiles = listOf(
    intArrayOf(2, 2, 1, 1, 1, 6),
    intArrayOf(3, 3, 3, 2, 1, 4),
    intArrayOf(1, 2, 3, 2, 1, 2),
    intArrayOf(0, 1, 2, 3, 2, 5),
)

internal val previewTierLists = listOf(
    TierList(1, "Sci-fi films", previewTiers(2, 2, 1, 1, 1, 6)),
    TierList(2, "Every A24 film", previewTiers(3, 3, 3, 2, 1, 4)),
) + (3..15).map { listNumber ->
    val profile = previewProfiles[(listNumber - 3) % previewProfiles.size]
    TierList(
        id = -listNumber.toLong(),
        title = "Demo list ${listNumber.toString().padStart(2, '0')}",
        tiers = previewTiers(
            s = profile[0],
            a = profile[1],
            b = profile[2],
            c = profile[3],
            d = profile[4],
            pool = profile[5],
        ),
    )
}

private fun previewTiers(s: Int, a: Int, b: Int, c: Int, d: Int, pool: Int): List<Tier> = listOf(
    previewTier("S", "#B03A32", "#F1948C", s),
    previewTier("A", "#C06A25", "#E9A867", a),
    previewTier("B", "#A98B1F", "#D8C05A", b),
    previewTier("C", "#3F7F55", "#7FC393", c),
    previewTier("D", "#3C6E99", "#86B8DE", d),
    previewTier("Pool", "#DAD7E0", "#46464F", pool, true),
)

private fun previewTier(
    label: String,
    light: String,
    dark: String,
    count: Int,
    isPool: Boolean = false,
) = Tier(
    0,
    label,
    light,
    dark,
    List(count) { TierItem(it.toLong(), "", null) },
    isPool,
)
