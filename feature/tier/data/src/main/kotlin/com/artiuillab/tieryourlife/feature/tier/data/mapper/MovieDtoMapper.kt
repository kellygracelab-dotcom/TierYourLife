package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

internal fun MovieDto.toDomain(): TierItem = TierItem(
    id = 0,
    title = title,
    imageUrl = posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" },
)
