package com.artiuillab.tieryourlife.feature.community.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.opensOn
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SEARCH_DELAY_MILLIS = 300L

/** How far a narrowed following page looks ahead on its own before saying there is nothing. */
private const val PAGES_FETCHED_FOR_A_FILTER = 4

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val _state = MutableStateFlow(CommunityFeedUiState())
    val state: StateFlow<CommunityFeedUiState> = _state.asStateFlow()

    private var feed: CommunityFeed = CommunityFeed.Loading
    private var category: ListCategory? = null
    private var source: FeedSource = FeedSource.Everyone

    /** Per source: switching source must not change the order the other was left in. */
    private val sortBySource = mutableMapOf(
        FeedSource.Everyone to FeedSource.Everyone.opensOn,
        FeedSource.Following to FeedSource.Following.opensOn,
    )
    private var query: String? = null
    private var searchJob: Job? = null
    private var yourTopics: List<Topic> = emptyList()
    private var topicsJob: Job? = null
    private var followingFilter: FollowingFilter = FollowingFilter.All

    /** Pages asked for by [fetchUntilSomethingShows] since the filter last changed. */
    private var pagesFetchedForFilter = 0

    /** One answer per load for the strip and the chips, whichever asks first. */
    private var followingAnswer: Deferred<Result<Following>>? = null

    /** What the feed on screen was already filtered against. */
    private var appliedHidden: Set<String> = emptySet()

    /** What to ask for to get the page after the one on screen. */
    private var cursor: String? = null
    private var moreJob: Job? = null

    private fun emit() {
        _state.value = CommunityFeedUiState(
            feed = feed.narrowedTo(followingFilter),
            category = category,
            source = source,
            sort = sortNow(),
            query = query,
            yourTopics = yourTopics,
            followingFilter = followingFilter,
        )
    }

    /** A following feed arrives as one page of people and topics together; the filter narrows what is shown, and [fetchUntilSomethingShows] asks on when that leaves nothing. */
    private fun CommunityFeed.narrowedTo(filter: FollowingFilter): CommunityFeed {
        if (this !is CommunityFeed.Ready || source != FeedSource.Following || filter == FollowingFilter.All) return this
        val people = followed.mapTo(mutableSetOf()) { it.uid }
        val topics = followedTopics.mapTo(mutableSetOf()) { it.id }
        return copy(
            lists = lists.filter {
                if (filter == FollowingFilter.People) it.authorUid in people else it.topicId in topics
            },
        )
    }

    private fun sortNow(): FeedSort = sortBySource.getValue(source)

    fun load() {
        searchJob?.cancel()
        followingAnswer = null
        refreshYourTopics()
        viewModelScope.launch { loadNow() }
    }

    /** The chips above the sections. Quiet: the feed stands without them, and a guest has none. */
    private fun refreshYourTopics() {
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            val mine = community.myTopics()
                .onFailure { Timber.i(it, "Reading this person's topics failed") }
                .getOrDefault(emptyList())
            val followed = followingNow()
                .onFailure { Timber.i(it, "Reading the followed topics failed") }
                .map { it.topics }
                .getOrDefault(emptyList())
            // A turned-down proposal has nothing to open; a waiting one still has its label.
            yourTopics = (mine + followed).distinctBy { it.id }.filterNot { it.status == TopicStatus.Rejected }
            emit()
        }
    }

    private suspend fun loadNow() {
        moreJob?.cancel()
        appliedHidden = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
        feed = community.feed(
            category = category,
            query = query,
            sort = sortNow(),
            following = source == FeedSource.Following,
        ).fold(
            onSuccess = { page ->
                cursor = page.nextCursor
                if (page.followingNobody) {
                    // The people to follow come with the answer, not a moment
                    // after it: shown in two steps, the screen blinked on every
                    // arrival, and coming back from a profile blinked it again.
                    val shown = feed as? CommunityFeed.FollowingNobody
                    val authors = community.suggestedAuthors()
                        .onFailure { Timber.w(it, "Reading who to follow failed") }
                        .getOrElse { shown?.authors.orEmpty() }
                    CommunityFeed.FollowingNobody(authors = authors, loading = false, followed = shown?.followed.orEmpty())
                } else {
                    val followed = if (source == FeedSource.Following) whoIsFollowed() else null
                    CommunityFeed.Ready(
                        lists = page.lists.filterNot(::isHidden),
                        canLoadMore = page.nextCursor != null,
                        followed = followed?.authors.orEmpty(),
                        followedTopics = followed?.topics.orEmpty(),
                    )
                }
            },
            onFailure = { error ->
                Timber.w(error, "Loading the community feed failed")
                cursor = null
                if (error is AppUnverified) CommunityFeed.Unverified else CommunityFeed.Failed
            },
        )
        emit()
        fetchUntilSomethingShows()
    }

    /** The strip above a following feed. Without it the feed still stands, so a failure here is a note, not a state. */
    private suspend fun whoIsFollowed(): Following? = followingNow()
        .onFailure { Timber.w(it, "Reading who is followed failed") }
        .getOrNull()

    private suspend fun followingNow(): Result<Following> {
        val answer = followingAnswer ?: viewModelScope.async { community.following() }.also { followingAnswer = it }
        return answer.await()
    }

    /**
     * The server hands a following feed over whole, so a page narrowed to
     * people or topics may hold nothing while the next does: ask on, a few
     * pages at most.
     */
    private fun fetchUntilSomethingShows() {
        val shown = feed as? CommunityFeed.Ready ?: return
        if (source != FeedSource.Following || followingFilter == FollowingFilter.All) return
        val narrowed = shown.narrowedTo(followingFilter) as? CommunityFeed.Ready ?: return
        if (narrowed.lists.isNotEmpty() || cursor == null || pagesFetchedForFilter >= PAGES_FETCHED_FOR_A_FILTER) return
        pagesFetchedForFilter++
        loadMore()
    }

    /** The feed again under new terms; the chips above it are not in question. */
    private fun reload() {
        followingAnswer = null
        feed = CommunityFeed.Loading
        emit()
        searchJob?.cancel()
        viewModelScope.launch { loadNow() }
    }

    fun selectCategory(selected: ListCategory?) {
        if (category == selected) return
        category = selected
        reload()
    }

    fun selectSource(selected: FeedSource) {
        if (source == selected) return
        source = selected
        reload()
    }

    /** Narrows what is already on screen: no request, nothing to wait for. */
    fun selectFollowingFilter(selected: FollowingFilter) {
        if (followingFilter == selected) return
        followingFilter = selected
        pagesFetchedForFilter = 0
        emit()
        fetchUntilSomethingShows()
    }

    fun selectSort(selected: FeedSort) {
        if (sortNow() == selected) return
        sortBySource[source] = selected
        reload()
    }

    fun enterSearch() {
        query = ""
        emit()
    }

    /** A request, not a filter over what is on screen: waits for the typing to settle. */
    fun updateQuery(typed: String) {
        query = typed
        searchJob?.cancel()
        feed = CommunityFeed.Loading
        emit()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MILLIS)
            loadNow()
        }
    }

    fun exitSearch() {
        if (query == null) return
        query = null
        reload()
    }

    /** The card stays put: a list that removes what was just tapped makes the next tap land elsewhere. */
    fun followSuggested(authorUid: String) {
        val shown = feed as? CommunityFeed.FollowingNobody ?: return
        feed = shown.copy(followed = shown.followed + authorUid)
        emit()
        viewModelScope.launch {
            community.follow(authorUid).onFailure { error ->
                Timber.w(error, "Following an author failed")
                val now = feed as? CommunityFeed.FollowingNobody ?: return@onFailure
                feed = now.copy(followed = now.followed - authorUid)
                emit()
            }
        }
    }

    fun loadMore() {
        val shown = feed as? CommunityFeed.Ready ?: return
        val after = cursor ?: return
        if (shown.loadingMore) return

        feed = shown.copy(loadingMore = true)
        emit()
        moreJob = viewModelScope.launch {
            community.feed(
                category = category,
                query = query,
                after = after,
                sort = sortNow(),
                following = source == FeedSource.Following,
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
        val shown = feed as? CommunityFeed.Ready ?: return
        cursor = page.nextCursor
        val alreadyShown = shown.lists.mapTo(mutableSetOf()) { it.id }
        feed = shown.copy(
            lists = shown.lists + page.lists.filterNot { it.id in alreadyShown || isHidden(it) },
            canLoadMore = page.nextCursor != null,
            loadingMore = false,
        )
        emit()
        fetchUntilSomethingShows()
    }

    private fun stopWaitingForMore() {
        val shown = feed as? CommunityFeed.Ready ?: return
        feed = shown.copy(loadingMore = false)
        emit()
    }

    /** Hiding is local and silent: the author is never told. */
    private fun isHidden(summary: PublishedListSummary): Boolean =
        summary.id in preferences.hiddenListIds() || summary.authorUid in preferences.hiddenAuthorUids()

    /** Newly hidden can be dropped from what is held; unhidden is not here to put back, so it refetches. */
    fun refreshHidden() {
        val hiddenNow = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
        if ((appliedHidden - hiddenNow).isNotEmpty()) {
            load()
            return
        }
        appliedHidden = hiddenNow
        dropFromFeed(::isHidden)
    }

    fun hideList(summary: PublishedListSummary) {
        preferences.hideList(summary.id, summary.title)
        noteHidden(summary.id, reported = false)
    }

    fun hideAuthor(authorUid: String, name: String) {
        preferences.hideAuthor(authorUid, name)
        dropFromFeed { it.authorUid == authorUid }
    }

    /** Hides it here at once; taking it down for everyone is a person's decision. */
    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) {
        preferences.hideList(summary.id, summary.title)
        noteHidden(summary.id, reported = true)
        viewModelScope.launch {
            community.report(summary.id, reason, note).onFailure {
                Timber.w(it, "Could not file the report")
                // Back on screen, so it can be reported again; hidden, it never could.
                preferences.unhideList(summary.id)
                val current = feed as? CommunityFeed.Ready
                if (current != null) {
                    feed = current.copy(justHidden = current.justHidden - summary.id)
                    emit()
                }
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    private fun noteHidden(publishedId: String, reported: Boolean) {
        val current = feed as? CommunityFeed.Ready ?: return
        feed = current.copy(justHidden = current.justHidden + (publishedId to reported))
        emit()
    }

    private fun dropFromFeed(matching: (PublishedListSummary) -> Boolean) {
        val current = feed as? CommunityFeed.Ready ?: return
        val kept = current.lists.filterNot(matching)
        if (kept.size == current.lists.size) return
        feed = current.copy(lists = kept)
        emit()
    }
}
