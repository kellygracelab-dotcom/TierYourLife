package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.domain.model.ListSource
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A house list's source (Proxy #141): one line of at most 120, an https address of at most 500. */
class ListSourceTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun opened(source: String) = json.decodeFromString<PublishedListDto>(
        """{"id":"h1","title":"Fruit","authorName":"","authorUid":"tyl-editorial","anonymous":true,"listSource":$source}""",
    ).toDomain().source

    @Test
    fun `the editor's source comes with the list`() {
        assertEquals(
            ListSource("Nix's fruit stream", "https://www.twitch.tv/videos/123456789"),
            opened("""{"label":"Nix's fruit stream","url":"https://www.twitch.tv/videos/123456789"}"""),
        )
    }

    @Test
    fun `an address that is not https, or has no host, opens nothing`() {
        assertNull(opened("""{"label":"Stream","url":"http://www.twitch.tv/videos/1"}"""))
        assertNull(opened("""{"label":"Stream","url":"javascript:alert(1)"}"""))
        assertNull(opened("""{"label":"Stream","url":"https://"}"""))
    }

    @Test
    fun `a label past the server's limits is no source`() {
        assertNull(opened("""{"label":"Two\nlines","url":"https://example.com"}"""))
        assertNull(opened("""{"label":"   ","url":"https://example.com"}"""))
        assertNull(opened("""{"label":"${"x".repeat(121)}","url":"https://example.com"}"""))
        assertNull(opened("""{"label":"Long","url":"https://example.com/${"x".repeat(481)}"}"""))
    }

    @Test
    fun `a list without one has none`() {
        assertNull(PublishedListDto(id = "a", title = "Films", authorName = "A").toDomain().source)
    }
}
