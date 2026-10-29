package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.cache.decodePage
import com.artiuillab.tieryourlife.feature.community.data.cache.encodePage
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedFeedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The server's `mine` reaches the screens, and a named list carrying this person's uid is theirs too. */
class OwnListMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun summary(authorUid: String, mine: Boolean = false, anonymous: Boolean = false) =
        PublishedListSummaryDto(id = "l", title = "Films", authorUid = authorUid, authorName = "", itemCount = 1, anonymous = anonymous, mine = mine)

    @Test
    fun `my anonymous list, which carries no uid, is mine because the server says so`() {
        assertTrue(summary(authorUid = "", mine = true, anonymous = true).toSummary(me = "me").mine)
    }

    @Test
    fun `my named list is mine by its uid`() {
        assertTrue(summary(authorUid = "me").toSummary(me = "me").mine)
    }

    @Test
    fun `somebody else's list is not mine, named or anonymous`() {
        assertFalse(summary(authorUid = "olena").toSummary(me = "me").mine)
        assertFalse(summary(authorUid = "", anonymous = true).toSummary(me = "me").mine)
    }

    @Test
    fun `an opened list says the same`() {
        val anonymous = PublishedListDto(id = "l", title = "Films", authorUid = "", authorName = "", anonymous = true, mine = true)
        val theirs = PublishedListDto(id = "l", title = "Films", authorUid = "olena", authorName = "Olena")

        assertTrue(anonymous.toDomain(me = "me").summary.mine)
        assertTrue(theirs.copy(authorUid = "me").toDomain(me = "me").summary.mine)
        assertFalse(theirs.toDomain(me = "me").summary.mine)
    }

    @Test
    fun `the field is read from the server's answer, and absent means not mine`() {
        val page = json.decodeFromString(
            PublishedFeedDto.serializer(),
            """{"lists":[{"id":"a","title":"A","authorUid":"","authorName":"","itemCount":1,"anonymous":true,"mine":true},""" +
                """{"id":"b","title":"B","authorUid":"x","authorName":"X","itemCount":1}]}""",
        )

        assertTrue(page.lists[0].toSummary(me = "me").mine)
        assertFalse(page.lists[1].toSummary(me = "me").mine)
    }

    @Test
    fun `a kept feed still knows which lists were mine`() {
        val mine = summary(authorUid = "", mine = true, anonymous = true).toSummary(me = "me")

        assertTrue(decodePage(encodePage(CommunityPage(lists = listOf(mine)))).lists.single().mine)
    }
}
