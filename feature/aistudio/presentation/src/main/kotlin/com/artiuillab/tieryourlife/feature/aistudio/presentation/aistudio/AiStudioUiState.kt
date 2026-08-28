package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

data class AiStudioUiState(
    val exchanges: List<AiExchange> = emptyList(),
    val generating: Boolean = false,
    /** Generations left, or null when nothing is counted — the local stub build. */
    val credits: Int? = null,
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

    /** Something went wrong on the way. Trying again is worth it. */
    data object Failed : AiExchangePhase

    /** Nothing left to spend. Trying again would fail the same way. */
    data object OutOfCredits : AiExchangePhase
}
