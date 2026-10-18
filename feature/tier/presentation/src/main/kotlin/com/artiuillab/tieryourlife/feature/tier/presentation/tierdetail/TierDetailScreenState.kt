package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Everything the board screen draws, as one value. */
data class TierDetailScreenState(
    val board: TierDetailUiState = TierDetailUiState.Loading,
    /** For the column beside this board on a wide window. Read here: the list screen is not on the stack when this one is. */
    val allLists: List<TierList> = emptyList(),
    val signedIn: Boolean = false,
    val publish: PublishState = PublishState(),
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
)
