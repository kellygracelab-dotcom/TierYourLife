package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.core.ui.logFailures
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.AiStudioRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val GENERATION_LOG_TAG = "AiStudio"

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

    private val messages = UserMessages()
    val userMessages: Flow<UserMessage> = messages.flow

    private var nextExchangeId = 1L

    val addedItemIds: List<Long>
        get() = _state.value.exchanges.mapNotNull { it.addedItemId }

    init {
        viewModelScope.launch { logFailures("Clearing generated images") { generator.discardAll() } }
    }

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
        if (exchange.addedItemId != null) return
        val previousImage = (exchange.phase as? AiExchangePhase.Result)?.image
        startGenerating(exchangeId)
        viewModelScope.launch {
            if (previousImage != null) {
                logFailures("Discarding the previous image") { generator.discard(previousImage) }
            }
            completeGeneration(exchangeId, exchange.prompt)
        }
    }

    fun addToList(exchangeId: Long, title: String) {
        val exchange = _state.value.exchanges.firstOrNull { it.id == exchangeId } ?: return
        if (exchange.addedItemId != null) return
        val image = (exchange.phase as? AiExchangePhase.Result)?.image ?: return
        viewModelScope.launch {
            var newItemId: Long? = null
            val saved = messages.guard("Adding a generated card") {
                newItemId = saver.save(tierListId, title, image.imageUri)
            }
            if (!saved) return@launch
            _state.update { current ->
                current.copy(
                    exchanges = current.exchanges.map {
                        if (it.id == exchangeId) it.copy(addedItemId = newItemId) else it
                    },
                )
            }
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
            onFailure = { error ->
                Log.w(GENERATION_LOG_TAG, "Image generation failed", error)
                AiExchangePhase.Failed
            },
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
