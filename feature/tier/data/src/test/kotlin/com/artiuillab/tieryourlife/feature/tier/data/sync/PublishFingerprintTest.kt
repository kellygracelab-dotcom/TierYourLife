package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Has to notice everything publishing sends and nothing else. Noticing too
 * much is worse: it puts an Update button on a board with nothing to send.
 */
class PublishFingerprintTest {

    @Test
    fun aBoardThatHasNotChanged_looksTheSame() {
        assertEquals(print(board()), print(board()))
    }

    @Test
    fun renamingTheBoard_showsUp() {
        assertNotEquals(print(board()), print(board(title = "Shows")))
    }

    @Test
    fun movingToAnotherCategory_showsUp() {
        assertNotEquals(print(board()), print(board(category = ListCategory.Anime)))
    }

    @Test
    fun changingTheCover_showsUp() {
        assertNotEquals(print(board()), print(board(cover = "https://example.test/other.jpg")))
    }

    @Test
    fun renamingATier_showsUp() {
        assertNotEquals(print(board()), print(board(tierLabel = "A")))
    }

    @Test
    fun recolouringATier_showsUp() {
        assertNotEquals(print(board()), print(board(tierColour = "#000000")))
    }

    @Test
    fun renamingACard_showsUp() {
        assertNotEquals(print(board()), print(board(cardTitle = "Dune")))
    }

    @Test
    fun addingACard_showsUp() {
        assertNotEquals(print(board()), print(board(extraCard = true)))
    }

    @Test
    fun givingACardADifferentPicture_showsUp() {
        assertNotEquals(print(board()), print(board(cardImage = "https://example.test/new.jpg")))
    }

    // The display mode never travels to the feed; saying the copy was behind
    // because of it would send somebody to republish a right list.
    @Test
    fun changingHowTiersAreDrawn_doesNot() {
        assertEquals(print(board()), print(board(mode = TierListDisplayMode.FLAT_RANKED)))
    }

    // Where the author put each card is not published either.
    @Test
    fun whatSitsInThePool_isCountedLikeAnythingElse() {
        // Not a claim that position is ignored: the cards are sent as one list.
        // This only pins that a card in the pool is part of it.
        assertNotEquals(print(board()), print(board(poolCard = true)))
    }

    // The same photograph at two paths is the same published copy; neither
    // phone should be told it is behind because of where it keeps its files.
    @Test
    fun theSamePhotographUnderTwoPaths_looksTheSame() {
        val here = board(cardImage = "/data/user/0/app/files/tier_images/abc")
        val there = board(cardImage = "/storage/emulated/0/Android/data/app/files/tier_images/abc")
        assertEquals(print(here), print(there))
    }

    @Test
    fun aDifferentPhotograph_showsUp() {
        val one = board(cardImage = "/data/user/0/app/files/tier_images/abc")
        val other = board(cardImage = "/data/user/0/app/files/tier_images/def")
        assertNotEquals(print(one), print(other))
    }

    /** The last path segment, which is how the app names its own pictures. */
    private fun print(list: TierList): String =
        PublishFingerprint.of(list) { url -> url?.takeIf { !it.startsWith("https://") }?.substringAfterLast('/') }

    private fun board(
        title: String = "Sci-fi films",
        category: ListCategory? = ListCategory.FilmTv,
        cover: String? = "https://example.test/cover.jpg",
        mode: TierListDisplayMode = TierListDisplayMode.WRAP,
        tierLabel: String = "S",
        tierColour: String = "#B03A32",
        cardTitle: String = "Arrival",
        cardImage: String? = "https://example.test/a.jpg",
        extraCard: Boolean = false,
        poolCard: Boolean = false,
    ) = TierList(
        id = 1,
        title = title,
        displayMode = mode,
        category = category,
        coverImageUrl = cover,
        tiers = listOf(
            Tier(
                id = 1,
                label = tierLabel,
                caption = "Masterpiece",
                colorLight = tierColour,
                colorDark = "#F1948C",
                items = buildList {
                    add(TierItem(id = 1, title = cardTitle, imageUrl = cardImage))
                    if (extraCard) add(TierItem(id = 2, title = "Dune", imageUrl = null))
                },
            ),
            Tier(
                id = 2,
                label = "Unranked",
                colorLight = "#DAD7E0",
                colorDark = "#46464F",
                isPool = true,
                items = if (poolCard) listOf(TierItem(id = 3, title = "Solaris", imageUrl = null)) else emptyList(),
            ),
        ),
    )
}
