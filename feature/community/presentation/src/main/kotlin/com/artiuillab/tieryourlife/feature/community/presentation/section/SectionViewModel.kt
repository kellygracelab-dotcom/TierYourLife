package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedLoader
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ListMenuActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.toCommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.sectionRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** The order each section was last read in, for this run of the app (§3.1). */
@Singleton
class SectionSortMemory @Inject constructor() {
    private val sorts = mutableMapOf<String, FeedSort>()

    fun sortOf(sectionId: String): FeedSort = sorts[sectionId] ?: FeedSort.Popular

    fun remember(sectionId: String, sort: FeedSort) {
        sorts[sectionId] = sort
    }
}

/** A section's page (§3): whether it is followed, its topics, and its lists in the order chosen. */
data class SectionUiState(
    /** The id this page is about: the route's, or the one it was merged into. */
    val sectionId: String,
    /** What to draw and name; Other's look while the id is still being asked about. */
    val section: Section = Section(id = sectionId),
    /** False while the counts are still the app's own zeros. */
    val countsKnown: Boolean = false,
    /** Approved topics, the most followed first. */
    val topics: List<Topic> = emptyList(),
    /** This person's own topics waiting for a moderator here: seen by nobody else. */
    val waiting: List<Topic> = emptyList(),
    val sort: FeedSort = FeedSort.Popular,
    val feed: FeedPane = FeedPane(),
    val nowMillis: Long = 0L,
) {
    /** The section with its counts, once they are the server's; null until then. */
    val stats: Section? get() = section.takeIf { countsKnown }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val community: CommunityRepository,
    private val cache: CommunityFeedCache,
    network: CommunityNetwork,
    preferences: AppPreferences,
    private val clock: CommunityClock,
    private val sortMemory: SectionSortMemory,
    private val catalog: SectionCatalog,
) : ViewModel() {

    private val route = savedStateHandle.sectionRoute()
    private val sectionId = MutableStateFlow(catalog.directory.value.resolve(route.sectionId))

    /** The id this page is about now. */
    val section: String get() = sectionId.value

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)
    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(CommunityNotice.ActionFailed) }

    private val topics = MutableStateFlow<List<Topic>>(emptyList())
    private val waiting = MutableStateFlow<List<Topic>>(emptyList())
    private val sort = MutableStateFlow(sortMemory.sortOf(section))

    private val loaders = mutableMapOf<String, FeedLoader>()
    private val current = MutableStateFlow(loaderFor(sort.value))

    /** The feed on screen now: the section's, in the order chosen. */
    internal val feed: FeedLoader get() = current.value

    val state: StateFlow<SectionUiState> = combine(
        combine(catalog.directory, sectionId) { directory, id -> directory to id },
        combine(topics, waiting) { t, w -> t to w },
        sort,
        current.flatMapLatest { it.pane },
    ) { (directory, id), (t, w), order, pane ->
        val found = directory.find(id)
        SectionUiState(
            sectionId = id,
            section = found ?: Section(id = id),
            countsKnown = found != null && countsKnown(directory, found),
            topics = t,
            waiting = w,
            sort = order,
            feed = pane,
            nowMillis = clock.nowMillis(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SectionUiState(section))

    val notices: Flow<CommunityNotice> = merge(
        notes.receiveAsFlow(),
        current.flatMapLatest { loader -> loader.notices.map { it.toCommunityNotice() } },
    )

    init {
        viewModelScope.launch { network.available.collect { feed.onNetworkBack() } }
        // A link to a section that was merged away opens the one it became, as soon as that is known.
        viewModelScope.launch {
            catalog.directory.map { it.resolve(route.sectionId) }.distinctUntilChanged().collect { id ->
                if (id != sectionId.value) {
                    sectionId.value = id
                    switchFeed()
                    loadAround()
                }
            }
        }
    }

    fun open() {
        feed.open()
        if (!catalog.directory.value.settled(section)) catalog.lookUp(section)
        viewModelScope.launch { catalog.refresh() }
        loadAround()
    }

    /** The section's topics, and this person's own waiting ones in it. */
    private fun loadAround() {
        val id = section
        viewModelScope.launch {
            community.topics(section = id)
                .onSuccess { found -> topics.value = found.filter { it.status == TopicStatus.Approved }.sortedByDescending { it.followerCount } }
                .onFailure { Timber.i(it, "Reading a section's topics failed") }
        }
        viewModelScope.launch {
            community.myTopics().onSuccess { mine ->
                // A waiting topic's lists sit under Other until a moderator places it.
                waiting.value = mine.filter { it.status == TopicStatus.Pending && (it.section ?: ListCategory.Other.id) == id }
            }
        }
    }

    fun selectSort(order: FeedSort) {
        if (sort.value == order) return
        sort.value = order
        sortMemory.remember(section, order)
        switchFeed()
    }

    private fun switchFeed() {
        val loader = loaderFor(sort.value)
        current.value = loader
        loader.open()
    }

    private fun loaderFor(order: FeedSort): FeedLoader {
        val id = section
        val key = FeedKeys.section(id, null) + if (order == FeedSort.Recent) ":newest" else ""
        return loaders.getOrPut(key) {
            FeedLoader(key, viewModelScope, cache, clock, menu::isVisible) { after ->
                community.feed(category = id, sort = order, after = after)
            }
        }
    }

    /** Answers in the same frame; goes back, with a snackbar, if the server refuses. */
    fun toggleSectionFollow() {
        val id = section
        val now = catalog.directory.value.find(id)?.following != true
        viewModelScope.launch {
            catalog.setFollowing(id, now).fold(
                onSuccess = { if (!now) notes.send(CommunityNotice.Unfollowed(id)) },
                onFailure = { notes.send(CommunityNotice.FollowFailed) },
            )
        }
    }

    fun hideList(summary: PublishedListSummary) = menu.hideList(feed, summary)

    fun hideAuthor(authorUid: String, name: String) = menu.hideAuthor(loaders.values.toList(), authorUid, name)

    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) = menu.report(feed, summary, reason, note)
}

/** A built-in's counts are the app's zeros until the list was read; anybody else's section only ever came from the server. */
private fun countsKnown(directory: SectionDirectory, found: Section): Boolean = directory.counted || found.art == null
