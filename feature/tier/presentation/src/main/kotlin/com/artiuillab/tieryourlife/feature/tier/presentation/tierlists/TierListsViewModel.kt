package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.SearchWords
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.PublishCarrier
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedSync
import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardOrder
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.repository.BoardOwner
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.publishedByAnotherAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    private val accounts: AccountRepository,
    private val boardSync: BoardSync,
    private val pictures: PictureRestore,
    private val owner: BoardOwner,
    private val drafts: RankingDrafts = RankingDrafts.None,
    private val games: SavedGames = NoSavedGames,
    private val startedSync: StartedSync = StartedSync.None,
    private val carrier: PublishCarrier? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val replacedHere = Channel<Unit>(Channel.CONFLATED)

    /** A list begun here gave way to a later arrangement of it from another device. */
    val updatedElsewhere: Flow<Unit> = replacedHere.receiveAsFlow()

    private var lastLoadedLists: List<TierListSummary> = emptyList()

    private var mode: HomeMode = HomeMode.Browsing
    private var boardSort: BoardSort = BoardSort.Newest
    private var boardFilters: BoardFilters = BoardFilters()

    private var account: Account = Account.Unknown

    /** Read once: consulted on every redraw. */
    private var conflictsSeen: Set<String> = emptySet()
    private var offerAnswered: Boolean = false
    private var syncJob: Job? = null
    private var syncAgain = false

    private var restoringPictures: PictureRestore.Progress = PictureRestore.Progress.Idle
    private var started: List<StartedList> = emptyList()

    /** The arrangement last discarded from Continue, as it was, for its Undo. */
    private var discarded: Discarded? = null
    private var discarding: Job? = null

    private class Discarded(val listId: String, val owner: String?, val draft: RankingDraft, val gameKey: String, val game: SavedGame?)

    init {
        offerAnswered = preferences.signInOfferAnswered()
        conflictsSeen = preferences.conflictsSeen()
        viewModelScope.launch {
            pictures.restoring.collect { progress ->
                restoringPictures = progress
                if (loaded) emitSuccess()
            }
        }
        viewModelScope.launch {
            accounts.account.collectLatest { current ->
                account = current
                // Redraws a screen that is already up. Emitting before the first
                // database read finished turned "loading" into "no boards".
                if (loaded) emitSuccess()
            }
        }
        // Another identity's boards are other boards: what is on screen is
        // read again, and the sync that follows the read is that identity's.
        viewModelScope.launch {
            owner.changes.collect { loadTierListsInternal() }
        }
        // A run from a returning connection, with no screen having asked for it.
        viewModelScope.launch {
            boardSync.finished.collect { report -> if (loaded && report.carried > 0) showWhatTheRunLeft() }
        }
        // A list on its way out says so on its tile.
        carrier?.let { carrying ->
            viewModelScope.launch {
                carrying.going.map { it.keys }.distinctUntilChanged().collect { if (loaded) emitSuccess() }
            }
        }
        // Lists begun on another device, or taken out of Continue there.
        viewModelScope.launch {
            startedSync.synced.collect { outcome ->
                if (!loaded) return@collect
                started = startedLists()
                emitSuccess()
                if (outcome.replaced > 0) replacedHere.trySend(Unit)
            }
        }
    }

    /**
     * Cancelled mid-run is fine: the next run recomputes from the same lists.
     * Asked for during a run, one more follows it: the run going may have been
     * planned before a sign-in settled, and a fresh phone then got nothing
     * until some later edit asked again.
     */
    private fun keepBoards() {
        if (!wantsBoardsKept()) return
        if (syncJob?.isActive == true) {
            syncAgain = true
            return
        }
        syncJob = viewModelScope.launch {
            do {
                syncAgain = false
                runSuspendCatching { boardSync.sync() }
                    .onFailure { failure -> Timber.w(failure, "Keeping boards did not finish") }
                showWhatTheRunLeft()
            } while (syncAgain && wantsBoardsKept())
        }
    }

    /** The run may have brought boards in or sent some to the trash: the screen shows the database again. */
    private suspend fun showWhatTheRunLeft() = loadMutex.withLock {
        val now = runSuspendCatching { repository.getTierListSummaries() }.getOrNull() ?: return@withLock
        if (now == lastLoadedLists) return@withLock
        lastLoadedLists = now
        emitSuccess()
    }

    // Asked afresh: a change of identity can reach this before the field above hears of it.
    private fun wantsBoardsKept(): Boolean {
        val signedIn = accounts.current() as? Account.SignedIn ?: return false
        return preferences.backUpBoards(signedIn.recordKey)
    }

    /** Answered once: a card that keeps returning is one people dismiss unread. */
    fun dismissConflictNotice(title: String) {
        preferences.markConflictSeen(title)
        conflictsSeen = conflictsSeen + title
        emitSuccess()
    }

    fun dismissSignInOffer() {
        offerAnswered = true
        preferences.markSignInOfferAnswered()
        emitSuccess()
    }

    fun loadTierLists() {
        viewModelScope.launch {
            loadTierListsInternal()
        }
    }

    private var lastArrival: Any? = null

    /**
     * Loads once per arrival: the screen hands over the same token while it
     * is only re-entering composition (a tab switch), and a new one when it
     * was arrived at or resumed. Loading on every re-entry also synced, and a
     * tab tap is not a reason to talk to the account.
     */
    fun onArrival(token: Any) {
        if (token === lastArrival) return
        lastArrival = token
        loadTierLists()
    }

    private suspend fun loadTierListsInternal() = loadMutex.withLock {
        val hasVisibleList = _state.value is TierListsUiState.Success

        if (!hasVisibleList) {
            _state.value = TierListsUiState.Loading
        }

        try {
            lastLoadedLists = repository.getTierListSummaries()
            started = startedLists()
            loaded = true
            emitSuccess().join()
            if (lastLoadedLists.isEmpty() && started.isEmpty()) findReadyList()
            keepBoards()
            startedSync.request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Loading tier lists failed")
            if (!hasVisibleList) {
                _state.value = TierListsUiState.Error
            }
        }
    }

    private var readyList: PublishedListSummary? = null
    private var readyListFound = false

    /**
     * The first list on the first page of the popular feed with at least eight cards, so the
     * Play tip has something to say there too. Asked for once found; a failure leaves no card
     * and is asked again on the next arrival.
     */
    private fun findReadyList() {
        if (readyListFound) return
        viewModelScope.launch {
            community.feed(sort = FeedSort.Popular).onSuccess { page ->
                readyListFound = true
                readyList = page.lists.firstOrNull { it.itemCount >= READY_LIST_FROM }
                emitSuccess()
            }.onFailure { Timber.i(it, "No ready list to offer") }
        }
    }

    /** Paired on screen, not in the database: the pair ends the moment either is deleted. */
    private fun withTwins(lists: List<TierListSummary>): List<TierListSummary> {
        val byTitle = lists.groupBy { it.title.removeSuffix(" ${it.arrivedFrom.orEmpty()}").trim() }
        return lists.map { list ->
            val twins = byTitle[list.title.removeSuffix(" ${list.arrivedFrom.orEmpty()}").trim()].orEmpty()
            list.copy(hasTwin = twins.size > 1 && twins.any { it.arrivedFrom != null })
        }
    }

    private var loaded = false
    private var arranging: Job? = null

    /**
     * Sorting and pairing every board runs off the main thread, and a newer
     * redraw cancels an older one, so a burst of keystrokes draws only the last.
     */
    private fun emitSuccess(): Job {
        val going = carrier?.going?.value?.keys.orEmpty()
        val lists = lastLoadedLists.map { if (it.id in going) it.copy(updating = true) else it }
        val mode = mode
        val sort = boardSort
        val filters = boardFilters
        val asPictures = preferences.boardsAsPictures()
        val localOnly = whereTheseLive()
        val restoring = restoringPictures
        val seen = conflictsSeen
        val begun = started
        val ready = readyList.takeIf { lists.isEmpty() && begun.isEmpty() }
        arranging?.cancel()
        return viewModelScope.launch {
            val shown = withContext(Dispatchers.Default) { arrange(lists, mode, sort, filters, seen) }
            _state.value = shown.copy(
                asPictures = asPictures,
                localOnly = localOnly,
                restoringPictures = restoring,
                started = begun,
                readyList = ready,
            )
        }.also { arranging = it }
    }

    private fun arrange(
        lists: List<TierListSummary>,
        mode: HomeMode,
        sort: BoardSort,
        filters: BoardFilters,
        conflictsSeen: Set<String>,
    ): TierListsUiState.Success {
        val query = (mode as? HomeMode.Searching)?.query
        val matching = if (query != null) lists.filter { SearchWords.holds(it.title, query) } else lists
        val arranged = BoardOrder.arrange(withTwins(matching), sort, filters)
        // A search or a filter makes the screen an answer to a question, and
        // nothing is pinned above an answer.
        val grouped = BoardOrder.shouldGroup(arranged, narrowed = query != null || filters.any)
        val paired = if (grouped) arranged.rest else BoardOrder.ungrouped(arranged, sort)
        return TierListsUiState.Success(
            lists = paired,
            conflict = paired.firstOrNull { it.hasTwin && it.arrivedFrom != null && it.title !in conflictsSeen },
            favourites = if (grouped) arranged.favourites else emptyList(),
            grouped = grouped,
            boardSort = sort,
            boardFilters = filters,
            filedUnder = lists.mapNotNull { it.category }.distinct().sorted(),
            totalListCount = lists.size,
            rankedCount = lists.sumOf { it.rankedCount },
            mode = mode,
        )
    }

    /**
     * Other people's lists begun on this phone by whoever is signed in now. A
     * list that will not read leaves the block out, never the boards.
     */
    private suspend fun startedLists(): List<StartedList> = runSuspendCatching {
        val who = (accounts.current() as? Account.SignedIn)?.recordKey
        val waiting = games.keys()
        drafts.started(who).take(MAX_STARTED).map { begun ->
            StartedList(
                listId = begun.listId,
                title = begun.title,
                placed = begun.placed,
                total = begun.total,
                gameSaved = PlayRequest.someoneElsesKey(begun.listId, begun.version) in waiting,
                section = begun.section,
            )
        }
    }.onFailure { Timber.w(it, "Reading the lists begun failed") }.getOrDefault(emptyList())

    /**
     * Takes a list out of Continue: its cards go back to the deck and its game is let go, as Return all
     * cards to the deck does on the list. The list itself, in Community, is not touched.
     */
    fun discardStarted(listId: String) {
        started = started.filterNot { it.listId == listId }
        emitSuccess()
        discarding = viewModelScope.launch {
            messages.guard("Discarding an arrangement") {
                val who = (accounts.current() as? Account.SignedIn)?.recordKey
                // Already in the deck: discarding it again would leave Undo nothing to bring back.
                val draft = drafts.read(listId, who)?.takeIf { it.placed > 0 } ?: return@guard
                val key = PlayRequest.someoneElsesKey(listId, draft.version)
                val game = games.load(key)
                drafts.keep(listId, draft.copy(rows = draft.rows.map { emptyList() }, pool = draft.rows.flatten() + draft.pool), who)
                if (game != null) games.discard(key)
                discarded = Discarded(listId, who, draft, key, game)
            }
            started = startedLists()
            emitSuccess()
        }
    }

    fun undoDiscardStarted() {
        viewModelScope.launch {
            discarding?.join()
            val last = discarded ?: return@launch
            discarded = null
            messages.guard("Bringing an arrangement back") {
                drafts.keep(last.listId, last.draft, last.owner)
                last.game?.let { games.save(last.gameKey, it) }
            }
            started = startedLists()
            emitSuccess()
        }
    }

    /** The sign-in offer waits for a first board: with nothing to lose there is nothing to offer. */
    private fun whereTheseLive(): LocalOnly = when {
        account is Account.Unknown -> LocalOnly.Unknown
        account is Account.SignedIn -> LocalOnly.Kept
        lastLoadedLists.isEmpty() -> LocalOnly.Unknown
        else -> LocalOnly.Here(offerSignIn = !offerAnswered)
    }

    fun selectBoardSort(sort: BoardSort) {
        if (boardSort == sort) return
        boardSort = sort
        emitSuccess()
    }

    fun applyBoardFilters(filters: BoardFilters) {
        if (boardFilters == filters) return
        boardFilters = filters
        emitSuccess()
    }

    /** The time is taken here so boards starred in one sitting keep their order. */
    fun toggleFavourite(id: Long) {
        val list = lastLoadedLists.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            repository.setFavouritedAt(id, if (list.favouritedAt == null) System.currentTimeMillis() else null)
            loadTierLists()
        }
    }

    /** Ticked lists all starred: unstarred together; otherwise the rest join them, in one sitting's time. */
    fun favouriteSelected(ids: Set<Long>) {
        val chosen = lastLoadedLists.filter { it.id in ids }
        if (chosen.isEmpty()) return
        val at = if (chosen.all { it.favouritedAt != null }) null else System.currentTimeMillis()
        setMode(HomeMode.Browsing)
        viewModelScope.launch {
            chosen.filter { (it.favouritedAt == null) == (at != null) }.forEach { repository.setFavouritedAt(it.id, at) }
            loadTierLists()
        }
    }

    fun picturesOnWifiOnly(): Boolean = preferences.picturesOnWifiOnly()

    fun toggleBoardsAsPictures() {
        preferences.setBoardsAsPictures(!preferences.boardsAsPictures())
        emitSuccess()
    }

    /** The search field reads its text from here, so the mode shows at once and the boards follow. */
    private fun setMode(newMode: HomeMode) {
        mode = newMode
        val shown = _state.value as? TierListsUiState.Success ?: return
        _state.value = shown.copy(mode = newMode)
        emitSuccess()
    }

    fun enterSearch() = setMode(HomeMode.Searching(""))

    fun updateSearchQuery(query: String) = setMode(HomeMode.Searching(query))

    fun exitSearch() = setMode(HomeMode.Browsing)

    fun enterSelection(id: Long) = setMode(HomeMode.Selecting(setOf(id)))

    fun toggleSelection(id: Long) {
        val current = mode as? HomeMode.Selecting ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        setMode(if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated))
    }

    fun exitSelection() = setMode(HomeMode.Browsing)

    /**
     * The community copy comes down first. If it cannot, the list stays: a
     * delete that leaves it public is worse than one that did not happen.
     */
    fun deleteTierLists(ids: List<Long>) {
        setMode(HomeMode.Browsing)
        viewModelScope.launch {
            val stillPublic = takeDownPublished(ids)
            val deletable = ids - stillPublic
            if (deletable.isNotEmpty()) {
                messages.guard("Deleting lists") { repository.deleteTierLists(deletable) }
            }
            if (stillPublic.isNotEmpty()) {
                messages.send(UserMessage.PublishedListStillPublic)
            }
            loadTierListsInternal()
        }
    }

    /** Answers with the ids that could not be taken out of the community. Another account's stay up, theirs to take down. */
    private suspend fun takeDownPublished(ids: List<Long>): Set<Long> {
        // A publish on its way is waited for, or never goes.
        ids.forEach { carrier?.stop(it) }
        val published = lastLoadedLists.filter { it.id in ids && it.isPublic }
        val somebodyElses = publishedByAnotherAccount(accounts, preferences)
        return published.mapNotNull { list ->
            val rankingOf = list.rankingOf
            val publishedId = list.publishedId
            val takenDown = when {
                rankingOf != null -> community.withdrawRanking(rankingOf)
                publishedId in somebodyElses -> return@mapNotNull null
                publishedId != null -> community.unpublish(publishedId)
                else -> return@mapNotNull null
            }
            takenDown.fold(
                onSuccess = {
                    repository.setPublished(list.id, null, null)
                    repository.setRankingOf(list.id, null)
                    null
                },
                onFailure = { error ->
                    Timber.w(error, "Could not take a deleted list out of the community")
                    list.id
                },
            )
        }.toSet()
    }

    fun restoreTierLists(ids: List<Long>) {
        mutate("Restoring lists") { repository.restoreTierLists(ids) }
    }

    /** A [draft] stays out of every list and the account until something is done on it. */
    fun createTierList(title: String, captions: List<String>, onCreated: (Long) -> Unit, draft: Boolean = false) {
        viewModelScope.launch {
            var createdId: Long? = null
            messages.guard("Creating a list") {
                createdId = if (draft) repository.createDraft(title, captions) else repository.createTierList(title, captions)
            }
            loadTierListsInternal()
            createdId?.let(onCreated)
        }
    }

    private fun mutate(operation: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            messages.guard(operation) { block() }
            loadTierListsInternal()
        }
    }
}

/** At most this many begun lists stand above one's own: the block is a way back, not a second library. */
private const val MAX_STARTED = 3

/** Enough cards that the Play tip has something to say on the ready list too (PARITY 4.9). */
private const val READY_LIST_FROM = 8

/** For tests that leave games out: none is ever put down. */
private object NoSavedGames : SavedGames {
    override suspend fun load(key: String): SavedGame? = null
    override suspend fun save(key: String, game: SavedGame) = false
    override suspend fun discard(key: String) = Unit
    override suspend fun keys(): Set<String> = emptySet()
    override suspend fun lastKind(): GameKind? = null
    override suspend fun rememberKind(kind: GameKind) = Unit
}
