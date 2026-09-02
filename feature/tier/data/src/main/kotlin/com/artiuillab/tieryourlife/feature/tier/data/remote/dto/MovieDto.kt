package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One result from TMDB's combined search, which answers with films, series
 * and people together.
 *
 * Three shapes in one, so nearly everything is optional: a film has a `title`
 * and a poster, a series has a `name` and a poster, a person has a `name` and
 * a photograph in a different field again. `mediaType` says which was meant.
 */
@Serializable
data class MovieDto(
    val id: Long,
    @SerialName("media_type")
    val mediaType: String? = null,
    /** Films. */
    val title: String? = null,
    /** Series and people. */
    val name: String? = null,
    @SerialName("poster_path")
    val posterPath: String? = null,
    /** People, whose picture is of them rather than of a poster. */
    @SerialName("profile_path")
    val profilePath: String? = null,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null,
    /** What a person is known for: "Acting", "Directing", and so on. */
    @SerialName("known_for_department")
    val knownForDepartment: String? = null,
)
