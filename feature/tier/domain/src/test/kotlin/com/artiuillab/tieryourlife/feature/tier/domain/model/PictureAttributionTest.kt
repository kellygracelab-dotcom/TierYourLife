package com.artiuillab.tieryourlife.feature.tier.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TMDB's notice goes by the attribution; the address says so only for a card made before there was one. */
class PictureAttributionTest {

    @Test
    fun theAttributionDecides_evenAgainstTheAddress() {
        assertTrue(PictureAttribution.isTmdb("https://firebasestorage.googleapis.com/x", PictureAttribution("tmdb")))
        assertFalse(PictureAttribution.isTmdb("https://image.tmdb.org/t/p/w500/a.jpg", PictureAttribution("web")))
    }

    @Test
    fun withoutOne_onlyTmdbsOwnHostCounts() {
        assertTrue(PictureAttribution.isTmdb("https://image.tmdb.org/t/p/w500/a.jpg", null))
        assertFalse(PictureAttribution.isTmdb("https://upload.wikimedia.org/a.jpg", null))
        assertFalse(PictureAttribution.isTmdb("https://image.tmdb.org.example.com/a.jpg", null))
        assertFalse(PictureAttribution.isTmdb(null, null))
    }
}
