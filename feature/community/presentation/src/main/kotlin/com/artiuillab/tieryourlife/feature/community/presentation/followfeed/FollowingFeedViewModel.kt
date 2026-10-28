package com.artiuillab.tieryourlife.feature.community.presentation.followfeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedKeys
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Following
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
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
import timber.log.Timber
import javax.inject.Inject

/** Why a card is on the Following feed, when the card does not already say so (§4.1). */
sealed interface FeedReason {
    data class InTopic(val name: String) : FeedReason
    data class InSection(val sectionId: String) : FeedReason
}

/** A followed person needs no line, the author is on the card; then a topic; then a section. */
fun reasonFor(summary: PublishedListSummary, following: Following): FeedReason? {
    if (following.authors.any { it.uid == summary.authorUid }) return null
    val topicId = summary.topicId
    val topic = topicId?.let { id -> following.topics.firstOrNull { it.id == id } }
    if (topic != null) return FeedReason.InTopic(summary.topicName ?: topic.name)
    if (following.sections.any { it.id == summary.category }) return FeedReason.InSection(summary.category)
    return null
}

data class FollowingFeedUiState(
    /** Null until the server has said who and what is followed. */
    val following: Following? = null,
    /** This person's own topics still waiting for a moderator: seen only by them. */
    val waiting: List<Topic> = emptyList(),
    val feed: FeedPane = FeedPane(),
    /** Section ids followed from the empty tab's mini tiles in this sitting: their + is a check. */
    val quickFollowed: Set<String> = emptySet(),
    val nowMillis: Long = 0L,
    /** This person's own section proposals still waiting: in their strip, and nobody else's. */
    val waitingSections: List<Section> = emptyList(),
    /** Their proposals declined or merged, until dismissed. */
    val proposalNotices: List<Section> = emptyList(),
) {
    /** Nothing followed at all: the tab offers the sections instead of a feed. */
    val followsNothing: Boolean
        get() = following?.let {
            it.authors.isEmpty() && it.topics.isEmpty() && it.sections.isEmpty() && waiting.isEmpty() && waitingSections.isEmpty()
        } ?: feed.followingNobody
}

@HiltViewModel
class FollowingFeedViewModel @Inject constructor(
    private val community: CommunityRepository,
    cache: CommunityFeedCache,
    network: CommunityNetwork,
    preferences: AppPreferences,
    private val clock: CommunityClock,
    private val catalog: SectionCatalog,
) : ViewModel() {

    private val notes = Channel<CommunityNotice>(Channel.BUFFERED)
    private val menu = ListMenuActions(preferences, community, viewModelScope) { notes.send(CommunityNotice.ActionFailed) }

    internal val feed = FeedLoader(FeedKeys.FOLLOWING, viewModelScope, cache, clock, menu::isVisible) { after ->
        community.feed(sort = FeedSort.Recent, following = true, after = after)
    }

    private val following = MutableStateFlow<Following?>(null)
    private val waiting = MutableStateFlow<List<Topic>>(emptyList())
    private val quickFollowed = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<FollowingFeedUiState> = combine(following, waiting, feed.pane, quickFollowed, catalog.directory) { f, w, pane, quick, directory ->
        FollowingFeedUiState(
            following = f,
            waiting = w,
            feed = pane,
            quickFollowed = quick,
            nowMillis = clock.nowMillis(),
            waitingSections = directory.waiting,
            proposalNotices = directory.settledAgainst,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FollowingFeedUiState())

    val notices: Flow<CommunityNotice> = merge(notes.receiveAsFlow(), feed.notices.map { it.toCommunityNotice() })

    init {
        viewModelScope.launch {
            network.available.collect {
                feed.onNetworkBack()
                if (following.value == null) loadFollowing()
            }
        }
    }

    fun open() {
        loadFollowing()
        feed.open()
    }

    private fun loadFollowing() {
        viewModelScope.launch {
            community.following()
                .onSuccess { following.value = it }
                .onFailure { Timber.i(it, "Reading who and what is followed failed") }
        }
        viewModelScope.launch {
            community.myTopics()
                .onSuccess { mine -> waiting.value = mine.filter { it.status == TopicStatus.Pending } }
                .onFailure { Timber.i(it, "Reading this person's topics failed") }
        }
    }

    /** One tap on a mini tile: the + turns into a check at once, and the tab becomes the feed it now has. */
    fun quickFollow(sectionId: String) {
        if (sectionId in quickFollowed.value) return
        quickFollowed.update { it + sectionId }
        viewModelScope.launch {
            catalog.setFollowing(sectionId, now = true).fold(
                onSuccess = {
                    following.update { now ->
                        val base = now ?: Following(emptyList(), emptyList())
                        base.copy(sections = base.sections + FollowedSection(sectionId))
                    }
                    feed.reload()
                },
                onFailure = {
                    quickFollowed.update { it - sectionId }
                    notes.send(CommunityNotice.FollowFailed)
                },
            )
        }
    }

    /** Withdraws a waiting proposal, or dismisses the notice of a declined or merged one. */
    fun withdrawProposal(sectionId: String) {
        viewModelScope.launch { catalog.withdraw(sectionId).onFailure { notes.send(CommunityNotice.ActionFailed) } }
    }

    fun hideList(summary: PublishedListSummary) = menu.hideList(feed, summary)

    fun hideAuthor(authorUid: String, name: String) = menu.hideAuthor(listOf(feed), authorUid, name)

    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) = menu.report(feed, summary, reason, note)

    fun refreshHidden() = feed.refilter()
}
