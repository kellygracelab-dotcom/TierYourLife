package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BoardFingerprintTest {

    private val board = TierListEntity(id = 1, title = "Sci-fi films", uid = "board-1")

    private val tiers = listOf(
        TierEntity(
            id = 10,
            tierListId = 1,
            position = 0,
            label = "S",
            caption = "Masterpiece",
            colorLight = "#B03A32",
            colorDark = "#F1948C",
            uid = "tier-s",
        ),
        TierEntity(
            id = 11,
            tierListId = 1,
            position = 1,
            label = "Unranked",
            colorLight = "#3A3A3A",
            colorDark = "#CFCFCF",
            isPool = true,
            uid = "tier-pool",
        ),
    )

    private val items = listOf(
        TierItemEntity(id = 100, tierId = 10, position = 0, title = "Arrival", imageUrl = null, uid = "item-1"),
        TierItemEntity(id = 101, tierId = 10, position = 1, title = "Dune", imageUrl = null, uid = "item-2"),
    )

    private fun fingerprint(
        board: TierListEntity = this.board,
        tiers: List<TierEntity> = this.tiers,
        items: List<TierItemEntity> = this.items,
    ) = BoardFingerprint.of(board, tiers, items, ::pictureIdOf)

    /**
     * What [TierImageStore] does: the file's own name, which is the same on
     * every phone, out of a path that is not.
     */
    private fun pictureIdOf(imageUrl: String?): String? =
        imageUrl?.takeUnless { it.startsWith("http") }?.substringAfterLast('/')

    @Test
    fun `the same board twice gives the same fingerprint`() {
        assertEquals(fingerprint(), fingerprint())
    }

    // The whole point: the ids are this database's own counters, so the same
    // board on two phones has different ones.
    @Test
    fun `row ids do not reach the fingerprint`() {
        val renumbered = fingerprint(
            board = board.copy(id = 77),
            tiers = tiers.map { it.copy(id = it.id + 500, tierListId = 77) },
            items = items.map { it.copy(id = it.id + 500, tierId = it.tierId + 500) },
        )

        assertEquals(fingerprint(), renumbered)
    }

    // Publishing is between one person and the feed. A phone that never
    // published should not be handed the board back as a change.
    @Test
    fun `publishing does not count as a change`() {
        assertEquals(fingerprint(), fingerprint(board = board.copy(publishedId = "published-1")))
    }

    @Test
    fun `reading the rows in a different order changes nothing`() {
        assertEquals(fingerprint(), fingerprint(tiers = tiers.reversed(), items = items.reversed()))
    }

    @Test
    fun `renaming the board changes it`() {
        assertNotEquals(fingerprint(), fingerprint(board = board.copy(title = "Sci-fi shows")))
    }

    // Dragging one card past another is the edit this whole thing exists for,
    // and it moves no text at all.
    @Test
    fun `swapping two cards changes it`() {
        val swapped = listOf(items[0].copy(position = 1), items[1].copy(position = 0))

        assertNotEquals(fingerprint(), fingerprint(items = swapped))
    }

    @Test
    fun `moving a card to another tier changes it`() {
        val moved = listOf(items[0].copy(tierId = 11), items[1])

        assertNotEquals(fingerprint(), fingerprint(items = moved))
    }

    @Test
    fun `throwing a card away changes it`() {
        val trashed = listOf(items[0].copy(deletedAt = 1_700_000_000_000), items[1])

        assertNotEquals(fingerprint(), fingerprint(items = trashed))
    }

    @Test
    fun `recolouring a tier changes it`() {
        val recoloured = listOf(tiers[0].copy(colorLight = "#123456"), tiers[1])

        assertNotEquals(fingerprint(), fingerprint(tiers = recoloured))
    }

    // A caption nobody wrote and a caption somebody cleared are different
    // states, and only one of them is worth sending.
    @Test
    fun `an absent caption is not an empty one`() {
        val cleared = listOf(tiers[0].copy(caption = ""), tiers[1])
        val never = listOf(tiers[0].copy(caption = null), tiers[1])

        assertNotEquals(fingerprint(tiers = cleared), fingerprint(tiers = never))
    }

    // Fields carry their lengths for this: otherwise a board called "Sci" with
    // a tier "fi" and a board called "Scifi" with a tier "" are one board.
    @Test
    fun `a character moved from one field to the next is still a change`() {
        val split = fingerprint(board = board.copy(title = "Sci"), tiers = listOf(tiers[0].copy(label = "fi"), tiers[1]))
        val joined = fingerprint(board = board.copy(title = "Scifi"), tiers = listOf(tiers[0].copy(label = ""), tiers[1]))

        assertNotEquals(split, joined)
    }

    // Without this two phones holding the same board would never agree that
    // they do, and would hand each other copies of it forever: the directory
    // around a picture is this phone's, and the name inside it is everyone's.
    @Test
    fun `the same picture under two different paths is the same picture`() {
        val here = listOf(items[0].copy(imageUrl = "/data/user/0/app/files/tier_images/pic-1"), items[1])
        val there = listOf(items[0].copy(imageUrl = "/data/user/10/app/files/tier_images/pic-1"), items[1])

        assertEquals(fingerprint(items = here), fingerprint(items = there))
    }

    @Test
    fun `swapping the picture on a card changes it`() {
        val before = listOf(items[0].copy(imageUrl = "/files/tier_images/pic-1"), items[1])
        val after = listOf(items[0].copy(imageUrl = "/files/tier_images/pic-2"), items[1])

        assertNotEquals(fingerprint(items = before), fingerprint(items = after))
    }

    @Test
    fun `a poster from a catalogue still counts as itself`() {
        val poster = listOf(items[0].copy(imageUrl = "https://image.tmdb.org/t/p/w500/a.jpg"), items[1])

        assertNotEquals(fingerprint(), fingerprint(items = poster))
    }

    @Test
    fun `two different boards do not collide`() {
        assertNotEquals(fingerprint(), fingerprint(board = board.copy(uid = "board-2")))
    }
}
