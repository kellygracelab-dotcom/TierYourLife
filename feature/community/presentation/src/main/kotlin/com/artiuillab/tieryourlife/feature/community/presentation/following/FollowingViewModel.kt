package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedSection
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface FollowingUiState {
    data object Loading : FollowingUiState

    /**
     * Sections, topics and people, each with its own way to stop (§4.3). A
     * row stopped in this sitting stays, its button turned back to Follow, so
     * Undo has something to point at.
     */
    data class Ready(
        val authors: List<FollowedAuthor>,
        val topics: List<Topic> = emptyList(),
        val sections: List<FollowedSection> = emptyList(),
        /** This person's own topics still waiting: they show, with nothing to stop. */
        val waiting: List<Topic> = emptyList(),
        /** Keys of the rows stopped while this screen has been open. */
        val stopped: Set<String> = emptySet(),
    ) : FollowingUiState {
        val isEmpty: Boolean get() = authors.isEmpty() && topics.isEmpty() && sections.isEmpty() && waiting.isEmpty()
    }

    data object Failed : FollowingUiState
}

/** The names Manage's rows go by, so one call and one Undo serve all three kinds. */
object FollowKeys {
    fun section(section: ListCategory) = "s:${section.id}"
    fun topic(id: String) = "t:$id"
    fun person(uid: String) = "p:$uid"
}

sealed interface ManageNotice {
    /** With Undo, which follows again. */
    data class Stopped(val name: String, val key: String) : ManageNotice
    data object Failed : ManageNotice
}

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val state: StateFlow<FollowingUiState> = _state.asStateFlow()

    private val notes = Channel<ManageNotice>(Channel.BUFFERED)
    val notices: Flow<ManageNotice> = notes.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = FollowingUiState.Loading
            val waiting = community.myTopics()
                .onFailure { Timber.i(it, "Reading this person's topics failed") }
                .getOrDefault(emptyList())
                .filter { it.status == TopicStatus.Pending }
            _state.value = community.following().fold(
                onSuccess = { following ->
                    FollowingUiState.Ready(
                        authors = following.authors,
                        topics = following.topics.filterNot { it.status == TopicStatus.Pending },
                        sections = following.sections,
                        waiting = waiting,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Reading who is followed failed")
                    FollowingUiState.Failed
                },
            )
        }
    }

    /** Following → Follow at once, and back again; a refusal undoes the flip and says so. */
    fun toggle(key: String) {
        val current = _state.value as? FollowingUiState.Ready ?: return
        val stopping = key !in current.stopped
        val name = nameOf(current, key) ?: return
        flip(key, stopped = stopping)
        viewModelScope.launch {
            val result = if (stopping) stop(key) else resume(key)
            result.fold(
                onSuccess = { if (stopping) notes.send(ManageNotice.Stopped(name, key)) },
                onFailure = { error ->
                    Timber.w(error, "Changing a follow failed")
                    flip(key, stopped = !stopping)
                    notes.send(ManageNotice.Failed)
                },
            )
        }
    }

    private fun flip(key: String, stopped: Boolean) {
        _state.update { state ->
            if (state !is FollowingUiState.Ready) return@update state
            state.copy(stopped = if (stopped) state.stopped + key else state.stopped - key)
        }
    }

    private suspend fun stop(key: String): Result<Unit> {
        val id = key.substringAfter(':')
        return when (key.substringBefore(':')) {
            "s" -> ListCategory.fromId(id)?.let { community.unfollowSection(it) } ?: Result.success(Unit)
            "t" -> community.unfollowTopic(id)
            else -> community.unfollow(id)
        }
    }

    private suspend fun resume(key: String): Result<Unit> {
        val id = key.substringAfter(':')
        return when (key.substringBefore(':')) {
            "s" -> ListCategory.fromId(id)?.let { community.followSection(it) } ?: Result.success(Unit)
            "t" -> community.followTopic(id)
            else -> community.follow(id)
        }
    }

    /** The section's own name is a resource the screen reads; here its id stands for it. */
    private fun nameOf(state: FollowingUiState.Ready, key: String): String? {
        val id = key.substringAfter(':')
        return when (key.substringBefore(':')) {
            "s" -> state.sections.firstOrNull { it.section.id == id }?.section?.id
            "t" -> state.topics.firstOrNull { it.id == id }?.name
            else -> state.authors.firstOrNull { it.uid == id }?.name
        }
    }
}
