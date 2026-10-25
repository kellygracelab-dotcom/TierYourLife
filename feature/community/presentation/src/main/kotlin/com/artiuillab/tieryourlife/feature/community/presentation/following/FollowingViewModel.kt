package com.artiuillab.tieryourlife.feature.community.presentation.following

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.community.domain.model.FollowedAuthor
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface FollowingUiState {
    data object Loading : FollowingUiState

    data class Ready(val authors: List<FollowedAuthor>) : FollowingUiState

    data object Failed : FollowingUiState
}

@HiltViewModel
class FollowingViewModel @Inject constructor(
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FollowingUiState>(FollowingUiState.Loading)
    val state: StateFlow<FollowingUiState> = _state.asStateFlow()

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = FollowingUiState.Loading
            _state.value = community.following().fold(
                onSuccess = { FollowingUiState.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "Reading who is followed failed")
                    FollowingUiState.Failed
                },
            )
        }
    }

    /** Gone from the list at once; back in the same place if the server refuses. */
    fun unfollow(uid: String) {
        val current = _state.value as? FollowingUiState.Ready ?: return
        val index = current.authors.indexOfFirst { it.uid == uid }
        if (index < 0) return
        val person = current.authors[index]
        _state.value = current.copy(authors = current.authors - person)

        viewModelScope.launch {
            community.unfollow(uid).onFailure { error ->
                Timber.w(error, "Unfollowing failed")
                _state.update { state ->
                    if (state !is FollowingUiState.Ready) return@update state
                    state.copy(authors = state.authors.toMutableList().apply { add(index.coerceAtMost(size), person) })
                }
                messages.send(UserMessage.ActionFailed)
            }
        }
    }
}
