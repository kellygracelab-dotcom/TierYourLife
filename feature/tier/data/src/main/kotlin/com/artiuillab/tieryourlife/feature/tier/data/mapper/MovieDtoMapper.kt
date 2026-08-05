package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
private const val TMDB_ID_PREFIX = "tmdb:"

internal fun MovieDto.toDomain(): CatalogueItem = CatalogueItem(
    id = "$TMDB_ID_PREFIX$id",
    title = title,
    subtitle = releaseDate?.take(4)?.toIntOrNull()?.toString(),
    imageUrl = posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" },
)
