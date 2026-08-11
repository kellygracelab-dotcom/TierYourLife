package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.AiStudioRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiStudioViewModel @Inject constructor(
    private val generator: CardImageGenerator,
    private val saver: GeneratedCardSaver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AiStudioRoute>()
    val tierListId = route.tierListId
    val listTitle = route.listTitle

    private val _state = MutableStateFlow(AiStudioUiState())
    val state: StateFlow<AiStudioUiState> = _state.asStateFlow()

    private var nextExchangeId = 1L

    fun send(prompt: String) {
        if (_state.value.generating) return
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        val exchangeId = nextExchangeId++
        _state.update {
            it.copy(
                exchanges = it.exchanges + AiExchange(exchangeId, trimmed, AiExchangePhase.Generating),
                generating = true,
            )
        }
        viewModelScope.launch { completeGeneration(exchangeId, trimmed) }
    }

    fun retry(exchangeId: Long) {
        if (_state.value.generating) return
        val exchange = _state.value.exchanges.firstOrNull { it.id == exchangeId } ?: return
        startGenerating(exchangeId)
        viewModelScope.launch { completeGeneration(exchangeId, exchange.prompt) }
    }

    fun regenerate(exchangeId: Long) {
        if (_state.value.generating) return
        val exchange = _state.value.exchanges.firstOrNull { it.id == exchangeId } ?: return
        val previousImage = (exchange.phase as? AiExchangePhase.Result)?.image
        startGenerating(exchangeId)
        viewModelScope.launch {
            if (previousImage != null) {
                generator.discard(previousImage)
            }
            completeGeneration(exchangeId, exchange.prompt)
        }
    }

    fun addToList(exchangeId: Long, title: String, onAdded: (itemId: Long) -> Unit) {
        val exchange = _state.value.exchanges.firstOrNull { it.id == exchangeId } ?: return
        val image = (exchange.phase as? AiExchangePhase.Result)?.image ?: return
        viewModelScope.launch {
            val newItemId = saver.save(tierListId, title, image.imageUri)
            onAdded(newItemId)
        }
    }

    private fun startGenerating(exchangeId: Long) {
        _state.update { current ->
            current.withPhase(exchangeId, AiExchangePhase.Generating).copy(generating = true)
        }
    }

    private suspend fun completeGeneration(exchangeId: Long, prompt: String) {
        val phase = runCatching { generator.generate(prompt) }.fold(
            onSuccess = { image -> AiExchangePhase.Result(image) },
            onFailure = { AiExchangePhase.Failed },
        )
        _state.update { current -> current.withPhase(exchangeId, phase).copy(generating = false) }
    }

    private fun AiStudioUiState.withPhase(exchangeId: Long, phase: AiExchangePhase): AiStudioUiState =
        copy(
            exchanges = exchanges.map { exchange ->
                if (exchange.id == exchangeId) exchange.copy(phase = phase) else exchange
            },
        )
}
