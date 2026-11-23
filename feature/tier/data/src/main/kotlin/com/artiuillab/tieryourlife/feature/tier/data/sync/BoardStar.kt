package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptBoardSummaryDto

/** The star on a board, and when it was last put on or taken off; either may be unknown. */
internal data class BoardStar(val at: Long?, val changedAt: Long?) {

    /** News to an account whose star last changed at [known]. */
    fun isNewerThan(known: Long?): Boolean = changedAt != null && (known == null || changedAt > known)
}

/** A board starred before changes were timed counts from the star itself, so it goes up once. */
internal val TierListEntity.star: BoardStar get() = BoardStar(favouritedAt, favouriteChangedAt ?: favouritedAt)

internal val KeptBoardDto.star: BoardStar get() = BoardStar(favouritedAt, favouriteChangedAt)

internal val KeptBoardSummaryDto.star: BoardStar get() = BoardStar(favouritedAt, favouriteChangedAt)

/** The later change wins, the account's on a tie; a side that never timed one loses to one that did. */
internal fun laterStar(here: BoardStar, there: BoardStar): BoardStar = when {
    there.changedAt == null -> here
    here.changedAt == null || there.changedAt >= here.changedAt -> there
    else -> here
}
