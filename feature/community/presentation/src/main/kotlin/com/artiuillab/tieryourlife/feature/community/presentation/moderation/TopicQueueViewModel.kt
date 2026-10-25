package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.Moderator
import com.artiuillab.tieryourlife.feature.community.domain.model.PendingTopic
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
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

/** The words people proposed, waiting to be placed, renamed, folded into another, or turned down. */
sealed interface TopicQueueUiState {
    data object Loading : TopicQueueUiState

    data class Ready(
        val pending: List<PendingTopic>,
        /** The topic a decision is on its way for; its row waits. */
        val settling: String? = null,
        /** Approved topics a search for a merge target found. */
        val candidates: List<Topic> = emptyList(),
    ) : TopicQueueUiState

    data object Failed : TopicQueueUiState
}

/** Who decides, as the owner sees it. Everyone else is [Unavailable] and gets no tab. */
sealed interface ModeratorsUiState {
    data object Loading : ModeratorsUiState

    data class Ready(val moderators: List<Moderator>, val busy: Boolean = false) : ModeratorsUiState

    data object Unavailable : ModeratorsUiState
}

/**
 * The other two queues on the moderation screen: topics for every moderator,
 * moderators for the owner. Apart from the reports so each stays readable.
 */
@HiltViewModel
class TopicQueueViewModel @Inject constructor(
    private val community: CommunityRepository,
    private val owner: OwnerMemory,
) : ViewModel() {

    private val _topics = MutableStateFlow<TopicQueueUiState>(TopicQueueUiState.Loading)
    val topics: StateFlow<TopicQueueUiState> = _topics.asStateFlow()

    private val _moderators = MutableStateFlow<ModeratorsUiState>(ModeratorsUiState.Loading)
    val moderators: StateFlow<ModeratorsUiState> = _moderators.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private var candidatesJob: Job? = null

    init {
        loadTopics()
        loadModerators()
    }

    fun loadTopics() {
        viewModelScope.launch {
            _topics.value = community.pendingTopics().fold(
                onSuccess = { TopicQueueUiState.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "Reading the topic queue failed")
                    TopicQueueUiState.Failed
                },
            )
        }
    }

    /** Places the topic and moves every list filed under it; the row is gone once the server agrees. */
    fun approveTopic(id: String, section: ListCategory) = decide(id) { community.approveTopic(id, section) }

    fun mergeTopic(id: String, into: String) = decide(id) { community.mergeTopic(id, into) }

    fun rejectTopic(id: String) = decide(id) { community.rejectTopic(id) }

    /** The row stays, under its new name: renaming decides nothing yet. */
    fun renameTopic(id: String, name: String) {
        val shown = readyToAct() ?: return
        _topics.value = shown.copy(settling = id)
        viewModelScope.launch {
            community.renameTopic(id, name).fold(
                onSuccess = { renamed ->
                    _topics.update { current ->
                        val now = current as? TopicQueueUiState.Ready ?: return@update current
                        now.copy(
                            pending = now.pending.map { if (it.topic.id == id) it.copy(topic = renamed) else it },
                            settling = null,
                        )
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Renaming a topic failed")
                    _topics.update { (it as? TopicQueueUiState.Ready)?.copy(settling = null) ?: it }
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    /** Approved topics only: a waiting one cannot be the home of another. */
    fun findMergeTargets(query: String) {
        candidatesJob?.cancel()
        val wanted = query.trim()
        if (wanted.isEmpty()) {
            _topics.update { (it as? TopicQueueUiState.Ready)?.copy(candidates = emptyList()) ?: it }
            return
        }
        candidatesJob = viewModelScope.launch {
            val found = community.topics(query = wanted)
                .onFailure { Timber.w(it, "Searching merge targets failed") }
                .getOrDefault(emptyList())
                .filter { it.status == TopicStatus.Approved }
            _topics.update { (it as? TopicQueueUiState.Ready)?.copy(candidates = found) ?: it }
        }
    }

    private fun readyToAct(): TopicQueueUiState.Ready? =
        (_topics.value as? TopicQueueUiState.Ready)?.takeIf { it.settling == null }

    private fun decide(id: String, act: suspend () -> Result<Topic>) {
        val shown = readyToAct() ?: return
        _topics.value = shown.copy(settling = id)
        viewModelScope.launch {
            act().fold(
                onSuccess = {
                    _topics.update { current ->
                        val now = current as? TopicQueueUiState.Ready ?: return@update current
                        now.copy(pending = now.pending.filterNot { it.topic.id == id }, settling = null, candidates = emptyList())
                    }
                },
                onFailure = { error ->
                    Timber.w(error, "Deciding on topic $id failed")
                    _topics.update { (it as? TopicQueueUiState.Ready)?.copy(settling = null) ?: it }
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    /** Answered only for the owner; being turned away is the ordinary case and draws nothing. */
    fun loadModerators() {
        viewModelScope.launch {
            _moderators.value = community.moderators().fold(
                onSuccess = { list ->
                    owner.learned(list.mapTo(mutableSetOf()) { it.uid })
                    ModeratorsUiState.Ready(list)
                },
                onFailure = { error ->
                    Timber.i(error, "Not showing the moderators")
                    ModeratorsUiState.Unavailable
                },
            )
        }
    }

    fun appointModerator(uid: String, name: String) {
        val shown = (_moderators.value as? ModeratorsUiState.Ready)?.takeIf { !it.busy } ?: return
        val trimmedUid = uid.trim()
        if (trimmedUid.isEmpty() || shown.moderators.any { it.uid == trimmedUid }) return
        _moderators.value = shown.copy(busy = true)
        viewModelScope.launch {
            community.appointModerator(trimmedUid, name.trim().takeIf { it.isNotEmpty() }, null).fold(
                onSuccess = {
                    owner.appointed(trimmedUid)
                    val appointed = Moderator(
                        uid = trimmedUid,
                        name = name.trim(),
                        photoUrl = null,
                        appointedAtMillis = System.currentTimeMillis(),
                    )
                    _moderators.value = ModeratorsUiState.Ready(shown.moderators + appointed)
                },
                onFailure = { error ->
                    Timber.w(error, "Appointing a moderator failed")
                    _moderators.value = shown
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }

    fun removeModerator(uid: String) {
        val shown = (_moderators.value as? ModeratorsUiState.Ready)?.takeIf { !it.busy } ?: return
        _moderators.value = shown.copy(busy = true)
        viewModelScope.launch {
            community.removeModerator(uid).fold(
                onSuccess = {
                    owner.removed(uid)
                    _moderators.value = ModeratorsUiState.Ready(shown.moderators.filterNot { it.uid == uid })
                },
                onFailure = { error ->
                    Timber.w(error, "Removing a moderator failed")
                    _moderators.value = shown
                    messages.send(UserMessage.ActionFailed)
                },
            )
        }
    }
}
