package com.artiuillab.tieryourlife.feature.tier.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CommonsImageUrlTest {

    @Test
    fun `a filename with spaces produces percent-20, not plus`() {
        val url = commonsFilePathUrl("European Brown Bear.jpg")

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/European%20Brown%20Bear.jpg?width=500",
            url,
        )
        assertFalse(url.contains("+"))
    }

    @Test
    fun `a filename with no special characters passes through unchanged`() {
        val url = commonsFilePathUrl("Interstellar.jpg")

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/Interstellar.jpg?width=500",
            url,
        )
    }

    @Test
    fun `a filename with non-ASCII characters is percent-encoded`() {
        val url = commonsFilePathUrl("Медведь бурый.jpg")

        assertEquals(
            "https://commons.wikimedia.org/wiki/Special:FilePath/" +
                "%D0%9C%D0%B5%D0%B4%D0%B2%D0%B5%D0%B4%D1%8C%20%D0%B1%D1%83%D1%80%D1%8B%D0%B9.jpg?width=500",
            url,
        )
    }
}
