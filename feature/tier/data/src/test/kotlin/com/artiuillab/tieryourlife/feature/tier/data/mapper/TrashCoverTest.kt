package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierListRow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Walkthrough 26.11, item 11: a list in the trash shows its own cover, not a common icon. */
class TrashCoverTest {

    private val row = DeletedTierListRow(id = 1, title = "TV series", itemCount = 14, deletedAt = 0, publishedId = null, rankingOf = null)

    @Test
    fun aChosenCollage_isTheCover() {
        val entry = row.copy(coverImageUrls = "https://a\nhttps://b").toDomain(cardPictures = listOf("https://card"))

        assertEquals(listOf("https://a", "https://b"), entry.pictures)
    }

    @Test
    fun aCoverLeftToTheCards_showsTheirPictures() {
        val entry = row.toDomain(cardPictures = listOf("https://card1", "https://card2"))

        assertEquals(listOf("https://card1", "https://card2"), entry.pictures)
    }

    @Test
    fun oneChosenPicture_staysTheCover() {
        val entry = row.copy(coverImageUrl = "https://one").toDomain()

        assertEquals("https://one", entry.coverImageUrl)
    }
}
