package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.community.domain.model.Topic
import com.artiuillab.tieryourlife.feature.community.domain.model.TopicStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.topicPickerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SEARCH_DELAY_MILLIS = 300L

@HiltViewModel
class TopicPickerViewModel @Inject constructor(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tierListId = savedStateHandle.topicPickerRoute().tierListId

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val _state = MutableStateFlow(TopicPickerUiState())
    val state: StateFlow<TopicPickerUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Each quietly: the picker stands without either answer.
        val mine = viewModelScope.async { community.myTopics() }
        viewModelScope.launch {
            val list = runSuspendCatching { repository.getTierListById(tierListId) }
                .onFailure { Timber.w(it, "Reading the board for its topic failed") }
                .getOrNull() ?: return@launch
            val topicId = list.topicId
            val newName = list.newTopicName
            val chosen = when {
                topicId != null -> TopicChoice.Known(
                    Topic(
                        id = topicId,
                        name = list.topicName.orEmpty(),
                        section = list.category,
                        // A name still beside the id is the board's memory of waiting.
                        status = if (newName != null) TopicStatus.Pending else TopicStatus.Approved,
                    ),
                )
                newName != null -> TopicChoice.New(newName)
                else -> TopicChoice.None
            }
            _state.update { it.copy(title = list.title, chosen = chosen) }
            suggestFor(list.title)
            if (topicId != null) settle(list, mine.await().getOrNull())
        }
        viewModelScope.launch {
            mine.await()
                .onFailure { Timber.w(it, "Reading this person's topics failed") }
                .onSuccess { known -> _state.update { it.copy(mine = known) } }
        }
        search("")
    }

    /**
     * The server's latest word on a topic of this person's own: approved
     * clears the board's waiting mark, folded into another points the board
     * there, turned down takes the topic off the board.
     */
    private suspend fun settle(list: TierList, mine: List<Topic>?) {
        val own = mine?.firstOrNull { it.id == list.topicId } ?: return
        val now = when (own.status) {
            TopicStatus.Pending, TopicStatus.Approved -> own
            TopicStatus.Merged -> own.mergedInto?.let { community.topic(it).getOrNull() }
            TopicStatus.Rejected -> null
        }
        val waiting = now?.status == TopicStatus.Pending
        if (now?.id == list.topicId && waiting == (list.newTopicName != null)) return
        val recorded = runSuspendCatching { repository.setTopic(list.id, now?.id, now?.name, now?.name?.takeIf { waiting }) }
            .onFailure { Timber.w(it, "Settling the board's topic failed") }
            .isSuccess
        if (recorded) _state.update { it.copy(chosen = now?.let { topic -> TopicChoice.Known(topic) } ?: TopicChoice.None) }
    }

    /** The one an existing topic would be looked up by, and only when the title plainly contains it. */
    private suspend fun suggestFor(title: String) {
        val word = topicLookupWord(title) ?: return
        val match = community.topics(query = word)
            .onFailure { Timber.i(it, "Looking for a topic the title names failed") }
            .getOrDefault(emptyList())
            .firstOrNull { title.contains(it.name, ignoreCase = true) }
        _state.update { it.copy(suggestion = match) }
    }

    /** A request, not a filter over what is on screen: waits for the typing to settle. */
    fun onQueryChange(typed: String) {
        _state.update { it.copy(query = typed) }
        search(typed, settle = true)
    }

    private fun search(query: String, settle: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (settle) delay(SEARCH_DELAY_MILLIS)
            _state.update { it.copy(searching = true) }
            val found = community.topics(query = query.takeIf { it.isNotBlank() })
                .onFailure { Timber.w(it, "Searching topics failed") }
                .getOrDefault(emptyList())
            _state.update { it.copy(results = found, searching = false) }
        }
    }

    fun choose(topic: Topic) {
        _state.update { it.copy(chosen = TopicChoice.Known(topic)) }
    }

    fun chooseNone() {
        _state.update { it.copy(chosen = TopicChoice.None) }
    }

    /** A name the server already knows is that topic, not a twin of it. */
    fun createNew() {
        val shown = _state.value
        val name = cleanTopicName(shown.query) ?: return
        val existing = (shown.results + shown.mine).firstOrNull { it.name.equals(name, ignoreCase = true) }
        when {
            existing != null -> choose(existing)
            shown.canCreate -> _state.update { it.copy(chosen = TopicChoice.New(name)) }
        }
    }

    fun save() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val chosen = _state.value.chosen
            val saved = messages.guard("Saving the topic") {
                when (chosen) {
                    TopicChoice.None -> repository.setTopic(tierListId, null, null, null)
                    is TopicChoice.Known ->
                        repository.setTopic(tierListId, chosen.topic.id, chosen.topic.name, chosen.topic.name.takeIf { chosen.topic.pending })
                    is TopicChoice.New -> repository.setTopic(tierListId, null, null, chosen.name)
                }
            }
            _state.update { it.copy(saving = false, saved = saved) }
        }
    }
}
