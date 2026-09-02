package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.MovieSearchResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDtoMapperTest {

    private fun firstOf(results: String) = networkJson
        .decodeFromString<MovieSearchResponseDto>(
            """{ "page": 1, "results": [$results], "total_pages": 1, "total_results": 1 }""",
        )
        .results[0]
        .toDomain()

    // A whole result as TMDB sends one, so the fields we read stay read even
    // when they arrive surrounded by two dozen we do not.
    private val film = """
        {
          "adult": false,
          "backdrop_path": "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
          "genre_ids": [12, 18, 878],
          "id": 157336,
          "media_type": "movie",
          "original_language": "en",
          "original_title": "Interstellar",
          "overview": "The adventures of a group of explorers.",
          "popularity": 140.241,
          "poster_path": "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
          "release_date": "2014-11-05",
          "title": "Interstellar",
          "video": false,
          "vote_average": 8.4,
          "vote_count": 34521
        }
    """.trimIndent()

    @Test
    fun `a film keeps its title, poster and year`() {
        val item = firstOf(film)!!

        assertEquals("tmdb:157336", item.id)
        assertEquals("Interstellar", item.title)
        assertEquals("https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", item.imageUrl)
        assertEquals("2014", item.subtitle)
    }

    @Test
    fun `a film with no poster still becomes a card`() {
        val item = firstOf("""{ "id": 1, "media_type": "movie", "title": "Undecorated", "poster_path": null }""")!!

        assertEquals("Undecorated", item.title)
        assertNull(item.imageUrl)
    }

    // A series is named `name` and dated `first_air_date`, and nothing else
    // about it differs -- which is the whole reason the search can be one
    // request rather than two.
    @Test
    fun `a series is read from the fields a series uses`() {
        val item = firstOf(
            """
            {
              "id": 1396,
              "media_type": "tv",
              "name": "Breaking Bad",
              "first_air_date": "2008-01-20",
              "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg"
            }
            """.trimIndent(),
        )!!

        assertEquals("Breaking Bad", item.title)
        assertEquals("2008", item.subtitle)
        assertEquals("https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pCfOacjizRGt.jpg", item.imageUrl)
    }

    // Their picture is of them, and lives in a different field again.
    @Test
    fun `a person is shown by their photograph and what they are known for`() {
        val item = firstOf(
            """
            {
              "id": 6193,
              "media_type": "person",
              "name": "Leonardo DiCaprio",
              "known_for_department": "Acting",
              "profile_path": "/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg"
            }
            """.trimIndent(),
        )!!

        assertEquals("Leonardo DiCaprio", item.title)
        assertEquals("Acting", item.subtitle)
        assertEquals("https://image.tmdb.org/t/p/w500/wo2hJpn04vbtmh0B9utCFdsQhxM.jpg", item.imageUrl)
    }

    // A poster path on a person would be their best-known film, which is not
    // what the card says it is.
    @Test
    fun `a person is not illustrated with somebody else's poster`() {
        val item = firstOf(
            """
            {
              "id": 1,
              "media_type": "person",
              "name": "Nobody",
              "poster_path": "/borrowed.jpg"
            }
            """.trimIndent(),
        )!!

        assertNull(item.imageUrl)
    }

    // TMDB's combined search answers with kinds this app has no card for, and
    // a card nobody can name is a blank in a tier list.
    @Test
    fun `a kind we cannot rank is not a card at all`() {
        assertNull(firstOf("""{ "id": 1, "media_type": "collection", "name": "The Trilogy" }"""))
        assertNull(firstOf("""{ "id": 2, "title": "No kind given" }"""))
    }

    @Test
    fun `a nameless result is not a card at all`() {
        assertNull(firstOf("""{ "id": 3, "media_type": "movie", "title": "   " }"""))
        assertNull(firstOf("""{ "id": 4, "media_type": "tv", "poster_path": "/x.jpg" }"""))
    }

    @Test
    fun `a date that is empty or unparseable leaves no year`() {
        assertNull(firstOf("""{ "id": 5, "media_type": "movie", "title": "Undated", "release_date": "" }""")!!.subtitle)
        assertNull(firstOf("""{ "id": 6, "media_type": "movie", "title": "Undated" }""")!!.subtitle)
    }
}
