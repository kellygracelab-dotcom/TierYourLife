package com.artiuillab.tieryourlife.feature.community.presentation.topic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.feed.CommunityFeed
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.topicRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TopicViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.topicRoute()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val _state = MutableStateFlow(
        TopicUiState(name = route.name.orEmpty(), section = route.section),
    )
    val state: StateFlow<TopicUiState> = _state.asStateFlow()

    private var cursor: String? = null
    private var loadJob: Job? = null
    private var moreJob: Job? = null

    init {
        loadTopic()
        load()
    }

    private fun loadTopic() {
        viewModelScope.launch {
            community.topic(route.topicId).fold(
                onSuccess = { topic ->
                    _state.update { it.copy(topic = topic, name = topic.name, section = topic.section, headerFailed = false) }
                },
                onFailure = { error ->
                    Timber.w(error, "Reading a topic failed")
                    _state.update { it.copy(headerFailed = true) }
                },
            )
        }
    }

    fun load() {
        loadJob?.cancel()
        moreJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(feed = CommunityFeed.Loading) }
            val hidden = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
            val feed = community.feed(topic = route.topicId, sort = _state.value.sort).fold(
                onSuccess = { page ->
                    cursor = page.nextCursor
                    CommunityFeed.Ready(
                        lists = page.lists.filterNot { it.id in hidden || it.authorUid in hidden },
                        canLoadMore = page.nextCursor != null,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Loading a topic's lists failed")
                    cursor = null
                    if (error is AppUnverified) CommunityFeed.Unverified else CommunityFeed.Failed
                },
            )
            _state.update { it.copy(feed = feed) }
        }
    }

    fun loadMore() {
        val shown = _state.value.feed as? CommunityFeed.Ready ?: return
        val after = cursor ?: return
        if (shown.loadingMore) return
        _state.update { it.copy(feed = shown.copy(loadingMore = true)) }
        moreJob = viewModelScope.launch {
            community.feed(topic = route.topicId, sort = _state.value.sort, after = after).fold(
                onSuccess = { page ->
                    cursor = page.nextCursor
                    val hidden = preferences.hiddenListIds() + preferences.hiddenAuthorUids()
                    _state.update { current ->
                        val now = current.feed as? CommunityFeed.Ready ?: return@update current
                        current.copy(
                            feed = now.copy(
                                lists = now.lists + page.lists.filterNot { it.id in hidden || it.authorUid in hidden },
                                canLoadMore = page.nextCursor != null,
                                loadingMore = false,
                            ),
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Loading another page of a topic failed")
                    _state.update { current ->
                        val now = current.feed as? CommunityFeed.Ready ?: return@update current
                        current.copy(feed = now.copy(loadingMore = false))
                    }
                },
            )
        }
    }

    fun selectSort(sort: FeedSort) {
        if (_state.value.sort == sort) return
        _state.update { it.copy(sort = sort) }
        load()
    }

    /** Answers before the server does, and goes back if the request fails. */
    fun toggleFollow() {
        val was = _state.value.topic ?: return
        if (was.status != TopicStatus.Approved) return
        val now = was.copy(following = !was.following)
        _state.update { it.copy(topic = now) }
        viewModelScope.launch {
            val result = if (now.following) community.followTopic(route.topicId) else community.unfollowTopic(route.topicId)
            result.onFailure { error ->
                Timber.w(error, "Following a topic failed")
                _state.update { it.copy(topic = was) }
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    /** A reader's word that the list does not belong here; a moderator moves it, nothing hides. */
    fun notThisTopic(summary: PublishedListSummary) {
        viewModelScope.launch {
            community.report(summary.id, ReportReason.WrongCategory, null).onFailure { error ->
                Timber.w(error, "Reporting a list as out of place failed")
                messages.send(UserMessage.ActionFailed)
            }
        }
    }

    fun hideList(summary: PublishedListSummary) {
        preferences.hideList(summary.id, summary.title)
        _state.update { current ->
            val now = current.feed as? CommunityFeed.Ready ?: return@update current
            current.copy(feed = now.copy(lists = now.lists.filterNot { it.id == summary.id }))
        }
    }

    fun report(summary: PublishedListSummary, reason: ReportReason, note: String?) {
        hideList(summary)
        viewModelScope.launch {
            community.report(summary.id, reason, note).onFailure { error ->
                Timber.w(error, "Could not file the report")
                preferences.unhideList(summary.id)
                messages.send(UserMessage.ActionFailed)
            }
        }
    }
}
