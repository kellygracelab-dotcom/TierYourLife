package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeminiErrorDto(
    val code: String? = null,
    val message: String? = null,
)
