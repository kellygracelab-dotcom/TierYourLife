package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A house list's cover credit as the editor wrote it by hand (Proxy #119). */
class CoverCreditTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun opened(cover: String, credit: String) = json.decodeFromString<PublishedListDto>(
        """{"id":"h1","title":"Wildlife","authorName":"TierYourLife","coverImageUrl":$cover,"coverAttribution":$credit}""",
    ).toDomain()

    @Test
    fun `the editor's credit comes with the list as a picture of a web page`() {
        val list = opened(
            "\"https://example.org/fox.jpg\"",
            """{"author":"Jane Doe","license":"CC BY 4.0","licenseUrl":"https://creativecommons.org/licenses/by/4.0/","link":"https://example.com/fox","manual":true}""",
        )

        assertEquals(
            PictureAttribution(
                source = "web",
                author = "Jane Doe",
                license = "CC BY 4.0",
                licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
                link = "https://example.com/fox",
            ),
            list.coverAttribution,
        )
    }

    @Test
    fun `a credit without an author is still one`() {
        val list = opened("\"https://example.org/fox.jpg\"", """{"license":"Used with permission","link":"https://example.com/fox","manual":true}""")

        assertEquals("Used with permission", list.coverAttribution?.license)
        assertNull(list.coverAttribution?.author)
    }

    @Test
    fun `no cover picture, nothing to credit`() {
        val list = opened("\"null\"", """{"license":"CC0 1.0","link":"https://example.com/fox","manual":true}""")

        assertNull(list.coverAttribution)
    }

    @Test
    fun `a list without one has none`() {
        assertNull(PublishedListDto(id = "a", title = "Films", authorName = "A", coverImageUrl = "https://i/c.jpg").toDomain().coverAttribution)
    }
}
