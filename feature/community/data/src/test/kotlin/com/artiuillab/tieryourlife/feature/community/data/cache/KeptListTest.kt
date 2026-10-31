package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicDto
import com.artiuillab.tieryourlife.feature.community.data.repository.goneIfMissing
import com.artiuillab.tieryourlife.feature.community.data.repository.toDomain
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.ListGone
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** An opened list kept on the phone reads back as the list that was opened, whose it is included. */
class KeptListTest {

    private val opened = PublishedList(
        summary = PublishedListSummary(
            id = "abc",
            title = "Every A24 film",
            authorUid = "author-1",
            authorName = "Olena M.",
            category = ListCategory.FilmTv.id,
            itemCount = 3,
            tierColors = listOf("#B03A32", "#C06A25"),
            updatedAtMillis = 42,
            rankingCount = 13,
            basedOn = BasedOn("src", "Source", "Kenji", authorUid = "u-kenji"),
            mine = true,
            tierCounts = listOf(1, 1, 1),
            viewerFollowsAuthor = true,
        ),
        tiers = listOf(
            Tier(id = 0, label = "S", caption = "Masterpiece", colorLight = "#B03A32", colorDark = "#F1948C", items = emptyList()),
            Tier(id = 1, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", items = emptyList()),
        ),
        items = listOf(
            TierItem(0, "Hereditary", "https://i/1.jpg"),
            TierItem(1, "Moonlight", null),
            TierItem(2, "Pearl", "https://i/3.jpg"),
        ),
        arrangement = listOf(1, null, 0),
    )

    @Test
    fun `an opened list reads back as it was kept`() {
        assertEquals(opened, decodeOpened(encodeOpened(opened, keptBy = "author-1"), me = "author-1"))
    }

    // A kept their list on this phone, then B signed in and opened it offline.
    @Test
    fun `a list kept under another account is not mine and does not know whom I follow`() {
        val read = decodeOpened(encodeOpened(opened, keptBy = "author-1"), me = "reader-2")?.summary

        assertEquals(false, read?.mine)
        assertNull(read?.viewerFollowsAuthor)
    }

    @Test
    fun `a list kept under another account is still mine when I wrote it`() {
        val keptByReader = opened.copy(summary = opened.summary.copy(mine = false))

        assertEquals(true, decodeOpened(encodeOpened(keptByReader, keptBy = "reader-2"), me = "author-1")?.summary?.mine)
    }

    @Test
    fun `a row kept before the keeper was written reads as never kept`() {
        assertNull(decodeOpened("""{"id":"abc","title":"Every A24 film","mine":true}""", me = "reader-2"))
    }

    @Test
    fun `each list has a row of its own, beside the feeds and the sections`() {
        assertEquals("list:abc", keyOf("abc"))
    }

    @Test
    fun `a list summary from a server before tier counts reads as not knowing them`() {
        val list = Json { ignoreUnknownKeys = true }
            .decodeFromString(PublishedListDto.serializer(), """{"id":"l","title":"Films","authorName":"Olena"}""")
            .toDomain()

        assertEquals(emptyList<Int>(), list.summary.tierCounts)
        assertNull(list.summary.viewerFollowsAuthor)
    }

    @Test
    fun `not found means the list is gone, anything else stays what it was`() {
        val missing = HttpException(Response.error<Unit>(404, "{}".toResponseBody("application/json".toMediaType())))
        val down = HttpException(Response.error<Unit>(503, "{}".toResponseBody("application/json".toMediaType())))

        assertTrue(missing.goneIfMissing() is ListGone)
        assertSame(down, down.goneIfMissing())
    }

    @Test
    fun `a topic answer carries its picture, or null for none`() {
        val json = Json { ignoreUnknownKeys = true }
        val withPicture = json.decodeFromString(TopicDto.serializer(), """{"id":"t","pictureUrl":"https://p/t.webp"}""")
        val withNone = json.decodeFromString(TopicDto.serializer(), """{"id":"t","pictureUrl":null}""")

        assertEquals("https://p/t.webp", withPicture.pictureUrl)
        assertNull(withNone.pictureUrl)
    }
}
