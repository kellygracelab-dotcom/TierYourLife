package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InteractionContentDto(
    val type: String? = null,
    val text: String? = null,
    val data: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
)
