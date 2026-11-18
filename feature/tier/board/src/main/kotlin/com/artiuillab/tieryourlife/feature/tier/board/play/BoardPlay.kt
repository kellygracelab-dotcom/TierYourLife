package com.artiuillab.tieryourlife.feature.tier.board.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.artiuillab.tieryourlife.feature.community.domain.repository.startedGameKey
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

/** Fewer cards than this and there is nothing for duels to sort out. */
const val PLAY_MIN_CARDS = 3

/** A board handed to Play: the one on screen, where a game of it is kept, and where the result goes. */
@Immutable
data class PlayRequest(
    /** What the cards are chosen from. */
    val board: TierList,
    /** One saved game per key: a board of one's own, or a published list. */
    val key: String,
    /** Someone else's list: the board the result is put into instead, Mine; [board] itself never changes then. */
    val applyTo: TierList? = null,
) {
    val intoMine: Boolean get() = applyTo != null

    companion object {
        fun ownBoard(board: TierList) = PlayRequest(board = board, key = "board:${board.id}")

        /**
         * Kept for [version] of the list alone: published again, its cards are numbered afresh,
         * and a game of the old one would hold answers about other cards.
         */
        fun someoneElses(publishedId: String, version: Long, shown: TierList, mine: TierList) =
            PlayRequest(board = shown, key = someoneElsesKey(publishedId, version), applyTo = mine)

        /** Where a game of [version] of someone else's list is kept, for whoever asks whether one is. */
        fun someoneElsesKey(publishedId: String, version: Long): String = startedGameKey(publishedId, version)

        /** Whether two keys are of the same board or list, whatever the version. */
        fun sameBoard(key: String, other: String): Boolean = key.substringBefore(VERSION_MARK) == other.substringBefore(VERSION_MARK)

        private const val VERSION_MARK = '@'
    }
}

/** The game's end on someone else's list: its result shown among the list's rankings, or sign in for that. */
@Immutable
data class PlayRankings(val signedIn: Boolean, val onShow: () -> Unit)

/**
 * Play as a board screen sees it. The screens only ask for it; navigation
 * hands in the real one from the Play feature, so neither feature opens the
 * other's screens.
 */
@Stable
interface BoardPlay {
    /** Whether a game of [key] is put down, for the dot on the button. */
    @Composable
    fun isSaved(key: String): Boolean

    /**
     * Everything Play draws over a board: the picker while [open], then the
     * duels and the result. Always composed, so what it says on the way out
     * outlives it. [onArranged] gets the board an applied result left, or
     * the one its undo put back, and which of the two it was; [onClose] says
     * Play is done with the screen, its window at the end closed too.
     * [rankings], on someone else's list, is offered in that window.
     */
    @Composable
    fun Layer(
        request: PlayRequest,
        open: Boolean,
        onArranged: (board: TierList, undone: Boolean) -> Unit,
        onClose: () -> Unit,
        rankings: PlayRankings?,
    )
}

/** Where nothing plays: previews, tests, and screens navigation gave no Play to. */
object NoPlay : BoardPlay {
    @Composable
    override fun isSaved(key: String): Boolean = false

    @Composable
    override fun Layer(
        request: PlayRequest,
        open: Boolean,
        onArranged: (board: TierList, undone: Boolean) -> Unit,
        onClose: () -> Unit,
        rankings: PlayRankings?,
    ) = Unit
}
