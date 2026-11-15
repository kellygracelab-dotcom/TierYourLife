package com.artiuillab.tieryourlife.feature.tier.data.local.dao

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus

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
    /** By board, the card pictures somebody moved and where to. */
    val focuses: Map<Long, Map<String, CoverFocus>> = emptyMap(),
)

/** A card picture somebody moved, with the board it is on. */
data class FocusedPictureRow(
    val tierListId: Long,
    val imageUrl: String,
    val focusX: Float,
    val focusY: Float,
    val focusZoom: Float,
)
