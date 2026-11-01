package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.matchesSearch
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a search shows instead of the tabs: the server's answer to what was typed. */
data class SearchResults(
    val lists: List<PublishedListSummary> = emptyList(),
    /** Approved topics whose name starts with what was typed. */
    val topics: List<Topic> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
) {
    /** An answer, and an empty one: not a search still asked or one that failed. */
    val foundNothing: Boolean get() = !loading && !failed && lists.isEmpty() && topics.isEmpty()
}

/** Explore (§2): the sections in the server's order, and the popular feed under them. */
data class ExploreUiState(
    /** What the tiles draw: approved sections only. */
    val sections: List<Section> = Section.BUILT_IN,
    val feed: FeedPane = FeedPane(),
    /** Non-null while the search bar is up. */
    val query: String? = null,
    val results: SearchResults = SearchResults(),
    val nowMillis: Long = 0L,
)

private const val SEARCH_DELAY_MILLIS = 300L

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val community: CommunityRepository,
    cache: CommunityFeedCache,
    network: CommunityNetwork,
    preferences: AppPreferences,
    private val clock: CommunityClock,
    private val catalog: SectionCatalog,
) : ViewModel() {

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)

    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(CommunityNotice.ActionFailed) }

    internal val feed = FeedLoader(FeedKeys.EXPLORE, viewModelScope, cache, clock, menu::isVisible) { after ->
        community.feed(sort = FeedSort.Popular, after = after)
    }

    private val query = MutableStateFlow<String?>(null)
    private val results = MutableStateFlow(SearchResults())
    private var searchJob: Job? = null

    val state: StateFlow<ExploreUiState> = combine(catalog.directory, feed.pane, query, results) { directory, pane, q, found ->
        ExploreUiState(
            sections = directory.shown,
            feed = pane,
            query = q,
            results = found,
            nowMillis = clock.nowMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreUiState())

    val notices: Flow<CommunityNotice> = merge(
        notes.receiveAsFlow(),
        feed.notices.map { notice -> notice.toCommunityNotice() },
    )

    init {
        viewModelScope.launch {
            network.available.collect {
                feed.onNetworkBack()
                if (!catalog.directory.value.counted) loadSections()
            }
        }
    }

    /** Every arrival: the kept feed first, the server when that is old. */
    fun open() {
        feed.open()
        loadSections()
    }

    /** At most every ten minutes: the catalog keeps the rest. */
    private fun loadSections() {
        viewModelScope.launch { catalog.refresh() }
    }

    fun follow(sectionId: String) = setFollowing(sectionId, true)

    fun unfollow(sectionId: String) = setFollowing(sectionId, false)

    private fun setFollowing(sectionId: String, now: Boolean) {
        viewModelScope.launch {
            catalog.setFollowing(sectionId, now).fold(
                onSuccess = { notes.send(if (now) CommunityNotice.NowFollowing(sectionId) else CommunityNotice.Unfollowed(sectionId)) },
                onFailure = { notes.send(CommunityNotice.FollowFailed) },
            )
        }
    }

    fun enterSearch() {
        query.value = ""
        results.value = SearchResults()
    }

    /** A request, not a filter over what is on screen: it waits for the typing to settle. */
    fun updateQuery(typed: String) {
        query.value = typed
        searchJob?.cancel()
        if (typed.isBlank()) {
            results.value = SearchResults()
            return
        }
        results.value = results.value.copy(loading = true, failed = false)
        searchJob = viewModelScope.launch {
            delay(SEARCH_DELAY_MILLIS)
            val topics = async {
                community.topics(query = typed).getOrDefault(emptyList()).filter { it.status == TopicStatus.Approved }
            }
            val lists = community.feed(query = typed)
            val found = topics.await()
            results.value = lists.fold(
                onSuccess = { page -> SearchResults(lists = page.lists.filter(menu::isVisible), topics = found) },
                // Topics found are still an answer when the lists could not be read.
                onFailure = { if (found.isEmpty()) SearchResults(failed = true) else SearchResults(topics = found) },
            )
            if (results.value.foundNothing && catalog.directory.value.shown.none { it.matchesSearch(typed) }) {
                community.reportSearchMiss(typed)
            }
        }
    }

    fun retrySearch() {
        query.value?.let(::updateQuery)
    }

    fun exitSearch() {
        searchJob?.cancel()
        query.value = null
        results.value = SearchResults()
    }

    fun hideList(summary: PublishedListSummary) = menu.hideList(feed, summary)

    fun hideAuthor(authorUid: String, name: String) = menu.hideAuthor(listOf(feed), authorUid, name)

    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) = menu.report(feed, summary, reason, note)

    /** Come back from Settings, where hiding can be undone: what shows is filtered again. */
    fun refreshHidden() = feed.refilter()
}

internal fun FeedNotice.toCommunityNotice(): CommunityNotice = when (this) {
    is FeedNotice.RefreshFailed -> CommunityNotice.RefreshFailed(showingFromMillis)
}
