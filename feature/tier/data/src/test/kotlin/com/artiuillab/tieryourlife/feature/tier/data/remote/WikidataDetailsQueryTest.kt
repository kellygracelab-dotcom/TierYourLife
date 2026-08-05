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

    // Both are OPTIONAL so that an item with neither an image nor a TMDB id still comes back as
    // a row. Without that it would silently vanish from the results the user sees.
    @Test
    fun query_asksForBothPropertiesOptionally() {
        val query = wikidataDetailsQuery(listOf("Q11788"))

        assertEquals(2, Regex("OPTIONAL").findAll(query).count())
    }

    // One malformed token would fail the whole request and take twenty good results with it.
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

    // The Query Service answers with http, and this app has no cleartext-traffic permission, so
    // an un-upgraded URL is one Coil silently refuses to load.
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

    // The service percent-encodes the filename itself. Re-encoding it here is what produced a
    // literal "+" in a URL path and a silent 404 before.
    @Test
    fun thumbnailUrl_leavesTheServicesOwnEncodingAlone() {
        val url = commonsThumbnailUrl(
            "http://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg",
        )

        assertTrue(url.contains("European%20Brown%20Bear.jpg"))
        assertFalse(url.contains("+"))
    }
}
