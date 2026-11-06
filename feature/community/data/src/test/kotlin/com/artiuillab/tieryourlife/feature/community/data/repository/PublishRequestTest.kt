package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.core.network.networkJson
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedItemDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedTierDto
import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        category = ListCategory.FilmTv.id,
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

    // The server decides whether the same cards make this a ranking of that list; the phone only says where they came from.
    @Test
    fun `a copy names the list it was taken from`() {
        val request = board.copy(takenFrom = "src-1").toRequest(Pictures, uploaded = emptySet())

        assertEquals("src-1", request.takenFrom)
        assertNull(board.toRequest(Pictures, uploaded = emptySet()).takenFrom)
    }

    // Missing means "keep what is stored" on a republish, so both answers are always said out loud.
    @Test
    fun `whether a list goes out anonymously is always said, true or false`() {
        val named = networkJson.parseToJsonElement(
            networkJson.encodeToString(PublishListRequestDto.serializer(), board.toRequest(Pictures, uploaded = emptySet())),
        ).jsonObject
        val anonymous = networkJson.parseToJsonElement(
            networkJson.encodeToString(PublishListRequestDto.serializer(), board.copy(anonymous = true).toRequest(Pictures, uploaded = emptySet())),
        ).jsonObject

        assertEquals("false", named["anonymous"].toString())
        assertEquals("true", anonymous["anonymous"].toString())
    }

    @Test
    fun `an anonymous original is credited to nobody, and a missing name reads as none`() {
        val json = """{"id":"p1","title":"T","authorName":"A","basedOn":{"id":"src","title":"Films","authorUid":"","authorName":null}}"""

        val basedOn = networkJson.decodeFromString(PublishedListDto.serializer(), json).toDomain().summary.basedOn

        assertEquals(BasedOn(id = "src", title = "Films", authorName = "", authorUid = ""), basedOn)
    }

    @Test
    fun `a named original keeps its author and their uid`() {
        val json = """{"id":"p1","title":"T","authorName":"A","basedOn":{"id":"src","title":"Films","authorUid":"u-7","authorName":"Olena"}}"""

        val basedOn = networkJson.decodeFromString(PublishedListDto.serializer(), json).toDomain().summary.basedOn

        assertEquals(BasedOn(id = "src", title = "Films", authorName = "Olena", authorUid = "u-7"), basedOn)
    }

    @Test
    fun `a credited picture is credited in the feed, and an uncredited one sends no key`() {
        val credited = board.copy(
            tiers = listOf(tier(10, "S", TierItem(1, "Red fox", poster, credit = "© Jane Doe, CC BY"), TierItem(2, "Badger", poster))),
        )

        val body = networkJson.parseToJsonElement(
            networkJson.encodeToString(PublishListRequestDto.serializer(), credited.toRequest(Pictures, uploaded = emptySet())),
        ).jsonObject
        val items = body.getValue("items").jsonArray.map { it.jsonObject }

        assertEquals(JsonPrimitive("© Jane Doe, CC BY"), items[0]["credit"])
        assertFalse("credit" in items[1])
    }

    // What an opened list is kept as reads back through the same mapping, so a credit must survive both ways.
    @Test
    fun `a published list's credits come back, and stay through the copy kept of it`() {
        val dto = PublishedListDto(
            id = "p1",
            title = "Animals",
            authorName = "Ann",
            items = listOf(PublishedItemDto("Red fox", poster, credit = "© Jane Doe, CC BY"), PublishedItemDto("Badger", null)),
        )

        val published = dto.toDomain()

        assertEquals(listOf("© Jane Doe, CC BY", null), published.items.map { it.credit })
        assertEquals(listOf("© Jane Doe, CC BY", null), published.toDto().toDomain().items.map { it.credit })
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
        assertEquals(ListCategory.FilmTv.id, published.summary.category)
    }

    @Test
    fun `a section this app has not heard of keeps its id, for the section list to name later`() {
        val dto = PublishedListDto(id = "p1", title = "T", authorName = "A", category = "street-food")

        assertEquals("street-food", dto.toDomain().summary.category)
    }

    @Test
    fun `a list filed nowhere reads as other`() {
        val dto = PublishedListDto(id = "p1", title = "T", authorName = "A", category = " ")

        assertEquals(ListCategory.Other.id, dto.toDomain().summary.category)
    }

    @Test
    fun `a board in somebody's section is published under that section`() {
        assertEquals("street-food", board.copy(category = "street-food").toRequest(Pictures, uploaded = emptySet()).category)
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

    private fun body(list: TierList) = networkJson.parseToJsonElement(
        networkJson.encodeToString(PublishListRequestDto.serializer(), list.toRequest(Pictures, uploaded = emptySet())),
    ).jsonObject

    // The author took it off: without noTopic the server would keep it while the section stays the same.
    @Test
    fun `a topic the author took off is said to be off, and nothing else about topics goes`() {
        val off = body(board.copy(publishedId = "p1", topicTakenOff = true))

        assertEquals(JsonPrimitive(true), off["noTopic"])
        assertFalse("topicId" in off)
        assertFalse("newTopic" in off)
    }

    // A moderator may have moved the list into a topic since; a board that knows of none must not undo that.
    @Test
    fun `a board that knows of no topic says nothing about one`() {
        val none = body(board.copy(publishedId = "p1"))

        assertFalse("noTopic" in none)
        assertFalse("topicId" in none)
        assertFalse("newTopic" in none)
    }

    @Test
    fun `a board with a topic, known or new, sends no noTopic at all`() {
        val known = body(board.copy(topicId = "t1", topicName = "Oscars"))
        val fresh = body(board.copy(newTopicName = "Oscars 2025"))

        assertEquals(JsonPrimitive("t1"), known["topicId"])
        assertFalse("noTopic" in known)
        assertEquals(JsonPrimitive("Oscars 2025"), fresh["newTopic"])
        assertFalse("noTopic" in fresh)
    }

    @Test
    fun `the board's uid goes with every publish, trimmed`() {
        assertEquals(JsonPrimitive("b-123"), body(board.copy(uid = " b-123 "))["boardUid"])
        assertEquals(JsonPrimitive("b-123"), body(board.copy(uid = "b-123", publishedId = "p1"))["boardUid"])
    }

    @Test
    fun `a uid the server would refuse is left out rather than sent`() {
        assertFalse("boardUid" in body(board.copy(uid = " ")))
        assertFalse("boardUid" in body(board.copy(uid = "x".repeat(101))))
        assertEquals(JsonPrimitive("x".repeat(100)), body(board.copy(uid = "x".repeat(100)))["boardUid"])
    }

    private val ownPhoto = "https://firebasestorage.googleapis.com/v0/b/app.appspot.com/o/users%2Fuid-anna%2Fpictures%2Fphoto-2?alt=media&token=t"

    // The website keeps a cover from the person's own photo by its private address, which names their account.
    @Test
    fun `a cover the website chose from an own photo goes by name, never by its private address`() {
        val request = board.copy(coverImageUrl = ownPhoto, coverImageUrls = listOf(ownPhoto)).toRequest(Pictures, uploaded = setOf("photo-2", "photo-3"))

        assertNull(request.coverImageUrl)
        assertEquals("photo-2", request.coverPictureId)
        assertEquals(emptyList<String>(), request.coverImageUrls)
    }

    @Test
    fun `a private cover whose card has left the board goes nowhere`() {
        val request = board.copy(coverImageUrl = ownPhoto.replace("photo-2", "photo-9")).toRequest(Pictures, uploaded = setOf("photo-2", "photo-9"))

        assertNull(request.coverImageUrl)
        assertNull(request.coverPictureId)
    }

    @Test
    fun `no private address travels in a collage or on a card`() {
        val list = board.copy(
            coverImageUrl = null,
            coverImageUrls = listOf(poster, ownPhoto),
            tiers = listOf(tier(10, "S", TierItem(1, "Holiday", ownPhoto))),
        )

        val body = networkJson.encodeToString(PublishListRequestDto.serializer(), list.toRequest(Pictures, uploaded = setOf("photo-2")))

        assertFalse(body, body.contains("users%2F", ignoreCase = true))
        assertTrue(body, body.contains(poster))
        assertEquals("photo-2", list.toRequest(Pictures, uploaded = setOf("photo-2")).items.single().pictureId)
    }

    @Test
    fun `a private address is known by its folder, whatever the case of its escapes`() {
        assertTrue(isPrivatePicture(ownPhoto))
        assertTrue(isPrivatePicture(ownPhoto.replace("%2F", "%2f")))
        assertFalse(isPrivatePicture("https://firebasestorage.googleapis.com/v0/b/app.appspot.com/o/published%2Fp1%2Fphoto-2?alt=media"))
        assertEquals("photo-2", privatePictureIdOf(ownPhoto.replace("%2F", "%2f")))
        assertNull(privatePictureIdOf(poster))
    }

    // 200 with existing: the list an interrupted publish made. The board takes its id as it would a new one's.
    @Test
    fun `a list the server had out already links the board like a new one`() {
        val existing = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p7","topicId":"t1","existing":true}""")
        val made = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p7","topicId":"t1"}""")

        assertTrue(existing.existing)
        assertFalse(made.existing)
        assertEquals(Published(id = "p7", fingerprint = "f", topicId = "t1"), existing.toPublished("f"))
        assertEquals(made.toPublished("f"), existing.toPublished("f"))
    }

    @Test
    fun `an answer that holds the list for a moderator says so, and one without the field does not`() {
        val held = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p8","topicId":"t1","underReview":true}""")
        val out = networkJson.decodeFromString(PublishedIdDto.serializer(), """{"id":"p8","topicId":"t1"}""")

        assertTrue(held.toPublished("f").underReview)
        assertFalse(out.toPublished("f").underReview)
    }

    @Test
    fun `the board a list of mine was published from comes back with it`() {
        val mine = networkJson.decodeFromString(
            PublishedListSummaryDto.serializer(),
            """{"id":"p1","title":"T","authorName":"A","itemCount":1,"boardUid":"b-123"}""",
        )
        val older = networkJson.decodeFromString(PublishedListSummaryDto.serializer(), """{"id":"p2","title":"T","authorName":"A","itemCount":1}""")

        assertEquals("b-123", mine.toSummary().boardUid)
        assertEquals("b-123", mine.toSummary().toDto().toSummary().boardUid)
        assertNull(older.toSummary().boardUid)
    }
}
