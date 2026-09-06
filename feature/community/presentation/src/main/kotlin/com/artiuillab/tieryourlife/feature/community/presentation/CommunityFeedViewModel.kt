package com.artiuillab.tieryourlife.feature.community.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSource
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.opensOn
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SEARCH_DELAY_MILLIS = 300L

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

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

    /** What the feed on screen was already filtered against. */
    private var appliedHidden: Set<String> = emptySet()

    /** What to ask for to get the page after the one on screen. */
    private var cursor: String? = null
    private var moreJob: Job? = null

    private fun emit() {
        _state.value = CommunityFeedUiState(
            feed = feed,
            category = category,
            source = source,
            sort = sortNow(),
            query = query,
        )
    }

    private fun sortNow(): FeedSort = sortBySource.getValue(source)

    fun load() {
        searchJob?.cancel()
        viewModelScope.launch { loadNow() }
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
                cursor = null
                if (error is AppUnverified) CommunityFeed.Unverified else CommunityFeed.Failed
            },
        )
        emit()
        // Only once the state above is set: an early answer arrives while
        // still Loading and leaves the spinner up for good.
        if (feed is CommunityFeed.FollowingNobody) {
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
            val shown = feed as? CommunityFeed.FollowingNobody ?: return@launch
            feed = shown.copy(authors = authors, loading = false)
            emit()
        }
    }

    private fun reload() {
        feed = CommunityFeed.Loading
        emit()
        load()
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
            community.report(summary.id, reason, note)
                .onFailure { Timber.w(it, "Could not file the report") }
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
