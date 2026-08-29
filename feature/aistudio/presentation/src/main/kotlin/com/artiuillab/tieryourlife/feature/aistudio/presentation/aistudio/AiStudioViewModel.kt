package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.artiuillab.tieryourlife.core.ui.UserMessage
import com.artiuillab.tieryourlife.core.ui.UserMessages
import com.artiuillab.tieryourlife.core.ui.guard
import com.artiuillab.tieryourlife.core.ui.logFailures
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.CardImageGenerator
import com.artiuillab.tieryourlife.feature.aistudio.domain.generation.GenerationOutcome
import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.aistudio.presentation.navigation.AiStudioRoute
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
class AiStudioViewModel @Inject constructor(
    private val generator: CardImageGenerator,
    private val credits: GenerationCredits,
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
        refreshCredits()
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
        val outcome = runCatching { generator.generate(prompt) }
            .onFailure { error -> Timber.w(error, "Image generation failed") }
            .getOrDefault(GenerationOutcome.Failed)

        _state.update { current ->
            val phase = when (outcome) {
                is GenerationOutcome.Success -> AiExchangePhase.Result(outcome.image)
                GenerationOutcome.OutOfCredits -> AiExchangePhase.OutOfCredits
                GenerationOutcome.Failed -> AiExchangePhase.Failed
            }
            current.withPhase(exchangeId, phase).copy(
                generating = false,
                credits = creditsAfter(outcome, current.credits),
            )
        }
    }

    /**
     * A successful generation answers with the balance it left, so the count
     * stays right without a second call. A refusal means empty by definition.
     * A failure changes nothing: the backend gave the credit back.
     */
    private fun creditsAfter(outcome: GenerationOutcome, current: Int?): Int? = when (outcome) {
        is GenerationOutcome.Success -> outcome.creditsRemaining ?: current
        GenerationOutcome.OutOfCredits -> 0
        GenerationOutcome.Failed -> current
    }

    private fun refreshCredits() {
        viewModelScope.launch {
            val remaining = runCatching { credits.remaining() }.getOrNull()
            if (remaining != null) {
                _state.update { it.copy(credits = remaining) }
            }
        }
    }

    private fun AiStudioUiState.withPhase(exchangeId: Long, phase: AiExchangePhase): AiStudioUiState =
        copy(
            exchanges = exchanges.map { exchange ->
                if (exchange.id == exchangeId) exchange.copy(phase = phase) else exchange
            },
        )
}
