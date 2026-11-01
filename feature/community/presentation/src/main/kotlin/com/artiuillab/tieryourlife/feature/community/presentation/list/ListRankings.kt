package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val reread: suspend () -> Unit,
) {
    /** The list as the server handed it over, for placing its cards a reader's way. */
    private var opened: PublishedList? = null

    /** False while [opened] is the copy kept from an earlier visit, which the author may have changed since. */
    private var fromServer = false
    private var cursor: String? = null

    private val told = Channel<RankingNotice>(Channel.BUFFERED)
    val notices: Flow<RankingNotice> = told.receiveAsFlow()

    fun opened(answer: PublishedList, fromServer: Boolean, sameBoard: Boolean = false) {
        opened = answer
        this.fromServer = fromServer
        if (!sameBoard) cursor = null
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
                // The author's own on a template is an empty half: the reader's is all there is.
                showing = if (ranking == null && !current.knowsTheirs) Showing.Mine else Showing.Theirs,
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

    /**
     * Mine, shown to others as this reader's ranking, or the one shown before
     * changed. Only somebody else's list, only once Mine was arranged, and only
     * signed in: a guest is asked to sign in instead. Its rows name the cards
     * by where they sit in the list, so only a list the server handed over in
     * this visit will do: a kept copy is read again first, and one that changed
     * meanwhile sends nothing.
     */
    fun showMine() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (opened == null) return
        if (!current.canShowRanking || current.sendingRanking) return
        if (!current.signedIn) {
            told.trySend(RankingNotice.SignInFirst)
            return
        }
        state.update { now -> if (now is CommunityListUiState.Success) now.copy(sendingRanking = true) else now }
        scope.launch {
            val answer = freshAnswer()
            val mine = (state.value as? CommunityListUiState.Success)?.takeIf { it.canShowRanking }?.mine
            if (answer == null || mine == null) {
                stopSending()
                messages.send(UserMessage.ActionFailed)
                return@launch
            }
            community.showMyRanking(listId, arrangementOf(answer, mine), rowsOf(answer, mine), tierLabelsOf(answer, mine)).fold(
                onSuccess = { shown ->
                    state.update { now -> if (now is CommunityListUiState.Success) now.withMine(shown, answer) else now }
                    told.send(RankingNotice.Shown)
                },
                onFailure = { error ->
                    Timber.w(error, "Showing a ranking failed")
                    stopSending()
                    if ((error as? RankingRefused)?.reason == RankingRefusal.NotSignedIn) {
                        told.send(RankingNotice.SignInFirst)
                    } else {
                        messages.send(UserMessage.ActionFailed)
                    }
                },
            )
        }
    }

    /** The list as the server has it now: the kept copy is read again first, and null when that fails. */
    private suspend fun freshAnswer(): PublishedList? {
        if (!fromServer) reread()
        return opened?.takeIf { fromServer }
    }

    private fun stopSending() {
        state.update { now -> if (now is CommunityListUiState.Success) now.copy(sendingRanking = false) else now }
    }

    /** First among the readers', as the server lists it, and in the left half if an older one of theirs was there. */
    private fun CommunityListUiState.Success.withMine(shown: ShownRanking, answer: PublishedList): CommunityListUiState.Success {
        val ranking = shown.ranking
        val wasShown = chosenRanking?.mine == true || chosenRanking?.uid == ranking.uid
        return copy(
            sendingRanking = false,
            rankings = listOf(ranking) + rankings.filterNot { it.mine || it.uid == ranking.uid },
            rankingCount = rankingCount + if (shown.created) 1 else 0,
            chosenRanking = if (wasShown) ranking else chosenRanking,
            theirs = if (wasShown) placedBy(answer, ranking) else theirs,
        )
    }
}

/** What a reader is told after asking to show their ranking; a failure is the usual one. */
enum class RankingNotice { Shown, SignInFirst }
