package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

data class AiStudioUiState(
    val exchanges: List<AiExchange> = emptyList(),
    val generating: Boolean = false,
)

data class AiExchange(
    val id: Long,
    val prompt: String,
    val phase: AiExchangePhase,
    val addedItemId: Long? = null,
)

sealed interface AiExchangePhase {
    data object Generating : AiExchangePhase
    data class Result(val image: GeneratedCardImage) : AiExchangePhase
    data object Failed : AiExchangePhase
}
