package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WikidataSearchResponseDtoTest {

    @Test
    fun captured_wbsearchentities_response_deserializes_label_and_description() {
        val json = """
            {
              "searchinfo": {"search": "медведь"},
              "search": [
                {
                  "id": "Q11788",
                  "title": "Q11788",
                  "pageid": 13615,
                  "repository": "",
                  "url": "//www.wikidata.org/wiki/Q11788",
                  "concepturi": "http://www.wikidata.org/entity/Q11788",
                  "label": "медвежьи",
                  "description": "семейство хищных млекопитающих",
                  "match": {"type": "alias", "language": "ru", "text": "медведь"},
                  "aliases": ["медведь"]
                }
              ],
              "success": 1
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<WikidataSearchResponseDto>(json)

        assertEquals(1, response.search.size)
        assertEquals("Q11788", response.search[0].id)
        assertEquals("медвежьи", response.search[0].label)
        assertEquals("семейство хищных млекопитающих", response.search[0].description)
    }

    @Test
    fun a_hit_with_no_description_maps_it_to_null_not_a_crash() {
        val json = """
            {
              "search": [
                {"id": "Q1", "label": "something"}
              ],
              "success": 1
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<WikidataSearchResponseDto>(json)

        assertNull(response.search[0].description)
    }

    @Test
    fun no_matches_deserializes_to_an_empty_list() {
        val json = """{"searchinfo": {"search": "zzzzxxxxccccvvvv"}, "search": [], "success": 1}"""

        val response = networkJson.decodeFromString<WikidataSearchResponseDto>(json)

        assertTrue(response.search.isEmpty())
    }
}
