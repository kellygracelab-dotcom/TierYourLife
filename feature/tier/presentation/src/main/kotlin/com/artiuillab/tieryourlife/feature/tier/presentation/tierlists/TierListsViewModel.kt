package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

private const val COMMUNITY_SEARCH_DELAY_MILLIS = 300L

@HiltViewModel
class TierListsViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    private val accounts: AccountRepository,
    private val boardSync: BoardSync,
) : ViewModel() {

    private val _state = MutableStateFlow<TierListsUiState>(TierListsUiState.Loading)
    val state: StateFlow<TierListsUiState> = _state.asStateFlow()
    private val loadMutex = Mutex()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private var lastLoadedLists: List<TierList> = emptyList()

    private var mode: HomeMode = HomeMode.Browsing
    private var tab: HomeTab = HomeTab.Mine

    /** What the feed on screen was already filtered against. */
    private var appliedHidden: Set<String> = emptySet()

    /** What to ask for to get the page after the one on screen. */
    private var communityCursor: String? = null
    private var moreJob: Job? = null
    private var communityFeed: CommunityFeed = CommunityFeed.Loading
    private var communityCategory: ListCategory? = null
    private var communitySearch: Job? = null

    private var account: Account = Account.Unknown
    private var offerAnswered: Boolean = false
    private var syncJob: Job? = null

    init {
        offerAnswered = preferences.signInOfferAnswered()
        viewModelScope.launch {
            accounts.account.collectLatest { current ->
                account = current
                emitSuccess()
                if (current is Account.SignedIn) keepBoards()
            }
        }
    }

    /**
     * Runs on the way back to the list, which is where somebody arrives after
     * changing a board. Cancelled if they leave again mid-run, and that is
     * fine: the next run works out the same answer from the same three lists,
     * so nothing is lost by stopping halfway.
     */
    private fun keepBoards() {
        if (account !is Account.SignedIn || !preferences.backUpBoards()) return
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            runCatching { boardSync.sync() }
                .onFailure { failure -> Timber.w(failure, "Keeping boards did not finish") }
        }
    }

    /**
     * "Not now" is answered once and for all. A card that returns is a card
     * people learn to dismiss without reading, and the footer line goes on
     * saying the same thing for as long as it is true.
     */
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

    private fun emitSuccess() {
        val query = (mode as? HomeMode.Searching)?.query
        val filtered = if (query != null) {
            lastLoadedLists.filter { it.title.contains(query, ignoreCase = true) }
        } else {
            lastLoadedLists
        }
        val rankedCount = lastLoadedLists.sumOf { list ->
            list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
        }
        _state.value = TierListsUiState.Success(
            lists = filtered,
            totalListCount = lastLoadedLists.size,
            rankedCount = rankedCount,
            mode = mode,
            tab = tab,
            community = communityFeed,
            communityCategory = communityCategory,
            localOnly = whereTheseLive(),
        )
    }

    /**
     * The offer waits for a board to exist. Somebody who has not made one has
     * nothing to lose yet, and asking them to sign in for the sake of an empty
     * screen is the advertisement this is trying not to be.
     */
    private fun whereTheseLive(): LocalOnly = when {
        account is Account.Unknown -> LocalOnly.Unknown
        account is Account.SignedIn -> LocalOnly.Kept
        lastLoadedLists.isEmpty() -> LocalOnly.Unknown
        else -> LocalOnly.Here(offerSignIn = !offerAnswered)
    }

    fun selectTab(selected: HomeTab) {
        if (tab == selected) return
        tab = selected
        emitSuccess()
        if (selected == HomeTab.Community) loadCommunityFeed()
    }

    /**
     * Searching the community is a request, not a filter over what is already
     * on screen, so it waits for the typing to settle first.
     */
    private fun searchCommunity(query: String) {
        communitySearch?.cancel()
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        communitySearch = viewModelScope.launch {
            delay(COMMUNITY_SEARCH_DELAY_MILLIS)
            loadCommunityFeedNow(query)
        }
    }

    fun selectCommunityCategory(category: ListCategory?) {
        if (communityCategory == category) return
        communityCategory = category
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        loadCommunityFeed()
    }

    fun loadCommunityFeed() {
        communitySearch?.cancel()
        viewModelScope.launch { loadCommunityFeedNow((mode as? HomeMode.Searching)?.query) }
    }

    private suspend fun loadCommunityFeedNow(query: String?) {
        moreJob?.cancel()
        appliedHidden = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
        communityFeed = community.feed(communityCategory, query).fold(
            onSuccess = { page ->
                communityCursor = page.nextCursor
                CommunityFeed.Ready(
                    lists = page.lists.filterNot(::isHidden),
                    canLoadMore = page.nextCursor != null,
                )
            },
            onFailure = { error ->
                Timber.w(error, "Loading the community feed failed")
                communityCursor = null
                CommunityFeed.Failed
            },
        )
        emitSuccess()
    }

    fun loadMoreCommunity() {
        val shown = communityFeed as? CommunityFeed.Ready ?: return
        val cursor = communityCursor ?: return
        if (shown.loadingMore) return

        communityFeed = shown.copy(loadingMore = true)
        emitSuccess()
        moreJob = viewModelScope.launch {
            community.feed(communityCategory, (mode as? HomeMode.Searching)?.query, after = cursor)
                .onSuccess { page -> appendPage(page) }
                // A page that never arrived is no reason to take away the
                // ones that did. The next scroll asks again.
                .onFailure { error ->
                    Timber.w(error, "Loading another page of the community feed failed")
                    stopWaitingForMore()
                }
        }
    }

    private fun appendPage(page: CommunityPage) {
        val shown = communityFeed as? CommunityFeed.Ready ?: return
        communityCursor = page.nextCursor
        val alreadyShown = shown.lists.mapTo(mutableSetOf()) { it.id }
        communityFeed = shown.copy(
            lists = shown.lists + page.lists.filterNot { it.id in alreadyShown || isHidden(it) },
            canLoadMore = page.nextCursor != null,
            loadingMore = false,
        )
        emitSuccess()
    }

    private fun stopWaitingForMore() {
        val shown = communityFeed as? CommunityFeed.Ready ?: return
        communityFeed = shown.copy(loadingMore = false)
        emitSuccess()
    }

    /** Hiding is local and silent: the author is never told. */
    private fun isHidden(summary: PublishedListSummary): Boolean =
        summary.id in preferences.hiddenListIds() || summary.authorUid in preferences.hiddenAuthorUids()

    /**
     * Hiding and unhiding both happen on other screens -- inside a list, on an
     * author's profile, in Settings -- while the feed here is already loaded.
     * Something newly hidden can be dropped from what we hold; something
     * unhidden is not in it to put back, so that costs a fetch.
     */
    fun refreshHidden() {
        val hiddenNow = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
        if ((appliedHidden - hiddenNow).isNotEmpty()) {
            loadCommunityFeed()
            return
        }
        appliedHidden = hiddenNow
        dropFromFeed(::isHidden)
    }

    fun hideCommunityList(summary: PublishedListSummary) {
        preferences.hideList(summary.id, summary.title)
        noteHidden(summary.id, reported = false)
    }

    fun hideCommunityAuthor(authorUid: String, name: String) {
        preferences.hideAuthor(authorUid, name)
        dropFromFeed { it.authorUid == authorUid }
    }

    /**
     * Reporting hides the list here at once. Taking it down for everyone is a
     * person's decision, and the screen says so rather than pretending.
     */
    fun reportCommunityList(summary: PublishedListSummary, reason: ReportReason, note: String?) {
        preferences.hideList(summary.id, summary.title)
        noteHidden(summary.id, reported = true)
        viewModelScope.launch {
            community.report(summary.id, reason, note)
                .onFailure { Timber.w(it, "Could not file the report") }
        }
    }

    private fun noteHidden(publishedId: String, reported: Boolean) {
        val current = communityFeed as? CommunityFeed.Ready ?: return
        communityFeed = current.copy(justHidden = current.justHidden + (publishedId to reported))
        emitSuccess()
    }

    private fun dropFromFeed(matching: (PublishedListSummary) -> Boolean) {
        val current = communityFeed as? CommunityFeed.Ready ?: return
        val kept = current.lists.filterNot(matching)
        if (kept.size == current.lists.size) return
        communityFeed = current.copy(lists = kept)
        emitSuccess()
    }

    private fun setMode(newMode: HomeMode) {
        mode = newMode
        if (_state.value is TierListsUiState.Success) {
            emitSuccess()
        }
    }

    fun enterSearch() = setMode(HomeMode.Searching(""))

    fun updateSearchQuery(query: String) {
        setMode(HomeMode.Searching(query))
        if (tab == HomeTab.Community) searchCommunity(query)
    }

    fun exitSearch() = setMode(HomeMode.Browsing)

    fun enterSelection(id: Long) = setMode(HomeMode.Selecting(setOf(id)))

    fun toggleSelection(id: Long) {
        val current = mode as? HomeMode.Selecting ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
        setMode(if (updated.isEmpty()) HomeMode.Browsing else HomeMode.Selecting(updated))
    }

    fun exitSelection() = setMode(HomeMode.Browsing)

    /**
     * A published snapshot that outlives the list it came from is one its owner
     * believes they deleted, so the community copy comes down first. If it
     * cannot, the list stays where it is: a delete that leaves the thing public
     * is worse than a delete that did not happen.
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
                    repository.setPublishedId(list.id, null)
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
