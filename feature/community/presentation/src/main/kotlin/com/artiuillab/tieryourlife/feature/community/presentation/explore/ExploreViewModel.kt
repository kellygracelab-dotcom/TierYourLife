package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a search shows instead of the tabs: the server's answer to what was typed. */
data class SearchResults(
    val lists: List<PublishedListSummary> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

/** Explore (§2): the nine sections, how many lists each has new, and the popular feed under them. */
data class ExploreUiState(
    val sections: List<SectionStats> = ListCategory.entries.map { SectionStats(it) },
    /** False until the server has counted: the "new this week" lines stay hidden rather than say zero. */
    val countsKnown: Boolean = false,
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
) : ViewModel() {

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)

    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(CommunityNotice.ActionFailed) }

    internal val feed = FeedLoader(FeedKeys.EXPLORE, viewModelScope, cache, clock, menu::isVisible) { after ->
        community.feed(sort = FeedSort.Popular, after = after)
    }

    private val sections = MutableStateFlow(ListCategory.entries.map { SectionStats(it) })
    private val countsKnown = MutableStateFlow(false)
    private val query = MutableStateFlow<String?>(null)
    private val results = MutableStateFlow(SearchResults())
    private var searchJob: Job? = null

    val state: StateFlow<ExploreUiState> = combine(sections, countsKnown, feed.pane, query, results) { s, known, pane, q, found ->
        ExploreUiState(sections = s, countsKnown = known, feed = pane, query = q, results = found, nowMillis = clock.nowMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreUiState())

    val notices: Flow<CommunityNotice> = merge(
        notes.receiveAsFlow(),
        feed.notices.map { notice -> notice.toCommunityNotice() },
    )

    init {
        viewModelScope.launch {
            network.available.collect {
                feed.onNetworkBack()
                if (!countsKnown.value) loadSections()
            }
        }
    }

    /** Every arrival: the kept feed first, the server when that is old. */
    fun open() {
        feed.open()
        loadSections()
    }

    private fun loadSections() {
        viewModelScope.launch {
            community.sections().onSuccess { counted ->
                sections.value = counted
                countsKnown.value = true
            }
        }
    }

    fun follow(section: ListCategory) = setFollowing(section, true)

    fun unfollow(section: ListCategory) = setFollowing(section, false)

    private fun setFollowing(section: ListCategory, now: Boolean) {
        val before = sections.value
        sections.update { all -> all.map { if (it.section == section) it.followed(now) else it } }
        viewModelScope.launch {
            val result = if (now) community.followSection(section) else community.unfollowSection(section)
            result.fold(
                onSuccess = { notes.send(if (now) CommunityNotice.NowFollowing(section) else CommunityNotice.Unfollowed(section)) },
                onFailure = {
                    sections.value = before
                    notes.send(CommunityNotice.FollowFailed)
                },
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
            community.feed(query = typed).fold(
                onSuccess = { page -> results.value = SearchResults(lists = page.lists.filter(menu::isVisible)) },
                onFailure = { results.value = SearchResults(failed = true) },
            )
        }
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

/** The count moves with the button, in the same frame. */
internal fun SectionStats.followed(now: Boolean): SectionStats =
    if (following == now) this else copy(following = now, followerCount = (followerCount + if (now) 1 else -1).coerceAtLeast(0))
