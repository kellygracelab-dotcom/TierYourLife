package com.artiuillab.tieryourlife.feature.tier.board.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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

        fun someoneElses(publishedId: String, shown: TierList, mine: TierList) =
            PlayRequest(board = shown, key = "list:$publishedId", applyTo = mine)
    }
}

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
     * outlives it. [onArranged] gets the board an applied result (or its undo)
     * left; [onClose] says Play is done with the screen.
     */
    @Composable
    fun Layer(request: PlayRequest, open: Boolean, onArranged: (TierList) -> Unit, onClose: () -> Unit)
}

/** Where nothing plays: previews, tests, and screens navigation gave no Play to. */
object NoPlay : BoardPlay {
    @Composable
    override fun isSaved(key: String): Boolean = false

    @Composable
    override fun Layer(request: PlayRequest, open: Boolean, onArranged: (TierList) -> Unit, onClose: () -> Unit) = Unit
}
