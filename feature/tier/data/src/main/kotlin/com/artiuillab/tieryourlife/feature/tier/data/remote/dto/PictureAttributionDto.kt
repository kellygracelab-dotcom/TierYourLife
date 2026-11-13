package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.serialization.Serializable

/** Proxy CONTRACT.md, PictureAttribution: on a search hit and on each card of a kept board. */
@Serializable
data class PictureAttributionDto(
    val source: String,
    val author: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val link: String? = null,
    val originalUrl: String? = null,
    val id: String? = null,
    val formerUrls: List<String> = emptyList(),
)

fun PictureAttributionDto.toDomain() = PictureAttribution(source, author, license, licenseUrl, link, originalUrl, id, formerUrls)

fun PictureAttribution.toDto() = PictureAttributionDto(source, author, license, licenseUrl, link, originalUrl, id, formerUrls)
