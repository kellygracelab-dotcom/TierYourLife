package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.community.domain.model.ListTranslations
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Whose arrangement is on screen. */
enum class Showing { Theirs, Mine }

/** What their own list's page offers when the board behind it is not on this phone. */
enum class OwnBoardAction { Restore, GetBack }

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
         * recorded, and on a list whose author placed nothing: a template.
         * Then there is nothing to switch to, and offering the choice would
         * be offering an empty half.
         */
        val knowsTheirs: Boolean = true,
        val authorName: String,
        val authorUid: String = "",
        val authorPhotoUrl: String? = null,
        /** A list that stands without an author: no byline, no profile, nothing to follow. */
        val editorial: Boolean = false,
        /** Somebody's list out without their name: the author line says "Anonymous" and leads nowhere. */
        val byNobody: Boolean = false,
        /** True once the reader has moved something; nothing is stored either way. */
        val arranged: Boolean = false,
        /** Null until the server answers whether this author is followed. */
        val follow: FollowState? = null,
        val saving: Boolean = false,
        /** The reader's own copy once there is one: the bar then offers to open it instead of saving again. */
        val savedId: Long? = null,
        /** How many readers have placed these cards their own way. */
        val rankingCount: Int = 0,
        /** The list this one was changed from, when the author took it from somebody. */
        val basedOn: BasedOn? = null,
        /** The section it is filed under, said beside the author where the bar has room. */
        val section: String? = null,
        /** Readers' placings, once asked for; this reader's own first. */
        val rankings: List<Ranking> = emptyList(),
        val rankingsLoading: Boolean = false,
        val moreRankings: Boolean = false,
        /** Whose placing the left half of the switch shows; null for the author's own. */
        val chosenRanking: Ranking? = null,
        /** The board as [chosenRanking] placed it, or [list] while none is chosen. */
        val theirs: TierList = list,
        /**
         * This person's own list, named or anonymous: shown as others see it,
         * with nothing to save, hide, report or follow.
         */
        val own: Boolean = false,
        /** On their own list, the board on this phone it was published from, if there is one. */
        val ownBoardId: Long? = null,
        /** The same board, waiting in the trash. */
        val trashedBoardId: Long? = null,
        /** True once this phone has been asked for the board, so "not here" is an answer rather than a guess. */
        val ownBoardLookedUp: Boolean = false,
        /** Restoring, rebuilding, republishing or taking it down: one at a time. */
        val ownBusy: Boolean = false,
        /** Out without the author's name. */
        val anonymous: Boolean = false,
        /** Their own list, asked for from its board: changed and taken down there, so not here. */
        val fromBoard: Boolean = false,
        val listId: String = "",
        /** When its author last published it: a game of it is kept for this version alone. */
        val version: Long = 0,
        /** Signed in with an account: only then can their ranking be shown to others. */
        val signedIn: Boolean = false,
        /** Their ranking on its way to the server. */
        val sendingRanking: Boolean = false,
        /**
         * Drawn from what the tapped card knew, before the server answered:
         * blank cards where the real ones will stand, nothing to move or save yet.
         */
        val arriving: Boolean = false,
        /** A house list's texts in the readers' languages; the boards above stay as written. */
        val translations: ListTranslations = ListTranslations(),
    ) : CommunityListUiState {
        /** What the board area is drawing right now. */
        val shown: TierList get() = if (showing == Showing.Theirs) theirs else mine

        /**
         * Nothing to add to or edit: their own list is changed on their board,
         * and Theirs is the author's. A move on Theirs still comes through, and
         * starts the reader's own version.
         */
        val readOnly: Boolean get() = own || showing == Showing.Theirs
        val namedAuthor: String? get() = authorName.takeUnless { editorial || byNobody || it.isBlank() }

        /** Their own list with no board here: the way to get it back. A board that is here opens instead of this page. */
        val ownBoardAction: OwnBoardAction? get() = when {
            !own || ownBoardId != null -> null
            trashedBoardId != null -> OwnBoardAction.Restore
            ownBoardLookedUp -> OwnBoardAction.GetBack
            else -> null
        }

        /** Whose placing the left half shows, by name; null when it has none to give. */
        val theirsName: String? get() = (chosenRanking?.authorName ?: authorName.takeUnless { editorial })?.takeUnless { it.isBlank() }

        /** Two halves to switch between and to choose from on saving: the author's placing, or a reader's chosen into its place. */
        val twoHalves: Boolean get() = !own && (knowsTheirs || chosenRanking != null)

        /** Somebody else's list, arranged the reader's way, by hand or by a game. */
        val canShowRanking: Boolean get() = !own && arranged
    }

    data object Error : CommunityListUiState
}
