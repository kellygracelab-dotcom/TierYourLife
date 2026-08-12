package com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageResponseFormatDto(
    @SerialName("aspect_ratio")
    val aspectRatio: String,
    @SerialName("image_size")
    val imageSize: String,
    val type: String = "image",
    @SerialName("mime_type")
    val mimeType: String = "image/jpeg",
)
