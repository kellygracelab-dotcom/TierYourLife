package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardOrder
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private var lastLoadedLists: List<TierList> = emptyList()

    private var mode: HomeMode = HomeMode.Browsing
    private var boardSort: BoardSort = BoardSort.Newest
    private var boardFilters: BoardFilters = BoardFilters()

    private var account: Account = Account.Unknown

    /** Read once: consulted on every redraw. */
    private var conflictsSeen: Set<String> = emptySet()
    private var offerAnswered: Boolean = false
    private var syncJob: Job? = null

    private var restoringPictures: PictureRestore.Progress = PictureRestore.Progress.Idle

    init {
        offerAnswered = preferences.signInOfferAnswered()
        conflictsSeen = preferences.conflictsSeen()
        viewModelScope.launch {
            pictures.restoring.collect { progress ->
                restoringPictures = progress
                if (_state.value is TierListsUiState.Success) emitSuccess()
            }
        }
        viewModelScope.launch {
            accounts.account.collectLatest { current ->
                account = current
                // Redraws a screen that is already up. Emitting before the first
                // database read finished turned "loading" into "no boards".
                if (_state.value is TierListsUiState.Success) emitSuccess()
                if (current is Account.SignedIn) keepBoards()
            }
        }
    }

    /** Cancelled mid-run is fine: the next run recomputes from the same lists. */
    private fun keepBoards() {
        if (account !is Account.SignedIn || !preferences.backUpBoards()) return
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            runCatching { boardSync.sync() }
                .onFailure { failure -> Timber.w(failure, "Keeping boards did not finish") }
        }
    }

    /** Answered once: a card that keeps returning is one people dismiss unread. */
    private fun seenConflict(list: TierList): Boolean = conflictsSeen.contains(list.title)

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

    private suspend fun loadTierListsInternal() = loadMutex.withLock {
        val hasVisibleList = _state.value is TierListsUiState.Success

        if (!hasVisibleList) {
            _state.value = TierListsUiState.Loading
        }

        try {
            lastLoadedLists = repository.getAllTierLists()
            emitSuccess()
            keepBoards()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Loading tier lists failed")
            if (!hasVisibleList) {
                _state.value = TierListsUiState.Error
            }
        }
    }

    /** Paired on screen, not in the database: the pair ends the moment either is deleted. */
    private fun withTwins(lists: List<TierList>): List<TierList> {
        val byTitle = lists.groupBy { it.title.removeSuffix(" ${it.arrivedFrom.orEmpty()}").trim() }
        return lists.map { list ->
            val twins = byTitle[list.title.removeSuffix(" ${list.arrivedFrom.orEmpty()}").trim()].orEmpty()
            list.copy(hasTwin = twins.size > 1 && twins.any { it.arrivedFrom != null })
        }
    }

    private fun emitSuccess() {
        val query = (mode as? HomeMode.Searching)?.query
        val matching = if (query != null) {
            lastLoadedLists.filter { it.title.contains(query, ignoreCase = true) }
        } else {
            lastLoadedLists
        }
        val arranged = BoardOrder.arrange(withTwins(matching), boardSort, boardFilters)
        // A search or a filter makes the screen an answer to a question, and
        // nothing is pinned above an answer.
        val grouped = BoardOrder.shouldGroup(arranged, narrowed = query != null || boardFilters.any)
        val paired = if (grouped) arranged.rest else arranged.all
        val rankedCount = lastLoadedLists.sumOf { list ->
            list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
        }
        _state.value = TierListsUiState.Success(
            lists = paired,
            favourites = if (grouped) arranged.favourites else emptyList(),
            grouped = grouped,
            boardSort = boardSort,
            boardFilters = boardFilters,
            totalListCount = lastLoadedLists.size,
            rankedCount = rankedCount,
            mode = mode,
            asPictures = preferences.boardsAsPictures(),
            localOnly = whereTheseLive(),
            restoringPictures = restoringPictures,
            conflict = paired.firstOrNull { it.hasTwin && it.arrivedFrom != null && !seenConflict(it) },
        )
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

    fun toggleBoardsAsPictures() {
        preferences.setBoardsAsPictures(!preferences.boardsAsPictures())
        emitSuccess()
    }

    private fun setMode(newMode: HomeMode) {
        mode = newMode
        if (_state.value is TierListsUiState.Success) {
            emitSuccess()
        }
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

    /** Answers with the ids that could not be taken out of the community. */
    private suspend fun takeDownPublished(ids: List<Long>): Set<Long> {
        val published = lastLoadedLists.filter { it.id in ids && it.publishedId != null }
        return published.mapNotNull { list ->
            val publishedId = list.publishedId ?: return@mapNotNull null
            community.unpublish(publishedId).fold(
                onSuccess = {
                    repository.setPublished(list.id, null, null)
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

    fun createTierList(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            var createdId: Long? = null
            messages.guard("Creating a list") { createdId = repository.createTierList(title) }
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
