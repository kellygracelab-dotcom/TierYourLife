package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDtoMapperTest {

    private val jsonWithNotNullPoster = """
        {
          "page": 1,
          "results": [
            {
              "adult": false,
              "backdrop_path": "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
              "genre_ids": [12, 18, 878],
              "id": 157336,
              "original_language": "en",
              "original_title": "Interstellar",
              "overview": "The adventures of a group of explorers who make use of a newly discovered wormhole.",
              "popularity": 140.241,
              "poster_path": "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
              "title": "Interstellar",
              "video": false,
              "vote_average": 8.4,
              "vote_count": 34521
            }
          ],
          "total_pages": 1,
          "total_results": 1
        }
    """.trimIndent()

    private val jsonWithNullPoster = """
        {
          "page": 1,
          "results": [
            {
              "adult": false,
              "backdrop_path": "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
              "genre_ids": [12, 18, 878],
              "id": 157336,
              "original_language": "en",
              "original_title": "Interstellar",
              "overview": "The adventures of a group of explorers who make use of a newly discovered wormhole.",
              "popularity": 140.241,
              "poster_path": null,
              "title": "Interstellar",
              "video": false,
              "vote_average": 8.4,
              "vote_count": 34521
            }
          ],
          "total_pages": 1,
          "total_results": 1
        }
    """.trimIndent()

    private val jsonWithReleaseDate = """
        {
          "page": 1,
          "results": [
            {
              "adult": false,
              "backdrop_path": "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
              "genre_ids": [12, 18, 878],
              "id": 157336,
              "original_language": "en",
              "original_title": "Interstellar",
              "overview": "The adventures of a group of explorers who make use of a newly discovered wormhole.",
              "popularity": 140.241,
              "poster_path": null,
              "release_date": "2014-11-05",
              "title": "Interstellar",
              "video": false,
              "vote_average": 8.4,
              "vote_count": 34521
            }
          ],
          "total_pages": 1,
          "total_results": 1
        }
    """.trimIndent()

    private val jsonWithBlankReleaseDate = """
        {
          "page": 1,
          "results": [
            {
              "adult": false,
              "backdrop_path": "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
              "genre_ids": [12, 18, 878],
              "id": 157336,
              "original_language": "en",
              "original_title": "Interstellar",
              "overview": "The adventures of a group of explorers who make use of a newly discovered wormhole.",
              "popularity": 140.241,
              "poster_path": null,
              "release_date": "",
              "title": "Interstellar",
              "video": false,
              "vote_average": 8.4,
              "vote_count": 34521
            }
          ],
          "total_pages": 1,
          "total_results": 1
        }
    """.trimIndent()

    @Test
    fun maps_full_poster_url_when_poster_path_present() {
        val response = networkJson.decodeFromString<MovieSearchResponseDto>(jsonWithNotNullPoster)
        val item = response.results[0].toDomain()

        assertEquals(157336L, item.tmdbId)
        assertEquals("Interstellar", item.title)
        assertEquals("https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", item.imageUrl)
    }

    @Test
    fun maps_null_image_url_when_poster_path_null() {
        val response = networkJson.decodeFromString<MovieSearchResponseDto>(jsonWithNullPoster)
        val item = response.results[0].toDomain()

        assertEquals(157336L, item.tmdbId)
        assertEquals("Interstellar", item.title)
        assertNull(item.imageUrl)
    }

    @Test
    fun maps_year_from_the_leading_four_digits_of_release_date() {
        val response = networkJson.decodeFromString<MovieSearchResponseDto>(jsonWithReleaseDate)
        val item = response.results[0].toDomain()

        assertEquals(2014, item.year)
    }

    @Test
    fun maps_null_year_when_release_date_is_absent() {
        val response = networkJson.decodeFromString<MovieSearchResponseDto>(jsonWithNotNullPoster)
        val item = response.results[0].toDomain()

        assertNull(item.year)
    }

    @Test
    fun maps_null_year_when_release_date_is_blank() {
        val response = networkJson.decodeFromString<MovieSearchResponseDto>(jsonWithBlankReleaseDate)
        val item = response.results[0].toDomain()

        assertNull(item.year)
    }
}
