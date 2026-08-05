package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataClaimDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataDataValueDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataEntityDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataMainSnakDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.WikidataSearchItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WikidataMapperTest {

    private fun claim(value: String) = WikidataClaimDto(
        mainsnak = WikidataMainSnakDto(datavalue = WikidataDataValueDto(value = value)),
    )

    @Test
    fun `id gets the wikidata prefix and label becomes the title`() {
        val searchItem = WikidataSearchItemDto(id = "Q11788", label = "медвежьи", description = "семейство хищных млекопитающих")

        val candidate = searchItem.toDomain(entity = null)

        assertEquals("wikidata:Q11788", candidate.item.id)
        assertEquals("медвежьи", candidate.item.title)
        assertEquals("семейство хищных млекопитающих", candidate.item.subtitle)
    }

    @Test
    fun `falls back to the raw id as the title when the search hit has no label`() {
        val searchItem = WikidataSearchItemDto(id = "Q11788", label = null, description = null)

        val candidate = searchItem.toDomain(entity = null)

        assertEquals("Q11788", candidate.item.title)
    }

    @Test
    fun `a missing entity (no claims call result) maps to a null image, not a crash`() {
        val searchItem = WikidataSearchItemDto(id = "Q11788", label = "медвежьи")

        val candidate = searchItem.toDomain(entity = null)

        assertNull(candidate.item.imageUrl)
        assertNull(candidate.linkedTmdbId)
    }

    @Test
    fun `an entity with no P18 claim at all maps to a null image, not a crash`() {
        val searchItem = WikidataSearchItemDto(id = "Q11788", label = "медвежьи")
        val entity = WikidataEntityDto(claims = emptyMap())

        val candidate = searchItem.toDomain(entity)

        assertNull(candidate.item.imageUrl)
    }

    @Test
    fun `P18 becomes a Commons FilePath url, not a bare filename`() {
        val searchItem = WikidataSearchItemDto(id = "Q11788", label = "медвежьи")
        val entity = WikidataEntityDto(claims = mapOf("P18" to listOf(claim("European Brown Bear.jpg"))))

        val candidate = searchItem.toDomain(entity)

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg?width=500",
            candidate.item.imageUrl,
        )
    }

    @Test
    fun `P4947 becomes the linked tmdb id as a Long`() {
        val searchItem = WikidataSearchItemDto(id = "Q206529", label = "Interstellar")
        val entity = WikidataEntityDto(claims = mapOf("P4947" to listOf(claim("157336"))))

        val candidate = searchItem.toDomain(entity)

        assertEquals(157336L, candidate.linkedTmdbId)
    }

    @Test
    fun `a non numeric P4947 value maps to a null linked id rather than crashing`() {
        val searchItem = WikidataSearchItemDto(id = "Q1", label = "Odd")
        val entity = WikidataEntityDto(claims = mapOf("P4947" to listOf(claim("not-a-number"))))

        val candidate = searchItem.toDomain(entity)

        assertNull(candidate.linkedTmdbId)
    }
}
