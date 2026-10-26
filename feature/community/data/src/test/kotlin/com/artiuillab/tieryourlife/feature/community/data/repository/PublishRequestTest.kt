package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What leaves the phone when a board is published, and what a published list becomes on the way back. */
class PublishRequestTest {

    private object Pictures : OwnPictures {
        override fun pictureIdOf(imageUrl: String?): String? =
            imageUrl?.takeIf { it.startsWith("file://") }?.substringAfterLast('/')

        override suspend fun sendNow(pictureIds: List<String>): Set<String> = pictureIds.toSet()
    }

    private val poster = "https://image.tmdb.org/t/p/w500/poster.jpg"

    private fun tier(id: Long, label: String, vararg items: TierItem, pool: Boolean = false) =
        Tier(id = id, label = label, colorLight = "#111111", colorDark = "#EEEEEE", items = items.toList(), isPool = pool)

    private val board = TierList(
        id = 1,
        title = "Films",
        category = ListCategory.FilmTv,
        coverImageUrl = "file:///pictures/cover-1",
        tiers = listOf(
            tier(10, "S", TierItem(1, "Alien", poster)),
            tier(20, "A", TierItem(2, "Heat", "file:///pictures/photo-2")),
            tier(30, "Pool", TierItem(3, "Up", "file:///pictures/photo-3"), pool = true),
        ),
    )

    @Test
    fun `the pool is not published as a tier`() {
        val request = board.toRequest(Pictures, uploaded = setOf("photo-2", "photo-3", "cover-1"))

        assertEquals(listOf("S", "A"), request.tiers.map { it.label })
    }

    @Test
    fun `each card names its tier among the published ones, and a pool card names none`() {
        val request = board.toRequest(Pictures, uploaded = setOf("photo-2", "photo-3", "cover-1"))

        assertEquals(listOf(0, 1, null), request.items.map { it.tierIndex })
    }

    @Test
    fun `a poster travels by address and an own photo by name`() {
        val request = board.toRequest(Pictures, uploaded = setOf("photo-2", "photo-3", "cover-1"))

        val alien = request.items[0]
        val heat = request.items[1]
        assertEquals(poster, alien.imageUrl)
        assertNull(alien.pictureId)
        assertNull(heat.imageUrl)
        assertEquals("photo-2", heat.pictureId)
        assertEquals("cover-1", request.coverPictureId)
        assertNull(request.coverImageUrl)
    }

    @Test
    fun `a photo that did not upload is left unnamed rather than named and missing`() {
        val request = board.toRequest(Pictures, uploaded = setOf("photo-3"))

        assertNull(request.items[1].pictureId)
        assertNull(request.coverPictureId)
    }

    @Test
    fun `a chosen collage travels by its web addresses`() {
        val collage = listOf(poster, "https://example.test/b.jpg")
        val request = board.copy(coverImageUrl = null, coverImageUrls = collage).toRequest(Pictures, uploaded = emptySet())

        assertEquals(collage, request.coverImageUrls)
        assertNull(request.coverImageUrl)
    }

    @Test
    fun `a board without a category is published as other`() {
        val request = board.copy(category = null).toRequest(Pictures, uploaded = emptySet())

        assertEquals(ListCategory.Other.id, request.category)
    }

    @Test
    fun `a published list comes back with its tiers, cards and the author's arrangement`() {
        val dto = PublishedListDto(
            id = "p1",
            title = "Films",
            authorName = "Ann",
            category = "film_tv",
            tiers = listOf(PublishedTierDto("S", null, "#111111", "#EEEEEE"), PublishedTierDto("A", "Good", "#222222", "#DDDDDD")),
            items = listOf(
                PublishedItemDto("Alien", poster, tierIndex = 1),
                PublishedItemDto("Up", null, tierIndex = null),
            ),
        )

        val published = dto.toDomain()

        assertEquals(listOf("S", "A"), published.tiers.map { it.label })
        assertEquals("Good", published.tiers[1].caption)
        assertEquals(listOf("Alien", "Up"), published.items.map { it.title })
        assertEquals(listOf(1, null), published.arrangement)
        assertEquals(ListCategory.FilmTv, published.summary.category)
    }

    @Test
    fun `a category this app does not know reads as other`() {
        val dto = PublishedListDto(id = "p1", title = "T", authorName = "A", category = "from-the-future")

        assertEquals(ListCategory.Other, dto.toDomain().summary.category)
    }

    @Test
    fun `a known topic travels by id, a new one by name, never both`() {
        val known = board.copy(topicId = "t1", topicName = "Oscars", newTopicName = "stale draft").toRequest(Pictures, uploaded = emptySet())
        assertEquals("t1", known.topicId)
        assertNull(known.newTopic)

        val fresh = board.copy(newTopicName = "Oscars 2025").toRequest(Pictures, uploaded = emptySet())
        assertNull(fresh.topicId)
        assertEquals("Oscars 2025", fresh.newTopic)
    }
}
