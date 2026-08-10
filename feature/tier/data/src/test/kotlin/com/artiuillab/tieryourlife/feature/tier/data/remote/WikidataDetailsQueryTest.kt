package com.artiuillab.tieryourlife.feature.tier.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikidataDetailsQueryTest {

    @Test
    fun query_asksForBothPropertiesForEveryId() {
        val query = wikidataDetailsQuery(listOf("Q11788", "Q243359"))

        assertTrue(query.contains("wd:Q11788"))
        assertTrue(query.contains("wd:Q243359"))
        assertTrue(query.contains("wdt:P18"))
        assertTrue(query.contains("wdt:P4947"))
    }

    @Test
    fun query_asksForBothPropertiesOptionally() {
        val query = wikidataDetailsQuery(listOf("Q11788"))

        assertEquals(2, Regex("OPTIONAL").findAll(query).count())
    }

    @Test
    fun query_dropsAnythingThatIsNotAQid() {
        val query = wikidataDetailsQuery(listOf("Q11788", "P31", "} DROP ALL {", "Q1"))

        assertTrue(query.contains("wd:Q11788"))
        assertTrue(query.contains("wd:Q1 "))
        assertFalse(query.contains("P31"))
        assertFalse(query.contains("DROP"))
    }

    @Test
    fun entityUri_reducesToItsQid() {
        assertEquals("Q11788", qidFromEntityUri("http://www.wikidata.org/entity/Q11788"))
    }

    @Test
    fun thumbnailUrl_upgradesHttpToHttps() {
        val url = commonsThumbnailUrl("http://commons.wikimedia.org/wiki/Special:FilePath/Bear.jpg")

        assertTrue(url.startsWith("https://"))
        assertFalse(url.contains("http://"))
    }

    @Test
    fun thumbnailUrl_asksForAWidthRatherThanTheFullSizeOriginal() {
        val url = commonsThumbnailUrl("https://commons.wikimedia.org/wiki/Special:FilePath/Bear.jpg")

        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Bear.jpg?width=500", url)
    }

    @Test
    fun thumbnailUrl_leavesTheServicesOwnEncodingAlone() {
        val url = commonsThumbnailUrl(
            "http://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg",
        )

        assertTrue(url.contains("European%20Brown%20Bear.jpg"))
        assertFalse(url.contains("+"))
    }
}
