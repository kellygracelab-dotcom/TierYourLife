package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InteractionStepDto(
    val type: String? = null,
    val content: List<InteractionContentDto> = emptyList(),
)
