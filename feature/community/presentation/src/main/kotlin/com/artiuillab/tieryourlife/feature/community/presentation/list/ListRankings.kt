package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.LikeRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.LikeRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.PageGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.list.components.RankingPublisher
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val resetMine: () -> Unit = {},
    private val visibilityChanged: suspend (Boolean) -> Unit = {},
    private val accounts: AccountRepository,
    private val mineReady: () -> Boolean = { true },
    private val savedBoard: suspend () -> TierList? = { null },
) {
    fun returnAllToPool() = resetMine()

    /** The list as the server handed it over, for placing its cards a reader's way. */
    private var opened: PublishedList? = null

    /** False while [opened] is the copy kept from an earlier visit, which the author may have changed since. */
    private var fromServer = false
    private var cursor: String? = null

    /** Whether the first page was read since the list opened; this reader's own may be all that is known before it. */
    private var firstPageRead = false

    /** Likes go to the server one at a time: a second tap waits for the first answer. */
    private val liking = Mutex()

    private val told = Channel<RankingNotice>(Channel.BUFFERED)
    val notices: Flow<RankingNotice> = told.receiveAsFlow()

    private val publisher = RankingPublisher(
        listId, community, accounts, state, scope, messages,
        reread = {
            fromServer = false
            reread()
            opened?.takeIf { fromServer }
        },
        ready = mineReady, visibilityChanged = visibilityChanged,
        shown = { shown, answer -> state.update { if (it is CommunityListUiState.Success) it.withMine(shown, answer) else it } },
        tell = told::send,
        savedBoard = savedBoard,
    )

    fun opened(answer: PublishedList, fromServer: Boolean, sameBoard: Boolean = false) {
        opened = answer
        this.fromServer = fromServer
        if (!sameBoard) {
            cursor = null
            firstPageRead = false
        }
    }

    /** The first page, asked for when the sheet opens; nothing is read for a list nobody has ranked. */
    fun load() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.rankingsLoading || firstPageRead || current.rankingCount == 0) return
        fetch(after = null)
    }

    /**
     * The first page as the page opens, for a signed-in reader with Mine
     * arranged: whether their own is among them decides between Show and
     * Update in the menu (QA list-flow Н6, as on the website). Nothing is said
     * when it cannot be read; the sheet asks again.
     */
    fun peek() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.mayHoldMine && !firstPageRead) fetch(after = null, quietly = true)
    }

    private val CommunityListUiState.Success.mayHoldMine: Boolean
        get() = signedIn && arranged && !own && !arriving && !rankingsLoading && rankingCount > 0

    /** A thumb up on somebody else's placing, or taken back; the server says which it is now (SPEC list-flow К2). */
    fun like(ranking: Ranking) {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (!ranking.likeable) return
        if (!current.signedIn) {
            scope.launch { told.send(RankingNotice.SignInToLike) }
            return
        }
        val wanted = !ranking.likedByMe
        setLiked(ranking.uid, wanted)
        scope.launch {
            liking.withLock { send(ranking, wanted) }
        }
    }

    private suspend fun send(ranking: Ranking, wanted: Boolean) {
        community.likeRanking(listId, ranking.uid, wanted).fold(
            onSuccess = { liked -> setLiked(ranking.uid, liked) },
            onFailure = { error ->
                setLiked(ranking.uid, ranking.likedByMe)
                when ((error as? LikeRefused)?.reason) {
                    LikeRefusal.NotSignedIn -> told.send(RankingNotice.SignInToLike)
                    LikeRefusal.TooMany -> told.send(RankingNotice.TooManyLikes)
                    else -> messages.send(UserMessage.ActionFailed)
                }
            },
        )
    }

    private fun setLiked(uid: String, liked: Boolean) {
        fun Ranking.liked() = if (this.uid == uid) copy(likedByMe = liked) else this
        state.update { current ->
            if (current !is CommunityListUiState.Success) return@update current
            current.copy(rankings = current.rankings.map { it.liked() }, chosenRanking = current.chosenRanking?.liked())
        }
    }

    fun loadMore() {
        val current = state.value as? CommunityListUiState.Success ?: return
        val after = cursor ?: return
        if (current.rankingsLoading) return
        fetch(after)
    }

    private fun fetch(after: String?, quietly: Boolean = false) {
        state.update { current ->
            if (current is CommunityListUiState.Success) current.copy(rankingsLoading = true) else current
        }
        scope.launch {
            community.rankings(listId, after).fold(
                onSuccess = { page ->
                    cursor = page.nextCursor
                    if (after == null) firstPageRead = true
                    state.update { current ->
                        if (current !is CommunityListUiState.Success) return@update current
                        // The first page is the server's order from the top: what was known before it goes.
                        val before = if (after == null) emptyList() else current.rankings
                        val known = before.mapTo(mutableSetOf()) { it.uid }
                        current.copy(
                            rankings = before + page.rankings.filterNot { it.uid in known },
                            rankingsLoading = false,
                            moreRankings = page.nextCursor != null,
                            rankingShown = current.rankingShown || page.rankings.any { it.mine },
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Reading a list's rankings failed")
                    if (error is PageGone && after != null) {
                        // A ranking on the last page went: the first page again, in place of what was there.
                        cursor = null
                        state.update { current ->
                            if (current is CommunityListUiState.Success) current.copy(rankings = emptyList()) else current
                        }
                        fetch(after = null)
                        return@fold
                    }
                    state.update { current ->
                        if (current is CommunityListUiState.Success) current.copy(rankingsLoading = false) else current
                    }
                    if (!quietly) messages.send(UserMessage.ActionFailed)
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
        // From the menu the rankings may not have been read: what this phone knows is enough to take it down.
        val mine = current.rankings.firstOrNull { it.mine }
        if (mine == null && !current.rankingShown) return
        state.update { now ->
            if (now !is CommunityListUiState.Success) return@update now
            val wasShown = mine != null && (now.chosenRanking?.uid == mine.uid || now.chosenRanking?.mine == true)
            now.copy(
                rankings = now.rankings.filterNot { it.mine },
                rankingCount = (now.rankingCount - 1).coerceAtLeast(0),
                chosenRanking = now.chosenRanking.takeUnless { wasShown },
                theirs = if (wasShown) now.list else now.theirs,
                rankingShown = false,
            )
        }
        scope.launch {
            community.withdrawRanking(listId).onSuccess { visibilityChanged(false) }.onFailure { error ->
                Timber.w(error, "Taking a ranking down failed")
                state.update { now ->
                    if (now !is CommunityListUiState.Success) return@update now
                    now.copy(rankings = listOfNotNull(mine) + now.rankings, rankingCount = now.rankingCount + 1, rankingShown = true)
                }
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    fun showMine() = publisher.show()

    fun accountChanged(account: Account) = publisher.accountChanged(account)

    /**
     * In its own place among the readers', as the server orders them; a new
     * one is placed by the server on the next read. In the left half if an
     * older one of theirs was there.
     */
    private fun CommunityListUiState.Success.withMine(shown: ShownRanking, answer: PublishedList): CommunityListUiState.Success {
        val ranking = shown.ranking
        val wasShown = chosenRanking?.mine == true || chosenRanking?.uid == ranking.uid
        val at = rankings.indexOfFirst { it.mine || it.uid == ranking.uid }
        val placed = when {
            at >= 0 -> rankings.mapIndexedNotNull { index, one -> if (index == at) ranking else one.takeUnless { one.mine || one.uid == ranking.uid } }
            else -> rankings + ranking
        }
        if (at < 0) firstPageRead = false
        return copy(
            sendingRanking = false,
            rankingShown = true,
            rankings = placed,
            rankingCount = rankingCount + if (shown.created) 1 else 0,
            chosenRanking = if (wasShown) ranking else chosenRanking,
            theirs = if (wasShown) placedBy(answer, ranking) else theirs,
        )
    }
}

/** What a reader is told after asking to show their ranking; a failure is the usual one. */
enum class RankingNotice {
    Shown,
    SignInFirst,
    ListChanged,
    Banned,
    OwnList,
    ListGone,
    BadTierNames,
    CardsDontMatch,
    NotAccepted,
    TooManyWebPictures,
    WebPicturesToday,
    SignInToLike,
    TooManyLikes,
}
