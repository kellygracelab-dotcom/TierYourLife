package com.artiuillab.tieryourlife.feature.tier.presentation.community

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Whose arrangement is on screen. */
enum class Showing { Theirs, Mine }

sealed interface CommunityListUiState {
    data object Loading : CommunityListUiState

    data class Success(
        /** The board as its author left it. */
        val list: TierList,
        /** The same cards, for the reader to rank themselves. */
        val mine: TierList = list,
        val showing: Showing = Showing.Theirs,
        /**
         * False on a snapshot published before the author's arrangement was
         * recorded. Then there is nothing to switch to, and offering the
         * choice would be offering an empty half.
         */
        val knowsTheirs: Boolean = true,
        val authorName: String,
        val authorUid: String = "",
        val authorPhotoUrl: String? = null,
        /** True once the reader has moved something; nothing is stored either way. */
        val arranged: Boolean = false,
        val saving: Boolean = false,
    ) : CommunityListUiState {
        /** What the board area is drawing right now. */
        val shown: TierList get() = if (showing == Showing.Theirs) list else mine
    }

    data object Error : CommunityListUiState
}
