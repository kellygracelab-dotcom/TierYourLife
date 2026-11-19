package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadsDto(
    val used: Int = 0,
    val left: Int = 0,
    val limit: Int = 0,
    val guest: Boolean = false,
    val warnAt: Int? = null,
    val resetsAt: Long = 0,
)

@Serializable
data class UploadsAnswerDto(val uploads: UploadsDto? = null)

@Serializable
data class UploadsRefusalDto(val code: String? = null, val retryAt: Long? = null, val uploads: UploadsDto? = null)
