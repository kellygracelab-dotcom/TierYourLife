package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStats
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.explore.CommunityNotice
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedLoader
import com.artiuillab.tieryourlife.feature.community.presentation.explore.FeedPane
import com.artiuillab.tieryourlife.feature.community.presentation.explore.ListMenuActions
import com.artiuillab.tieryourlife.feature.community.presentation.explore.followed
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
    private val sorts = mutableMapOf<ListCategory, FeedSort>()

    fun sortOf(section: ListCategory): FeedSort = sorts[section] ?: FeedSort.Popular

    fun remember(section: ListCategory, sort: FeedSort) {
        sorts[section] = sort
    }
}

/** A section's page (§3): its counts and followers, its topics, and its lists in the order chosen. */
data class SectionUiState(
    val section: ListCategory,
    /** Null until the server has counted. */
    val stats: SectionStats? = null,
    /** Approved topics, the most followed first. */
    val topics: List<Topic> = emptyList(),
    /** This person's own topics waiting for a moderator here: seen by nobody else. */
    val waiting: List<Topic> = emptyList(),
    val selectedTopic: Topic? = null,
    val sort: FeedSort = FeedSort.Popular,
    val feed: FeedPane = FeedPane(),
    val nowMillis: Long = 0L,
)

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
) : ViewModel() {

    private val route = savedStateHandle.sectionRoute()
    val section: ListCategory = ListCategory.fromId(route.sectionId) ?: ListCategory.Other

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)
    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(CommunityNotice.ActionFailed) }

    private val stats = MutableStateFlow<SectionStats?>(null)
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
        combine(stats, topics, waiting) { s, t, w -> Triple(s, t, w) },
        combine(selectedTopicId, chosenTopic, sort) { id, chosen, order -> Triple(id, chosen, order) },
        current.flatMapLatest { it.pane },
    ) { (s, t, w), (id, chosen, order), pane ->
        val selected = id?.let { wanted -> t.firstOrNull { it.id == wanted } ?: w.firstOrNull { it.id == wanted } ?: chosen?.takeIf { it.id == wanted } }
        SectionUiState(section, s, t, w, selected, order, pane, clock.nowMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SectionUiState(section))

    val notices: Flow<CommunityNotice> = merge(
        notes.receiveAsFlow(),
        current.flatMapLatest { loader -> loader.notices.map { it.toCommunityNotice() } },
    )

    init {
        viewModelScope.launch { network.available.collect { feed.onNetworkBack() } }
    }

    fun open() {
        feed.open()
        viewModelScope.launch {
            community.sections().onSuccess { all -> stats.value = all.firstOrNull { it.section == section } }
        }
        viewModelScope.launch {
            community.topics(section = section)
                .onSuccess { found -> topics.value = found.filter { it.status == TopicStatus.Approved }.sortedByDescending { it.followerCount } }
                .onFailure { Timber.i(it, "Reading a section's topics failed") }
        }
        viewModelScope.launch {
            community.myTopics().onSuccess { mine ->
                // A waiting topic's lists sit under Other until a moderator places it.
                waiting.value = mine.filter { it.status == TopicStatus.Pending && (it.section ?: ListCategory.Other) == section }
            }
        }
        val wanted = selectedTopicId.value
        if (wanted != null && chosenTopic.value == null) {
            viewModelScope.launch { community.topic(wanted).onSuccess { chosenTopic.value = it } }
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
        val key = FeedKeys.section(section.id, topicId) + if (order == FeedSort.Recent) ":newest" else ""
        return loaders.getOrPut(key) {
            FeedLoader(key, viewModelScope, cache, clock, menu::isVisible) { after ->
                community.feed(category = section, topic = topicId, sort = order, after = after)
            }
        }
    }

    /** Answers in the same frame; goes back, with a snackbar, if the server refuses. */
    fun toggleSectionFollow() {
        val before = stats.value ?: SectionStats(section)
        val now = !before.following
        stats.value = before.followed(now)
        viewModelScope.launch {
            val result = if (now) community.followSection(section) else community.unfollowSection(section)
            result.fold(
                onSuccess = { if (!now) notes.send(CommunityNotice.Unfollowed(section)) },
                onFailure = {
                    stats.value = before
                    notes.send(CommunityNotice.FollowFailed)
                },
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
