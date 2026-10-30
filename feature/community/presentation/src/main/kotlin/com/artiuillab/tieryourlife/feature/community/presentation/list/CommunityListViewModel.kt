package com.artiuillab.tieryourlife.feature.community.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.communityListRoute
import com.artiuillab.tieryourlife.feature.tier.domain.lists.withItemMoved
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val POOL_TIER_ID = -1L

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val tiers: TierRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val route = savedStateHandle.communityListRoute()
    val publishedId = route.publishedId

    /** What the card showed, for the screen to draw while the list is on its way. */
    val preview: ListPreview? = route.preview()

    private val _state = MutableStateFlow<CommunityListUiState>(CommunityListUiState.Loading)
    val state: StateFlow<CommunityListUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    /** The readers' placings of this list: read on demand, chosen into the left half, this reader's own taken down. */
    internal val rankings = ListRankings(publishedId, community, _state, viewModelScope, messages)

    /** Told once a ranking was shown, or that it takes signing in. */
    val rankingNotices: Flow<RankingNotice> = rankings.notices

    private val told = Channel<ListNotice>(Channel.BUFFERED)
    val listNotices: Flow<ListNotice> = told.receiveAsFlow()

    /** On their own list: the board behind it, and the list's name and taking down. */
    internal val ownList = OwnList(
        publishedId = publishedId,
        community = community,
        tiers = tiers,
        state = _state,
        scope = viewModelScope,
        messages = messages,
        tell = told::send,
        reread = { open(quietly = true) },
    )

    /** Mine as it was before a move on Theirs replaced it, for Undo. */
    private var beforeTheirs: Pair<TierList, Boolean>? = null

    init {
        load()
        viewModelScope.launch {
            accounts.account.collect { account ->
                _state.update { current ->
                    if (current is CommunityListUiState.Success) current.copy(signedIn = account is Account.SignedIn) else current
                }
            }
        }
    }

    private fun loadFollowState(authorUid: String) {
        if (authorUid.isEmpty()) return
        viewModelScope.launch {
            val state = community.followState(authorUid)
                .onFailure { Timber.i(it, "Not showing whether this author is followed") }
                .getOrNull() ?: return@launch
            _state.update { current ->
                if (current is CommunityListUiState.Success) current.copy(follow = state) else current
            }
        }
    }

    /** Shown before the server answers, put back if it refuses. */
    fun toggleFollow() {
        val current = _state.value as? CommunityListUiState.Success ?: return
        val was = current.follow ?: return
        val now = was.copy(
            following = !was.following,
            followers = (was.followers + if (was.following) -1 else 1).coerceAtLeast(0),
        )
        _state.update { (it as CommunityListUiState.Success).copy(follow = now) }

        viewModelScope.launch {
            val result = if (now.following) {
                community.follow(current.authorUid)
            } else {
                community.unfollow(current.authorUid)
            }
            result.onFailure { error ->
                Timber.w(error, "Following an author failed")
                _state.update { state ->
                    if (state is CommunityListUiState.Success) state.copy(follow = was) else state
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch { open(quietly = false) }
    }

    /** Quietly: no placeholder in between, and a failure leaves what was there. */
    private suspend fun open(quietly: Boolean) {
        val before = _state.value as? CommunityListUiState.Success
        if (!quietly) _state.value = CommunityListUiState.Loading
        val opened = community.open(publishedId).fold(
            onSuccess = { answer ->
                rankings.opened(answer)
                ownList.opened(answer.summary)
                val own = answer.summary.mine
                CommunityListUiState.Success(
                    list = asTheAuthorLeftIt(answer),
                    mine = emptied(answer),
                    // What somebody came for: the list as its author ranked it. Their own is only ever that.
                    showing = if (answer.arrangement.isEmpty() && !own) Showing.Mine else Showing.Theirs,
                    knowsTheirs = answer.arrangement.isNotEmpty(),
                    authorName = answer.summary.authorName,
                    authorUid = answer.summary.authorUid,
                    authorPhotoUrl = answer.summary.authorPhotoUrl,
                    editorial = answer.summary.editorial,
                    byNobody = answer.summary.byNobody,
                    rankingCount = answer.summary.rankingCount,
                    takeCount = answer.summary.takeCount,
                    basedOn = answer.summary.basedOn,
                    section = answer.summary.category,
                    own = own,
                    anonymous = answer.summary.anonymous,
                    listId = publishedId,
                    signedIn = accounts.current() is Account.SignedIn,
                    ownBoardId = before?.ownBoardId.takeIf { quietly },
                    trashedBoardId = before?.trashedBoardId.takeIf { quietly },
                    ownBoardLookedUp = quietly && before?.ownBoardLookedUp == true,
                )
            },
            onFailure = { if (quietly && before != null) before else CommunityListUiState.Error },
        )
        _state.value = opened
        val loaded = opened as? CommunityListUiState.Success ?: return
        if (loaded.own) ownList.find() else if (!loaded.editorial) loadFollowState(loaded.authorUid)
    }

    /** Two boards kept side by side: your own work stays where you left it while you glance at the author's. */
    fun show(which: Showing) {
        _state.update { current ->
            if (current !is CommunityListUiState.Success) current else current.copy(showing = which)
        }
    }

    /** In memory only until a copy is asked for, so backing out costs nothing. */
    fun moveItem(itemId: Long, toTierId: Long, toPosition: Int) {
        val current = _state.value as? CommunityListUiState.Success ?: return
        if (current.own) return
        if (current.showing == Showing.Theirs) return startFromTheirs(current, itemId, toTierId, toPosition)
        _state.update { state ->
            if (state !is CommunityListUiState.Success) return@update state
            state.copy(mine = state.mine.withItemMoved(itemId, toTierId, toPosition), arranged = true)
        }
    }

    /**
     * A move while their arrangement is on screen: the reader's own version
     * begins as what they were looking at, a chosen reader's placing included,
     * with that one move made. The board stays as it was under the finger.
     */
    private fun startFromTheirs(current: CommunityListUiState.Success, itemId: Long, toTierId: Long, toPosition: Int) {
        beforeTheirs = current.mine to current.arranged
        _state.update { state ->
            if (state !is CommunityListUiState.Success) return@update state
            state.copy(
                mine = state.mine.copy(tiers = state.theirs.tiers).withItemMoved(itemId, toTierId, toPosition),
                showing = Showing.Mine,
                arranged = true,
            )
        }
        viewModelScope.launch { told.send(ListNotice.StartedFromTheirs(current.theirsName, replaced = current.arranged)) }
    }

    /** Mine as it was before, and their arrangement back on screen. */
    fun undoStartFromTheirs() {
        val (mine, arranged) = beforeTheirs ?: return
        beforeTheirs = null
        _state.update { state ->
            if (state !is CommunityListUiState.Success) state else state.copy(mine = mine, arranged = arranged, showing = Showing.Theirs)
        }
    }

    /** A game's result on someone else's list: it becomes the reader's own arrangement, on screen, still unsaved. */
    fun putInMine(board: TierList) {
        _state.update { current ->
            if (current !is CommunityListUiState.Success) current else current.copy(mine = board, showing = Showing.Mine, arranged = true)
        }
    }

    /** The copy's id lands in the state; the screen then offers to open it rather than leaving on its own. */
    fun saveToMyLists(which: Showing) {
        val current = _state.value as? CommunityListUiState.Success ?: return
        if (current.own || current.saving || current.savedId != null) return
        _state.update { (it as CommunityListUiState.Success).copy(saving = true) }

        viewModelScope.launch {
            var newId: Long? = null
            val saved = messages.guard("Saving a community list") {
                // The reader's choice, asked for at the moment of saving: keeping
                // the author's ranking is a reason to save, not a mistake to correct.
                val keeping = if (which == Showing.Theirs) current.theirs else current.mine
                newId = tiers.createFromTemplate(
                    title = keeping.title,
                    authorName = current.authorName,
                    tiers = keeping.tiers.filterNot { it.isPool },
                    items = keeping.tiers.filter { it.isPool }.flatMap { it.items },
                    takenFrom = publishedId,
                )
            }
            _state.update { current ->
                current as CommunityListUiState.Success
                current.copy(
                    saving = false,
                    savedId = newId.takeIf { saved },
                    takeCount = if (saved) current.takeCount + 1 else current.takeCount,
                )
            }
            if (saved) {
                // What the popular ordering counts. Its failure is not this
                // person's problem: they have their copy either way.
                community.noteTaken(publishedId)
                    .onFailure { Timber.i(it, "Not counting this list as taken") }
            }
        }
    }

    fun showMyRanking() = rankings.showMine()

    fun hide() {
        val current = _state.value as? CommunityListUiState.Success ?: return
        if (current.own) return
        preferences.hideList(publishedId, current.list.title)
    }

    fun hideAuthor(authorUid: String, name: String) {
        preferences.hideAuthor(authorUid, name)
    }

    /** Off this reader's screen at once; whether it comes down for everyone is a person's decision. */
    fun report(reason: ReportReason, note: String?) {
        if ((_state.value as? CommunityListUiState.Success)?.own == true) return
        hide()
        viewModelScope.launch {
            community.report(publishedId, reason, note).onFailure {
                Timber.w(it, "Could not file the report")
                // Shown again in the feed, so it can be reported again; hidden, it never could.
                preferences.unhideList(publishedId)
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    /** As the author left it. A card whose tier the snapshot does not know goes to the pool, beside the author's own unranked cards. */
    private fun asTheAuthorLeftIt(published: PublishedList): TierList {
        val where = published.arrangement
        val ranked = published.tiers.mapIndexed { index, tier ->
            tier.copy(items = published.items.filterIndexed { at, _ -> where.getOrNull(at) == index })
        }
        val unranked = published.items.filterIndexed { at, _ -> where.getOrNull(at) == null }
        return TierList(
            id = 0,
            title = published.summary.title,
            tiers = ranked + pool(unranked),
            authorName = published.summary.authorName,
        )
    }

    /** The same board with every card back in the pool, for ranking it yourself. */
    private fun emptied(published: PublishedList) = TierList(
        id = 0,
        title = published.summary.title,
        tiers = published.tiers.map { it.copy(items = emptyList()) } + pool(published.items),
        authorName = published.summary.authorName,
    )
}

/** The one tier a published list does not carry: where its unplaced cards wait. */
internal fun pool(items: List<TierItem>) = Tier(
    id = POOL_TIER_ID,
    label = "Unranked",
    colorLight = "#DAD7E0",
    colorDark = "#46464F",
    items = items,
    isPool = true,
)
