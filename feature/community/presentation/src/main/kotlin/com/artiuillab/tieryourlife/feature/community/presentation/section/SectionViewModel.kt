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
import kotlinx.coroutines.flow.update
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

/** A section's page (§3): its counts and followers, its topics, and its lists in the order chosen. */
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
    val selectedTopic: Topic? = null,
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
    private val selectedTopicId = MutableStateFlow(route.topicId)
    private val chosenTopic = MutableStateFlow<Topic?>(null)
    private val sort = MutableStateFlow(sortMemory.sortOf(section))

    private val loaders = mutableMapOf<String, FeedLoader>()
    private val current = MutableStateFlow(loaderFor(route.topicId, sort.value))

    /** The feed on screen now: the section's, or the chosen topic's, in the order chosen. */
    internal val feed: FeedLoader get() = current.value

    val state: StateFlow<SectionUiState> = combine(
        combine(catalog.directory, sectionId) { directory, id -> directory to id },
        combine(topics, waiting) { t, w -> t to w },
        combine(selectedTopicId, chosenTopic, sort) { id, chosen, order -> Triple(id, chosen, order) },
        current.flatMapLatest { it.pane },
    ) { (directory, id), (t, w), (wanted, chosen, order), pane ->
        val selected = wanted?.let { topicId -> t.firstOrNull { it.id == topicId } ?: w.firstOrNull { it.id == topicId } ?: chosen?.takeIf { it.id == topicId } }
        val found = directory.find(id)
        SectionUiState(
            sectionId = id,
            section = found ?: Section(id = id),
            countsKnown = found != null && countsKnown(directory, found),
            topics = t,
            waiting = w,
            selectedTopic = selected,
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
        val wanted = selectedTopicId.value
        if (wanted != null && chosenTopic.value == null) {
            viewModelScope.launch { community.topic(wanted).onSuccess { chosenTopic.value = it } }
        }
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

    /** A chip, or All for null: the feed changes in place, and Back clears the topic before it leaves. */
    fun selectTopic(topic: Topic?) {
        if (selectedTopicId.value == topic?.id) return
        selectedTopicId.value = topic?.id
        chosenTopic.value = topic
        switchFeed()
    }

    fun selectSort(order: FeedSort) {
        if (sort.value == order) return
        sort.value = order
        sortMemory.remember(section, order)
        switchFeed()
    }

    private fun switchFeed() {
        val loader = loaderFor(selectedTopicId.value, sort.value)
        current.value = loader
        loader.open()
    }

    private fun loaderFor(topicId: String?, order: FeedSort): FeedLoader {
        val id = section
        val key = FeedKeys.section(id, topicId) + if (order == FeedSort.Recent) ":newest" else ""
        return loaders.getOrPut(key) {
            FeedLoader(key, viewModelScope, cache, clock, menu::isVisible) { after ->
                community.feed(category = id, topic = topicId, sort = order, after = after)
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

    /** The topic row's own button: it follows the topic, not the section. */
    fun toggleTopicFollow() {
        val topic = state.value.selectedTopic ?: return
        val now = !topic.following
        val changed = topic.copy(following = now, followerCount = (topic.followerCount + if (now) 1 else -1).coerceAtLeast(0))
        replaceTopic(changed)
        viewModelScope.launch {
            val result = if (now) community.followTopic(topic.id) else community.unfollowTopic(topic.id)
            result.onFailure {
                replaceTopic(topic)
                notes.send(CommunityNotice.FollowFailed)
            }
        }
    }

    private fun replaceTopic(topic: Topic) {
        topics.update { all -> all.map { if (it.id == topic.id) topic else it } }
        if (chosenTopic.value?.id == topic.id) chosenTopic.value = topic
    }

    fun hideList(summary: PublishedListSummary) = menu.hideList(feed, summary)

    fun hideAuthor(authorUid: String, name: String) = menu.hideAuthor(loaders.values.toList(), authorUid, name)

    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) = menu.report(feed, summary, reason, note)
}

/** A built-in's counts are the app's zeros until the list was read; anybody else's section only ever came from the server. */
private fun countsKnown(directory: SectionDirectory, found: Section): Boolean = directory.counted || found.art == null
