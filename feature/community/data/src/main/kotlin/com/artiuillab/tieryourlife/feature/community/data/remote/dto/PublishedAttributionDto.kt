package com.artiuillab.tieryourlife.feature.community.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.serialization.Serializable

/** Proxy CONTRACT.md, PictureAttribution, on a published card. */
@Serializable
data class PublishedAttributionDto(
    val source: String,
    val author: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val link: String? = null,
    val originalUrl: String? = null,
    val id: String? = null,
    val formerUrls: List<String> = emptyList(),
)

fun PublishedAttributionDto.toDomain() = PictureAttribution(source, author, license, licenseUrl, link, originalUrl, id, formerUrls)

fun PictureAttribution.toDto() = PublishedAttributionDto(source, author, license, licenseUrl, link, originalUrl, id, formerUrls)
