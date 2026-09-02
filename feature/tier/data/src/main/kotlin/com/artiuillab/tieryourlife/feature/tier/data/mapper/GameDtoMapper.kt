package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.GameDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

private const val IGDB_ID_PREFIX = "igdb:"

internal fun GameDto.toDomain(): CatalogueItem = CatalogueItem(
    id = "$IGDB_ID_PREFIX$id",
    title = name,
    subtitle = year?.toString(),
    imageUrl = imageUrl,
)
