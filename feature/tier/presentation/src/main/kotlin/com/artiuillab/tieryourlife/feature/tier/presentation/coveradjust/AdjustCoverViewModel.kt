package com.artiuillab.tieryourlife.feature.tier.presentation.coveradjust

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.adjustCoverRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdjustCoverViewModel @Inject constructor(
    private val repository: TierRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tierListId = savedStateHandle.adjustCoverRoute().tierListId

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val _state = MutableStateFlow(AdjustCoverUiState())
    val state: StateFlow<AdjustCoverUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = runSuspendCatching { repository.getTierListById(tierListId) }
                .onFailure { Timber.w(it, "Reading the board for its cover failed") }
                .getOrNull()
            _state.update {
                it.copy(picture = list?.coverImageUrl, focus = list?.coverFocus ?: CoverFocus.CENTRED, loaded = true)
            }
        }
    }

    fun move(focus: CoverFocus) {
        _state.update { it.copy(focus = focus) }
    }

    fun reset() {
        _state.update { it.copy(focus = CoverFocus.CENTRED) }
    }

    fun save() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = messages.guard("Saving the cover's focus") {
                repository.setCoverFocus(tierListId, _state.value.focus.inRange().takeUnless { it.isCentred })
            }
            _state.update { it.copy(saving = false, saved = saved) }
        }
    }
}
