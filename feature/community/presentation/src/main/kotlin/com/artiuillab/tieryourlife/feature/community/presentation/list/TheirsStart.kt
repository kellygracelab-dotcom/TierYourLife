package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.tier.domain.lists.itemLayout
import com.artiuillab.tieryourlife.feature.tier.domain.lists.returningAllToPool
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The reader's own version put in place in one step, each with Undo and none
 * asked about first, as on the web: a move on their placing before there is a
 * version, their placing again from the menu, and every card back in the pool.
 */
internal class TheirsStart(
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val tell: suspend (ListNotice) -> Unit,
    private val replacing: () -> Unit,
    private val changed: () -> Unit,
) {
    /** Mine as it was before the last replacement, for Undo. */
    private var before: UndoStart? = null
    private var nextUndoId = 0L

    /**
     * A move while their placing is on screen and the reader has no version
     * yet: theirs, a chosen reader's included, becomes Mine with that one move
     * made. The board stays as it was under the finger.
     */
    fun move(current: CommunityListUiState.Success, itemId: Long, toTierId: Long, toPosition: Int) {
        // Cards another reader added stay in their version: Mine on this page holds the list's cards only.
        if (itemId !in current.listCards() || current.peeking) return
        replace(current, { name, id -> ListNotice.StartedFromTheirs(name, replaced = false, undoId = id) }) {
            it.theirsCards().withItemMoved(itemId, toTierId, toPosition)
        }
    }

    /** "Back to Olena's ranking": Mine becomes the placing the left half holds. */
    fun backToTheirs() {
        val current = (state.value as? CommunityListUiState.Success)?.takeIf { it.twoHalves && !it.arriving } ?: return
        replace(current, { name, id -> ListNotice.StartedFromTheirs(name, replaced = true, undoId = id) }) { it.theirsCards() }
    }

    /** "Start from scratch": every card of Mine back in the pool. */
    fun startFromScratch() {
        val current = (state.value as? CommunityListUiState.Success)?.takeIf { !it.own && !it.arriving && it.minePlaced } ?: return
        replace(current, { _, id -> ListNotice.StartedFromScratch(id) }) { it.mine.returningAllToPool() }
    }

    /** Mine as it was before, and the page as it showed it. */
    fun undo() {
        before?.let { undo(it.id) }
    }

    fun undo(id: Long) {
        val step = before?.takeIf { it.id == id } ?: return
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.arriving || current.version != step.version || current.mine.itemLayout() != step.after.itemLayout()) {
            forget(id)
            return
        }
        before = null
        state.value = current.copy(mine = step.mine, arranged = step.arranged, showing = step.showing)
        scope.launch { tell(ListNotice.TheirsUndoDismissed(id)) }
        changed()
    }

    /** Undo no longer applies: the board was reopened, emptied or changed under it. */
    fun forget() {
        val old = before ?: return
        before = null
        scope.launch { tell(ListNotice.TheirsUndoDismissed(old.id)) }
    }

    /** A late snackbar completion must never consume a newer action's snapshot. */
    fun forget(id: Long) {
        if (before?.id != id) return
        before = null
        scope.launch { tell(ListNotice.TheirsUndoDismissed(id)) }
    }

    private fun replace(
        current: CommunityListUiState.Success,
        notice: (name: String?, undoId: Long) -> ListNotice,
        newMine: (CommunityListUiState.Success) -> TierList,
    ) {
        val id = nextUndoId++
        replacing()
        state.update { now ->
            if (now !is CommunityListUiState.Success) return@update now
            now.copy(mine = newMine(now), showing = Showing.Mine, arranged = true, gameStep = null)
        }
        val after = (state.value as? CommunityListUiState.Success)?.mine ?: return
        before = UndoStart(id, current.mine, current.arranged, current.showing, after, current.version)
        scope.launch { tell(notice(current.theirsName, id)) }
        changed()
    }

    /** Their placing in Mine's frame, without the cards another reader added. */
    private fun CommunityListUiState.Success.theirsCards(): TierList {
        val listCards = listCards()
        return mine.copy(tiers = theirs.tiers.map { tier -> tier.copy(items = tier.items.filter { it.id in listCards }) })
    }

    private fun CommunityListUiState.Success.listCards(): Set<Long> = list.tiers.flatMap { it.items }.mapTo(mutableSetOf()) { it.id }

    private data class UndoStart(
        val id: Long,
        val mine: TierList,
        val arranged: Boolean,
        val showing: Showing,
        val after: TierList,
        val version: Long,
    )
}
