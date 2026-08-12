package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InteractionResponseDto(
    val id: String? = null,
    val status: String? = null,
    val steps: List<InteractionStepDto> = emptyList(),
)
