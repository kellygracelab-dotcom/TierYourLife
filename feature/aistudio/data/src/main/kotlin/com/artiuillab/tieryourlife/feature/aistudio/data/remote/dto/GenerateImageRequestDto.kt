package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenerateImageRequestDto(
    val prompt: String,
)
