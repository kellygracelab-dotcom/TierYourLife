package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A move while their arrangement is on screen: the reader's own version
 * begins as what they were looking at, a chosen reader's placing included,
 * with that one move made. The board stays as it was under the finger.
 * A Mine with a card in a tier is asked about first, each time, as on the web.
 */
internal class TheirsStart(
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val tell: suspend (ListNotice) -> Unit,
    private val replacing: () -> Unit,
    private val changed: () -> Unit,
) {
    /** Mine as it was before a move on Theirs replaced it, for Undo. */
    private var before: Pair<TierList, Boolean>? = null

    /** The move on Theirs that asked before replacing Mine. */
    private var asked: Triple<Long, Long, Int>? = null

    fun move(current: CommunityListUiState.Success, itemId: Long, toTierId: Long, toPosition: Int) {
        // Cards another reader added stay in their version: Mine on this page holds the list's cards only.
        if (itemId !in current.listCards()) return
        if (current.arranged && current.mine.tiers.any { !it.isPool && it.items.isNotEmpty() }) {
            asked = Triple(itemId, toTierId, toPosition)
            state.value = current.copy(replaceAsk = ReplaceAsk(current.theirsName))
            return
        }
        start(current, itemId, toTierId, toPosition, replaced = false)
    }

    /** Replace: Mine starts from Theirs with the move that asked. */
    fun replace() {
        val (itemId, toTierId, toPosition) = asked ?: return
        asked = null
        val current = state.value as? CommunityListUiState.Success ?: return
        start(current, itemId, toTierId, toPosition, replaced = true)
    }

    /** Cancel: the move is not made, and Theirs stays on screen as it was. */
    fun keep() {
        asked = null
        state.update { if (it is CommunityListUiState.Success) it.copy(replaceAsk = null) else it }
    }

    /** Mine as it was before, and their arrangement back on screen. */
    fun undo() {
        val (mine, arranged) = before ?: return
        before = null
        state.update { if (it is CommunityListUiState.Success) it.copy(mine = mine, arranged = arranged, showing = Showing.Theirs) else it }
        changed()
    }

    /** Undo no longer applies: the board was reopened, emptied or changed under it. */
    fun forget() {
        before = null
    }

    private fun start(current: CommunityListUiState.Success, itemId: Long, toTierId: Long, toPosition: Int, replaced: Boolean) {
        val listCards = current.listCards()
        before = current.mine to current.arranged
        replacing()
        state.update { now ->
            if (now !is CommunityListUiState.Success) return@update now
            val theirs = now.theirs.tiers.map { tier -> tier.copy(items = tier.items.filter { it.id in listCards }) }
            now.copy(
                mine = now.mine.copy(tiers = theirs).withItemMoved(itemId, toTierId, toPosition),
                showing = Showing.Mine,
                arranged = true,
                replaceAsk = null,
            )
        }
        scope.launch { tell(ListNotice.StartedFromTheirs(current.theirsName, replaced = replaced)) }
        changed()
    }

    private fun CommunityListUiState.Success.listCards(): Set<Long> = list.tiers.flatMap { it.items }.mapTo(mutableSetOf()) { it.id }
}
