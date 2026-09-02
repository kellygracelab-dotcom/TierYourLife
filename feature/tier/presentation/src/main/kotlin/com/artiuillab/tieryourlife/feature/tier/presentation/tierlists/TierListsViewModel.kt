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
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardFilters
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardOrder
import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.opensOn
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
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
    private var tab: HomeTab = HomeTab.Mine

    /** What the feed on screen was already filtered against. */
    private var appliedHidden: Set<String> = emptySet()

    /** What to ask for to get the page after the one on screen. */
    private var communityCursor: String? = null
    private var moreJob: Job? = null
    private var communityFeed: CommunityFeed = CommunityFeed.Loading
    private var communityCategory: ListCategory? = null
    private var communitySource: FeedSource = FeedSource.Everyone

    /**
     * The order each source was last read in, remembered separately.
     *
     * They open on different orders and for a reason, so one shared setting
     * would make switching source silently change the order too. Somebody who
     * chose Newest among everybody expects it back when they come back.
     */
    private val communitySort = mutableMapOf(
        FeedSource.Everyone to FeedSource.Everyone.opensOn,
        FeedSource.Following to FeedSource.Following.opensOn,
    )
    private var communitySearch: Job? = null

    private var account: Account = Account.Unknown

    /**
     * Read once and kept, because it is consulted on every redraw and a
     * preferences file is not a thing to reach for on every frame.
     */
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
                // Redraws a screen that is already up; it does not put one
                // there. Firebase answers before the first read of the
                // database finishes, and emitting here turned "still loading"
                // into "you have no boards" for as long as that took.
                if (_state.value is TierListsUiState.Success) emitSuccess()
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

    /**
     * A board that arrived from another phone is only half the story; the
     * other half is the copy it landed beside. Marked as a pair here rather
     * than in the database, because it stops being a pair the moment somebody
     * deletes one -- a fact about what is on screen, not about the board.
     */
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
            tab = tab,
            asPictures = preferences.boardsAsPictures(),
            community = communityFeed,
            communityCategory = communityCategory,
            communitySource = communitySource,
            communitySort = sortNow(),
            localOnly = whereTheseLive(),
            restoringPictures = restoringPictures,
            conflict = paired.firstOrNull { it.hasTwin && it.arrivedFrom != null && !seenConflict(it) },
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

    /**
     * Rows or pictures, remembered. A property of the screen rather than of
     * the person, but somebody who chose pictures once meant it.
     */
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

    /**
     * The star, both ways. The time is taken here rather than in the database
     * so that several boards starred in one sitting still come out in the
     * order they were starred.
     */
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

    fun selectCommunitySource(source: FeedSource) {
        if (communitySource == source) return
        communitySource = source
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        loadCommunityFeed()
    }

    fun selectCommunitySort(sort: FeedSort) {
        if (sortNow() == sort) return
        communitySort[communitySource] = sort
        communityFeed = CommunityFeed.Loading
        emitSuccess()
        loadCommunityFeed()
    }

    private fun sortNow(): FeedSort = communitySort.getValue(communitySource)

    /**
     * Follows somebody from the screen that offered them.
     *
     * Their card stays where it is rather than disappearing: a list that
     * removes what you just touched makes the next tap land on somebody else.
     * The feed behind it is left alone until the screen is opened again.
     */
    fun followSuggested(authorUid: String) {
        val shown = communityFeed as? CommunityFeed.FollowingNobody ?: return
        communityFeed = shown.copy(followed = shown.followed + authorUid)
        emitSuccess()
        viewModelScope.launch {
            community.follow(authorUid).onFailure { error ->
                Timber.w(error, "Following an author failed")
                val now = communityFeed as? CommunityFeed.FollowingNobody ?: return@onFailure
                communityFeed = now.copy(followed = now.followed - authorUid)
                emitSuccess()
            }
        }
    }

    fun loadCommunityFeed() {
        communitySearch?.cancel()
        viewModelScope.launch { loadCommunityFeedNow((mode as? HomeMode.Searching)?.query) }
    }

    private suspend fun loadCommunityFeedNow(query: String?) {
        moreJob?.cancel()
        appliedHidden = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
        communityFeed = community.feed(
            category = communityCategory,
            query = query,
            sort = sortNow(),
            following = communitySource == FeedSource.Following,
        ).fold(
            onSuccess = { page ->
                communityCursor = page.nextCursor
                if (page.followingNobody) {
                    CommunityFeed.FollowingNobody()
                } else {
                    CommunityFeed.Ready(
                        lists = page.lists.filterNot(::isHidden),
                        canLoadMore = page.nextCursor != null,
                    )
                }
            },
            onFailure = { error ->
                Timber.w(error, "Loading the community feed failed")
                communityCursor = null
                CommunityFeed.Failed
            },
        )
        emitSuccess()
        // Asked for only once the state above is in place. Started any earlier
        // and a fast answer -- a cached one, or a failure -- arrives while the
        // screen is still Loading, finds nothing of its own to fill in, and
        // leaves the spinner up for good.
        if (communityFeed is CommunityFeed.FollowingNobody) {
            loadSuggestedAuthors()
        }
    }

    private fun loadSuggestedAuthors() {
        viewModelScope.launch {
            val authors = community.suggestedAuthors()
                .onFailure { Timber.w(it, "Reading who to follow failed") }
                .getOrDefault(emptyList())
            // Only if the screen is still the one that asked. Switching back to
            // everybody while this was in flight must not put it back.
            val shown = communityFeed as? CommunityFeed.FollowingNobody ?: return@launch
            communityFeed = shown.copy(authors = authors, loading = false)
            emitSuccess()
        }
    }

    fun loadMoreCommunity() {
        val shown = communityFeed as? CommunityFeed.Ready ?: return
        val cursor = communityCursor ?: return
        if (shown.loadingMore) return

        communityFeed = shown.copy(loadingMore = true)
        emitSuccess()
        moreJob = viewModelScope.launch {
            community.feed(
                category = communityCategory,
                query = (mode as? HomeMode.Searching)?.query,
                after = cursor,
                sort = sortNow(),
                following = communitySource == FeedSource.Following,
            )
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
