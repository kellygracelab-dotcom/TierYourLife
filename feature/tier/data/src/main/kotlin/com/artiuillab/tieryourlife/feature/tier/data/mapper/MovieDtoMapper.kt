package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"
private const val TMDB_ID_PREFIX = "tmdb:"
private const val TMDB_PAGE_BASE_URL = "https://www.themoviedb.org"

private const val MEDIA_MOVIE = "movie"
private const val MEDIA_TV = "tv"
private const val MEDIA_PERSON = "person"

/** TMDB's combined search also answers with kinds this app has no card for, and rows with no name; neither becomes a card. */
internal fun MovieDto.toDomain(): CatalogueItem? {
    val name = when (mediaType) {
        MEDIA_MOVIE -> title
        MEDIA_TV, MEDIA_PERSON -> this.name
        else -> return null
    }?.trim()?.ifBlank { null } ?: return null

    // A person's picture is of them; anything else without a poster gets
    // a still, which beats an empty frame.
    val imageUrl = (if (mediaType == MEDIA_PERSON) profilePath else posterPath ?: backdropPath)
        ?.let { "$TMDB_IMAGE_BASE_URL$it" }
    return CatalogueItem(
        id = "$TMDB_ID_PREFIX$id",
        title = name,
        subtitle = subtitle(),
        imageUrl = imageUrl,
        // The page the server keeps a link to, in its one shape.
        attribution = imageUrl?.let { PictureAttribution(PictureAttribution.TMDB, link = "$TMDB_PAGE_BASE_URL/$mediaType/$id", id = "tmdb:$mediaType:$id") },
    )
}

/** The year, or for a person the department: two people share a name often enough that a bare name is a guess. */
private fun MovieDto.subtitle(): String? = when (mediaType) {
    MEDIA_PERSON -> knownForDepartment?.trim()?.ifBlank { null }
    MEDIA_TV -> firstAirDate.year()
    else -> releaseDate.year()
}

private fun String?.year(): String? = this?.take(4)?.toIntOrNull()?.toString()
