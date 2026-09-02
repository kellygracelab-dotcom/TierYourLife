package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
private const val TMDB_ID_PREFIX = "tmdb:"

private const val MEDIA_MOVIE = "movie"
private const val MEDIA_TV = "tv"
private const val MEDIA_PERSON = "person"

/**
 * A card, or nothing.
 *
 * TMDB's combined search also answers with kinds this app has no card for,
 * and with rows whose name is missing altogether. Neither can be ranked, so
 * neither becomes a card -- a blank in a tier list is worse than an absence.
 */
internal fun MovieDto.toDomain(): CatalogueItem? {
    val name = when (mediaType) {
        MEDIA_MOVIE -> title
        MEDIA_TV, MEDIA_PERSON -> this.name
        else -> return null
    }?.trim()?.ifBlank { null } ?: return null

    return CatalogueItem(
        id = "$TMDB_ID_PREFIX$id",
        title = name,
        subtitle = subtitle(),
        // A person's picture is of them; everything else has a poster.
        imageUrl = (if (mediaType == MEDIA_PERSON) profilePath else posterPath)
            ?.let { "$TMDB_IMAGE_BASE_URL$it" },
    )
}

/**
 * The year, or for a person what they are known for.
 *
 * Two people share a name often enough that a bare name is a guess, and the
 * department is the shortest thing that tells them apart.
 */
private fun MovieDto.subtitle(): String? = when (mediaType) {
    MEDIA_PERSON -> knownForDepartment?.trim()?.ifBlank { null }
    MEDIA_TV -> firstAirDate.year()
    else -> releaseDate.year()
}

private fun String?.year(): String? = this?.take(4)?.toIntOrNull()?.toString()
