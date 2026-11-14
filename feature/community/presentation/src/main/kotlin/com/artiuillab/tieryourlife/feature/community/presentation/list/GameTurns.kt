package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A game's result on someone else's list and the board before it: the page's
 * own Undo and Redo, until the next change. [changed] keeps the draft and asks
 * whether to offer showing it, after a game or after a turn.
 */
internal class GameTurns(
    private val state: MutableStateFlow<CommunityListUiState>,
    private val changed: (afterGame: Boolean) -> Unit,
) {
    private var turn: Turn? = null

    /**
     * The result becomes the reader's own arrangement, on screen, still
     * unsaved. The game's own undo puts back what was on screen before,
     * Theirs and an untouched Mine included.
     */
    fun put(board: TierList, undone: Boolean) {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.arriving) return
        // No record: a republished list replaced Mine meanwhile, and the result with it.
        if (undone) {
            if (current.gameStep == GameStep.Undo) turn()
            return
        }
        val before = Side(current.mine, current.showing, current.arranged)
        val after = Side(board.inWordsOf(current.mine, current.list), Showing.Mine, arranged = true)
        turn = Turn(before, after)
        state.value = current.copy(mine = after.mine, showing = after.showing, arranged = true, gameStep = GameStep.Undo)
        changed(true)
    }

    /** The board before the game, or its result again. */
    fun turn() {
        val (before, after) = turn ?: return
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.arriving || current.gameStep == null) return
        val (side, next) = if (current.gameStep == GameStep.Undo) before to GameStep.Redo else after to GameStep.Undo
        state.value = current.copy(mine = side.mine, showing = side.showing, arranged = side.arranged, gameStep = next)
        changed(false)
    }

    /** Something else changed the board, or replaced it: there is no game to turn over any more. */
    fun forget() {
        turn = null
        state.update { if (it is CommunityListUiState.Success && it.gameStep != null) it.copy(gameStep = null) else it }
    }

    private data class Side(val mine: TierList, val showing: Showing, val arranged: Boolean)

    private data class Turn(val before: Side, val after: Side)
}
