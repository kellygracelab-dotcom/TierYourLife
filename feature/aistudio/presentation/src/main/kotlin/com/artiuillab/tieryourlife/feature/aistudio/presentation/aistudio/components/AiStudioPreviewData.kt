package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiExchange
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiExchangePhase
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioUiState

internal val previewAiStudioEmptyState = AiStudioUiState()

internal val previewAiStudioConversationState = AiStudioUiState(
    credits = 7,
    exchanges = listOf(
        AiExchange(
            id = 1,
            prompt = "A neon-lit Tokyo street in the rain",
            phase = AiExchangePhase.Result(
                GeneratedCardImage(prompt = "A neon-lit Tokyo street in the rain", imageUri = ""),
            ),
        ),
    ),
)

internal val previewAiStudioGeneratingState = AiStudioUiState(
    exchanges = listOf(
        AiExchange(
            id = 1,
            prompt = "A lone figure on a red desert planet",
            phase = AiExchangePhase.Generating,
        ),
    ),
    generating = true,
)

internal val previewAiStudioFailedState = AiStudioUiState(
    exchanges = listOf(
        AiExchange(
            id = 1,
            prompt = "A retro VHS cover with bold type",
            phase = AiExchangePhase.Failed,
        ),
    ),
)

internal val previewAiStudioOutOfCreditsState = AiStudioUiState(
    exchanges = listOf(
        AiExchange(
            id = 1,
            prompt = "A quiet library at midnight",
            phase = AiExchangePhase.OutOfCredits,
        ),
    ),
    credits = 0,
)
