package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InteractionRequestDto(
    val model: String,
    val input: List<InteractionInputDto>,
    @SerialName("response_format")
    val responseFormat: ImageResponseFormatDto,
)
