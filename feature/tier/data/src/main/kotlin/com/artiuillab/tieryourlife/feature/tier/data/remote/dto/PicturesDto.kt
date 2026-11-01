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
        )
    },
    hasMore = hasMore,
    // Codes this build does not know are for a newer one.
    notice = notices.firstNotNullOfOrNull { code ->
        when (code) {
            "webDailyLimit" -> SearchNotice.WebDailyLimit
            "webOff" -> SearchNotice.WebOff
            else -> null
        }
    },
)
