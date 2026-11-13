package com.artiuillab.tieryourlife.feature.tier.domain.search

import com.artiuillab.tieryourlife.feature.tier.domain.repository.isPageAddress
import com.artiuillab.tieryourlife.feature.tier.domain.repository.linkIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureLinksTextTest {

    @Test
    fun anAddressIsFound_inWhatABrowserShares_andWordsAreNotOne() {
        assertEquals("https://i.example.net/p/8812.jpg", linkIn("  https://i.example.net/p/8812.jpg \n"))
        assertEquals("https://www.ghibli.jp/works/totoro/", linkIn("Totoro https://www.ghibli.jp/works/totoro/"))
        assertEquals("HTTP://example.org/a.png", linkIn("HTTP://example.org/a.png"))
        assertNull(linkIn("totoro poster"))
        assertNull(linkIn("https://"))
    }

    @Test
    fun aPageIsHttps_withoutALoginOrPort_andNotTooLong() {
        assertTrue(isPageAddress("https://www.ghibli.jp/works/totoro/"))
        assertTrue(isPageAddress(" https://ghibli.jp "))
        assertFalse(isPageAddress("ghibli works totoro"))
        assertFalse(isPageAddress("http://ghibli.jp/"))
        assertFalse(isPageAddress("https://me@ghibli.jp/"))
        assertFalse(isPageAddress("https://ghibli.jp:8080/"))
        assertFalse(isPageAddress("https://localhost/"))
        assertFalse(isPageAddress("https://ghibli.jp/" + "a".repeat(500)))
    }
}
