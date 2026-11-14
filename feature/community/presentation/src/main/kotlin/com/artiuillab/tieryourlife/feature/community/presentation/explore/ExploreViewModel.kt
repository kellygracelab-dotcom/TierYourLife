package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.matchesSearch
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowAfterSignIn
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowRefusal
import com.artiuillab.tieryourlife.feature.community.presentation.follow.FollowTarget
import com.artiuillab.tieryourlife.feature.community.presentation.follow.GuestFollows
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val community: CommunityRepository,
    cache: CommunityFeedCache,
    network: CommunityNetwork,
    preferences: AppPreferences,
    private val clock: CommunityClock,
    private val catalog: SectionCatalog,
    afterSignIn: FollowAfterSignIn,
) : ViewModel() {

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)

    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(it) }

    internal val feed = FeedLoader(FeedKeys.EXPLORE, viewModelScope, cache, clock, menu::isVisible) { after ->
        community.feed(sort = FeedSort.Popular, after = after)
    }

    val picturesOnWifiOnly: () -> Boolean = preferences::picturesOnWifiOnly

    val search = ExploreSearch(community, viewModelScope, menu::isVisible) { typed ->
        catalog.directory.value.shown.any { it.matchesSearch(typed) }
    }

    val state: StateFlow<ExploreUiState> = combine(catalog.directory, feed.pane, search.query, search.results) { directory, pane, q, found ->
        ExploreUiState(
            sections = directory.shown,
            feed = pane,
            query = q,
            results = found,
            nowMillis = clock.nowMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreUiState())

    private val guest = GuestFollows(afterSignIn, viewModelScope)

    /** A guest's Follow opens the sign-in; signing in follows. */
    val signInRequests: Flow<Unit> = guest.signInRequests

    val notices: Flow<CommunityNotice> = merge(
        notes.receiveAsFlow(),
        feed.notices.map { notice -> notice.toCommunityNotice() },
    )

    init {
        guest.afterSignIn({ it is FollowTarget.Section }) { setFollowing((it as FollowTarget.Section).id, true) }
        viewModelScope.launch {
            network.available.collect {
                feed.onNetworkBack()
                if (!catalog.directory.value.counted) loadSections()
            }
        }
    }

    /** Every arrival: the kept feed first, then the server, every time. */
    fun open() {
        feed.open()
        loadSections()
    }

    /** At most every ten minutes: the catalog keeps the rest. */
    private fun loadSections() {
        viewModelScope.launch { catalog.refresh() }
    }

    fun follow(sectionId: String) {
        if (guest.mayFollow(FollowTarget.Section(sectionId))) setFollowing(sectionId, true)
    }

    fun unfollow(sectionId: String) = setFollowing(sectionId, false)

    private fun setFollowing(sectionId: String, now: Boolean) {
        viewModelScope.launch {
            catalog.setFollowing(sectionId, now).fold(
                onSuccess = { followed ->
                    notes.send(if (now) CommunityNotice.NowFollowing(followed, moved = followed != sectionId) else CommunityNotice.Unfollowed(sectionId))
                },
                onFailure = { notes.send(CommunityNotice.FollowFailed(FollowRefusal.of(now, it))) },
            )
        }
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
