package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary

/** Everything the board screen draws, as one value. */
data class TierDetailScreenState(
    val board: TierDetailUiState = TierDetailUiState.Loading,
    /**
     * For the column beside this board on a wide window, read only once the
     * window has room for it. Read here: the list screen is not on the stack
     * when this one is.
     */
    val allLists: List<TierListSummary> = emptyList(),
    val signedIn: Boolean = false,
    /** The signed-in person's name, for what their published lists say about them. */
    val ownName: String? = null,
    val publish: PublishState = PublishState(),
    /** Made by New list with nothing done on it yet: Back leaves nothing behind. */
    val untouchedDraft: Boolean = false,
)

data class PublishState(
    val publishing: Boolean = false,
    val error: PublishError? = null,
    /**
     * Only ever said with proof: a board published before what was sent was
     * recorded reads as false, and the row offers to send it again anyway.
     */
    val publishedIsBehind: Boolean = false,
    /** What the switch reads while the request is in flight; waiting for the server read as a tap that missed. */
    val publicPending: Boolean? = null,
    /** Set when the switch was turned on and the list has no category yet. */
    val categoryWanted: Boolean = false,
    /** Out, but waiting for approval before the feed: its link opens all the same. */
    val awaitingApproval: Boolean = false,
)
