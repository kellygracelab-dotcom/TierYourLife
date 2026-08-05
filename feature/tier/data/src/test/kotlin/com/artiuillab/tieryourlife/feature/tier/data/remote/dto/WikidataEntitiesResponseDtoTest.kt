package com.artiuillab.tieryourlife.feature.tier.data.remote.dto

import com.artiuillab.tieryourlife.feature.tier.data.remote.networkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikidataEntitiesResponseDtoTest {

    @Test
    fun captured_wbgetentities_response_reads_the_image_filename_off_P18() {
        val json = """
            {
              "entities": {
                "Q11788": {
                  "pageid": 13615,
                  "ns": 0,
                  "title": "Q11788",
                  "lastrevid": 123456789,
                  "modified": "2024-01-01T00:00:00Z",
                  "type": "item",
                  "id": "Q11788",
                  "claims": {
                    "P18": [
                      {
                        "mainsnak": {
                          "snaktype": "value",
                          "property": "P18",
                          "hash": "abc123",
                          "datavalue": {
                            "value": "European Brown Bear.jpg",
                            "type": "string"
                          },
                          "datatype": "commonsMedia"
                        },
                        "type": "statement",
                        "id": "Q11788${'$'}abc-def",
                        "rank": "normal"
                      }
                    ]
                  }
                }
              },
              "success": 1
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<WikidataEntitiesResponseDto>(json)
        val entity = response.entities.getValue("Q11788")

        assertEquals("European Brown Bear.jpg", entity.firstClaimValue("P18"))
    }

    @Test
    fun an_entity_with_both_P18_and_P4947_reads_each_independently() {
        val json = """
            {
              "entities": {
                "Q206529": {
                  "id": "Q206529",
                  "claims": {
                    "P18": [{"mainsnak": {"datavalue": {"value": "Interstellar film poster.jpg", "type": "string"}}}],
                    "P4947": [{"mainsnak": {"datavalue": {"value": "157336", "type": "string"}}}]
                  }
                }
              }
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<WikidataEntitiesResponseDto>(json)
        val entity = response.entities.getValue("Q206529")

        assertEquals("Interstellar film poster.jpg", entity.firstClaimValue("P18"))
        assertEquals("157336", entity.firstClaimValue("P4947"))
    }

    @Test
    fun an_entity_with_no_P18_claim_at_all_maps_the_image_to_null_not_a_crash() {
        val json = """
            {
              "entities": {
                "Q1": {
                  "id": "Q1",
                  "claims": {
                    "P31": [{"mainsnak": {"datavalue": {"value": "Q123", "type": "wikibase-entityid"}}}]
                  }
                }
              }
            }
        """.trimIndent()

        val response = networkJson.decodeFromString<WikidataEntitiesResponseDto>(json)
        val entity = response.entities.getValue("Q1")

        assertNull(entity.firstClaimValue("P18"))
        assertNull(entity.firstClaimValue("P4947"))
    }

    @Test
    fun an_entity_with_no_claims_object_at_all_still_maps_safely() {
        val json = """{"entities": {"Q1": {"id": "Q1"}}}"""

        val response = networkJson.decodeFromString<WikidataEntitiesResponseDto>(json)
        val entity = response.entities.getValue("Q1")

        assertNull(entity.firstClaimValue("P18"))
    }
}
