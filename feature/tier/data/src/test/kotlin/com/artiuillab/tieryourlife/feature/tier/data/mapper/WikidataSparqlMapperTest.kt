package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// The JSON here is the real shape returned by query.wikidata.org, captured from a live request.
class WikidataSparqlMapperTest {

    private fun parse(json: String): WikidataSparqlResponseDto =
        networkJson.decodeFromString(WikidataSparqlResponseDto.serializer(), json)

    @Test
    fun binding_withAnImage_becomesAnHttpsThumbnailUrl() {
        val details = parse(
            """
            {"results":{"bindings":[
              {"item":{"value":"http://www.wikidata.org/entity/Q11788"},
               "image":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg"}}
            ]}}
            """.trimIndent(),
        ).toDetailsByQid()

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg?width=500",
            details.getValue("Q11788").imageUrl,
        )
        assertNull(details.getValue("Q11788").linkedTmdbId)
    }

    // Most items have no picture. That must be an item without a picture, not a failure.
    @Test
    fun binding_withNoImage_leavesTheUrlNull() {
        val details = parse(
            """{"results":{"bindings":[{"item":{"value":"http://www.wikidata.org/entity/Q1"}}]}}""",
        ).toDetailsByQid()

        assertNull(details.getValue("Q1").imageUrl)
    }

    @Test
    fun binding_withATmdbId_carriesItForDeduplication() {
        val details = parse(
            """
            {"results":{"bindings":[
              {"item":{"value":"http://www.wikidata.org/entity/Q13417189"},
               "tmdb":{"value":"157336"}}
            ]}}
            """.trimIndent(),
        ).toDetailsByQid()

        assertEquals(157336L, details.getValue("Q13417189").linkedTmdbId)
    }

    // An item with several images produces several rows for the same entity.
    @Test
    fun severalRowsForOneItem_keepTheFirstImage() {
        val details = parse(
            """
            {"results":{"bindings":[
              {"item":{"value":"http://www.wikidata.org/entity/Q11788"},
               "image":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/First.jpg"}},
              {"item":{"value":"http://www.wikidata.org/entity/Q11788"},
               "image":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Second.jpg"}}
            ]}}
            """.trimIndent(),
        ).toDetailsByQid()

        assertEquals(1, details.size)
        assertEquals(true, details.getValue("Q11788").imageUrl?.contains("First.jpg"))
    }

    @Test
    fun emptyResults_produceNoDetails() {
        assertEquals(emptyMap<String, WikidataItemDetails>(), parse("""{"results":{"bindings":[]}}""").toDetailsByQid())
    }
}
