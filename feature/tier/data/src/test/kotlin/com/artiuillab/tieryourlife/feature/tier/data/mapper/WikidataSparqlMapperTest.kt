package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSparqlResponseDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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

    // A club, a company, a competition: no photograph of the thing itself
    // exists, and the badge is the only picture there is.
    @Test
    fun binding_withOnlyALogo_usesTheLogo() {
        val details = parse(
            """
            {"results":{"bindings":[
              {"item":{"value":"http://www.wikidata.org/entity/Q476"},
               "logo":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Badge.svg"}}
            ]}}
            """.trimIndent(),
        ).toDetailsByQid()

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Badge.svg?width=500",
            details.getValue("Q476").imageUrl,
        )
    }

    // Something can have both. A photograph of the building says more than
    // the wordmark above its door.
    @Test
    fun binding_withBoth_prefersThePhotograph() {
        val details = parse(
            """
            {"results":{"bindings":[
              {"item":{"value":"http://www.wikidata.org/entity/Q95"},
               "image":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Office.jpg"},
               "logo":{"value":"http://commons.wikimedia.org/wiki/Special:FilePath/Wordmark.svg"}}
            ]}}
            """.trimIndent(),
        ).toDetailsByQid()

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Office.jpg?width=500",
            details.getValue("Q95").imageUrl,
        )
    }

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
