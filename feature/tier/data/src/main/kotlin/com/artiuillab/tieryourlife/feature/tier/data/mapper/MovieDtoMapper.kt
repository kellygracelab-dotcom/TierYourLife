package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.MovieSearchResult

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

internal fun MovieDto.toDomain(): MovieSearchResult = MovieSearchResult(
    tmdbId = id,
    title = title,
    imageUrl = posterPath?.let { "$TMDB_IMAGE_BASE_URL$it" },
    year = releaseDate?.take(4)?.toIntOrNull(),
)
