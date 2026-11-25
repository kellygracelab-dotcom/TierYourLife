package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A result's tile loads a picture its own size, as the site's does, and never a larger one than it was given. */
class SearchPictureTest {

    @Test
    fun aTmdbPoster_comesAtTheFirstWidthThatFillsTheTile() {
        assertEquals("https://image.tmdb.org/t/p/w154/abc.jpg", sizedFor("https://image.tmdb.org/t/p/w500/abc.jpg", 116))
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", sizedFor("https://image.tmdb.org/t/p/w500/abc.jpg", 300))
    }

    @Test
    fun aTmdbThumb_isNeverAskedForLarger() {
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", sizedFor("https://image.tmdb.org/t/p/w342/abc.jpg", 420))
    }

    @Test
    fun aCommonsFile_isDrawnAtWikimediasStep() {
        val file = "https://commons.wikimedia.org/wiki/Special:FilePath/Risotto.jpg"
        assertEquals("$file?width=120", sizedFor("$file?width=400", 116))
        assertEquals("$file?width=330", sizedFor("$file?width=400", 300))
        assertEquals("$file?width=400", sizedFor("$file?width=400", 420))
    }

    @Test
    fun anyOtherAddress_staysAsItIs() {
        assertEquals("https://example.test/pic.jpg", sizedFor("https://example.test/pic.jpg", 120))
    }

    @Test
    fun theSmallCopyGoesFirst_andNoPictureIsNone() {
        assertEquals("https://image.tmdb.org/t/p/w154/a.jpg", pictureFor("https://image.tmdb.org/t/p/w342/a.jpg", "https://image.tmdb.org/t/p/w500/a.jpg", 116))
        assertEquals("https://example.test/big.jpg", pictureFor(null, "https://example.test/big.jpg", 116))
        assertNull(pictureFor(null, " ", 116))
    }

    @Test
    fun theCellWidth_isTheGridsOwn() {
        assertEquals(159f, cellWidth(360.dp).value, 0.01f)
        assertEquals(119.67f, cellWidth(411.dp).value, 0.01f)
    }
}
