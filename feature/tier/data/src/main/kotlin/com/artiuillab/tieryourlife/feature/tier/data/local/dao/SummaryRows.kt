package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity

/** One tier of a board, with how many cards it holds rather than the cards. */
data class TierCountRow(
    val id: Long,
    val tierListId: Long,
    val position: Int,
    val colorLight: String,
    val colorDark: String,
    val isPool: Boolean,
    val cards: Int,
)

/** Everything a list of boards draws, read in one transaction. */
data class SummaryRows(
    val boards: List<TierListEntity>,
    val tiers: List<TierCountRow>,
    val previews: Map<Long, List<String>>,
)
