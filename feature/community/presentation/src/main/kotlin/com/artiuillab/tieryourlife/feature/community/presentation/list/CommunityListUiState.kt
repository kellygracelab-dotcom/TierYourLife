package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
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
        /** A list that stands without an author: no byline, no profile, nothing to follow. */
        val editorial: Boolean = false,
        /** True once the reader has moved something; nothing is stored either way. */
        val arranged: Boolean = false,
        /** Null until the server answers whether this author is followed. */
        val follow: FollowState? = null,
        val saving: Boolean = false,
        /** The reader's own copy once there is one: the bar then offers to open it instead of saving again. */
        val savedId: Long? = null,
        /** How many readers have placed these cards their own way. */
        val rankingCount: Int = 0,
        /** How many readers saved a copy: the number on the chip, as on every feed card. */
        val takeCount: Int = 0,
        /** The list this one was changed from, when the author took it from somebody. */
        val basedOn: BasedOn? = null,
        /** The section it is filed under, said beside the author where the bar has room. */
        val section: ListCategory? = null,
        /** Readers' placings, once asked for; this reader's own first. */
        val rankings: List<Ranking> = emptyList(),
        val rankingsLoading: Boolean = false,
        val moreRankings: Boolean = false,
        /** Whose placing the left half of the switch shows; null for the author's own. */
        val chosenRanking: Ranking? = null,
        /** The board as [chosenRanking] placed it, or [list] while none is chosen. */
        val theirs: TierList = list,
    ) : CommunityListUiState {
        /** What the board area is drawing right now. */
        val shown: TierList get() = if (showing == Showing.Theirs) theirs else mine
    }

    data object Error : CommunityListUiState
}
