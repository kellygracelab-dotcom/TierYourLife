package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LinkAskDto(val url: String)

/** A catalogue's own address comes back as [imageUrl]; any other picture is kept in the account's folder as [pictureId]. */
@Serializable
data class LinkAnswerDto(
    val pictureId: String? = null,
    val imageUrl: String? = null,
    val attribution: PictureAttributionDto? = null,
    val credit: String? = null,
)

@Serializable
data class LinkRefusalDto(
    val code: String? = null,
    val retryAt: Long? = null,
    val quota: LinkQuotaDto? = null,
)

@Serializable
data class LinkQuotaDto(val resetsAt: Long? = null)
