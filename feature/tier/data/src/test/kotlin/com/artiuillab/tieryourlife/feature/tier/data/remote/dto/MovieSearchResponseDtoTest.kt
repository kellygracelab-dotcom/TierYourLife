package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieSearchResponseDtoTest {

    @Test
    fun full_tmdb_response_deserializes_into_dto_and_maps_poster_path() {

        val json = """
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

        val response = networkJson.decodeFromString<MovieSearchResponseDto>(json)

        assertEquals(1, response.page)
        assertEquals("/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", response.results[0].posterPath)
    }
}
