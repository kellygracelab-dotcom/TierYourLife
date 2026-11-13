package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import kotlinx.serialization.Serializable

@Serializable
data class PicturesDto(
    val items: List<PictureHitDto> = emptyList(),
    val hasMore: Boolean = false,
    val notices: List<String> = emptyList(),
    val rest: Boolean = false,
)

@Serializable
data class PictureHitDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String,
    val thumbUrl: String? = null,
    val source: String = "",
    val kind: String = "",
    /** Who the picture's license asks to be named; absent when it asks for nobody. */
    val credit: String? = null,
    val attribution: PictureAttributionDto? = null,
)

/** The server's 429 for a person who searched too often; [retryAt] is when it answers again, epoch ms. */
@Serializable
data class SearchRefusalDto(
    val code: String? = null,
    val retryAt: Long? = null,
)

private const val WEB_SOURCE = "web"

fun PicturesDto.toDomain(): CatalogueSearchPage = CatalogueSearchPage(
    items = items.map { hit ->
        CatalogueItem(
            id = hit.id,
            title = hit.title,
            subtitle = hit.subtitle,
            imageUrl = hit.imageUrl,
            thumbUrl = hit.thumbUrl,
            fromWeb = hit.source == WEB_SOURCE,
            credit = hit.credit?.trim()?.takeIf { it.isNotEmpty() },
            attribution = hit.attribution?.toDomain(),
        )
    },
    hasMore = hasMore,
    rest = rest,
    // Codes this build does not know are for a newer one.
    notice = notices.firstNotNullOfOrNull { code ->
        when (code) {
            "webDailyLimit" -> SearchNotice.WebDailyLimit
            "webOff" -> SearchNotice.WebOff
            else -> null
        }
    },
)
