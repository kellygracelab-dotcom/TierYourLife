package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ModerationUiState {
    data object Loading : ModerationUiState

    data class Ready(val reports: List<ModerationReport>, val settling: String? = null) : ModerationUiState

    data object Failed : ModerationUiState
}

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val community: CommunityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ModerationUiState>(ModerationUiState.Loading)
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = community.reports().fold(
                onSuccess = { ModerationUiState.Ready(it) },
                onFailure = { error ->
                    Timber.w(error, "Reading the report queue failed")
                    ModerationUiState.Failed
                },
            )
        }
    }

    fun takeDown(listId: String) = settle(listId) { community.takeDown(listId) }

    fun dismiss(listId: String) = settle(listId) { community.dismissReports(listId) }

    /**
     * Both endings close every complaint about the list, so every row for it
     * leaves together. Failing leaves them all where they were: a queue that
     * quietly loses entries is worse than one that will not budge.
     */
    private fun settle(listId: String, act: suspend () -> Result<Unit>) {
        val shown = _state.value as? ModerationUiState.Ready ?: return
        if (shown.settling != null) return

        _state.value = shown.copy(settling = listId)
        viewModelScope.launch {
            act().fold(
                onSuccess = {
                    _state.value = ModerationUiState.Ready(shown.reports.filterNot { it.listId == listId })
                },
                onFailure = { error ->
                    Timber.w(error, "Settling reports about $listId failed")
                    _state.value = shown.copy(settling = null)
                },
            )
        }
    }
}
