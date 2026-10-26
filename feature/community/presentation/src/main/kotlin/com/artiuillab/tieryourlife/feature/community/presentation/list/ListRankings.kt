package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The readers' placings of one list, kept beside the list's state and
 * changing it: which are known, which one the left half of the switch shows,
 * and this reader's own coming down.
 */
internal class ListRankings(
    private val listId: String,
    private val community: CommunityRepository,
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
) {
    /** The list as the server handed it over, for placing its cards a reader's way. */
    private var opened: PublishedList? = null
    private var cursor: String? = null

    fun opened(answer: PublishedList) {
        opened = answer
        cursor = null
    }

    /** The first page, asked for when the sheet opens; nothing is read for a list nobody has ranked. */
    fun load() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.rankingsLoading || current.rankings.isNotEmpty() || current.rankingCount == 0) return
        fetch(after = null)
    }

    fun loadMore() {
        val current = state.value as? CommunityListUiState.Success ?: return
        val after = cursor ?: return
        if (current.rankingsLoading) return
        fetch(after)
    }

    private fun fetch(after: String?) {
        state.update { current ->
            if (current is CommunityListUiState.Success) current.copy(rankingsLoading = true) else current
        }
        scope.launch {
            community.rankings(listId, after).fold(
                onSuccess = { page ->
                    cursor = page.nextCursor
                    state.update { current ->
                        if (current !is CommunityListUiState.Success) return@update current
                        val known = current.rankings.mapTo(mutableSetOf()) { it.uid }
                        current.copy(
                            rankings = current.rankings + page.rankings.filterNot { it.uid in known },
                            rankingsLoading = false,
                            moreRankings = page.nextCursor != null,
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Reading a list's rankings failed")
                    state.update { current ->
                        if (current is CommunityListUiState.Success) current.copy(rankingsLoading = false) else current
                    }
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    /** Whose placing the left half shows: a reader's, or the author's own again for null. */
    fun choose(ranking: Ranking?) {
        val answer = opened ?: return
        state.update { current ->
            if (current !is CommunityListUiState.Success) return@update current
            current.copy(
                chosenRanking = ranking,
                theirs = if (ranking == null) current.list else placedBy(answer, ranking),
                showing = Showing.Theirs,
            )
        }
    }

    /** This reader's own comes down at once, back if the server refuses; the author's placing fills the left half meanwhile. */
    fun withdrawMine() {
        val current = state.value as? CommunityListUiState.Success ?: return
        val mine = current.rankings.firstOrNull { it.mine } ?: return
        state.update { now ->
            if (now !is CommunityListUiState.Success) return@update now
            val wasShown = now.chosenRanking?.uid == mine.uid
            now.copy(
                rankings = now.rankings - mine,
                rankingCount = (now.rankingCount - 1).coerceAtLeast(0),
                chosenRanking = now.chosenRanking.takeUnless { wasShown },
                theirs = if (wasShown) now.list else now.theirs,
            )
        }
        scope.launch {
            community.withdrawRanking(listId).onFailure { error ->
                Timber.w(error, "Taking a ranking down failed")
                state.update { now ->
                    if (now !is CommunityListUiState.Success) return@update now
                    now.copy(rankings = listOf(mine) + now.rankings, rankingCount = now.rankingCount + 1)
                }
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    /** The author's cards where [ranking] put them, under the words it gave the tiers. */
    private fun placedBy(answer: PublishedList, ranking: Ranking): TierList {
        val where = ranking.arrangement
        val ranked = answer.tiers.mapIndexed { index, tier ->
            tier.copy(
                label = ranking.tierLabels.getOrNull(index) ?: tier.label,
                items = answer.items.filterIndexed { at, _ -> where.getOrNull(at) == index },
            )
        }
        val unranked = answer.items.filterIndexed { at, _ -> where.getOrNull(at) == null }
        return TierList(id = 0, title = answer.summary.title, tiers = ranked + pool(unranked), authorName = ranking.authorName)
    }
}
