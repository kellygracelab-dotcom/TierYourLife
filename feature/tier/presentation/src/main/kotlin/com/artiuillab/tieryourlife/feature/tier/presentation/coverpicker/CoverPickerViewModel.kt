package com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.core.theme.messages.guard
import com.artiuillab.tieryourlife.feature.tier.board.components.MOSAIC_CELLS
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.navigation.coverPickerRoute
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
class CoverPickerViewModel @Inject constructor(
    private val repository: TierRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tierListId = savedStateHandle.coverPickerRoute().tierListId

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private val _state = MutableStateFlow(CoverPickerUiState())
    val state: StateFlow<CoverPickerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = runSuspendCatching { repository.getTierListById(tierListId) }
                .onFailure { Timber.w(it, "Reading the board for its cover failed") }
                .getOrNull() ?: return@launch
            val pictures = list.tiers.flatMap { it.items }.mapNotNull { it.imageUrl }
            _state.update {
                it.copy(
                    title = list.title,
                    candidates = pictures.filter(::travels).distinct(),
                    automatic = pictures.distinct().take(MOSAIC_CELLS),
                    tierColors = list.tiers.filterNot { tier -> tier.isPool }.map { tier -> tier.colorLight },
                    chosen = list.coverImageUrl?.let(::listOf) ?: list.coverImageUrls,
                )
            }
        }
    }

    /** Chosen pictures keep the order of the taps; a second tap on one lets it go. */
    fun toggle(pictureUrl: String) {
        _state.update { shown ->
            val chosen = shown.chosen
            when {
                pictureUrl in chosen -> shown.copy(chosen = chosen - pictureUrl)
                // A photograph from this phone cannot sit in a collage, so a web picture beside it starts over.
                chosen.any { !travels(it) } -> shown.copy(chosen = listOf(pictureUrl))
                !shown.canChooseMore -> shown
                else -> shown.copy(chosen = chosen + pictureUrl)
            }
        }
    }

    /** The whole cover: only a web picture can join a collage. */
    fun pickFromGallery(uri: String) {
        _state.update { it.copy(chosen = listOf(uri)) }
    }

    fun reset() {
        _state.update { it.copy(chosen = emptyList()) }
    }

    fun showTab(tab: CoverPickerTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun save() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = messages.guard("Saving the cover") { repository.setCover(tierListId, _state.value.chosen) }
            _state.update { it.copy(saving = false, saved = saved) }
        }
    }
}

/** A web address reaches the community as it is; a file on this phone only travels by being uploaded, which a collage does not do. */
private fun travels(url: String): Boolean = url.startsWith("https://")
