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

/**
 * The house editor's credit for a list's cover: written by hand, or a
 * catalogue's (TMDB, IGDB and the like) named by its [source], as on a topic.
 */
@Serializable
data class CoverAttributionDto(
    val source: String? = null,
    val author: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val link: String? = null,
)

fun CoverAttributionDto.toDomain() = PictureAttribution(
    source = source?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "web",
    author = author,
    license = license,
    licenseUrl = licenseUrl,
    link = link,
)

fun PictureAttribution.toDto() = PublishedAttributionDto(source, author, license, licenseUrl, link, originalUrl, id, formerUrls)
