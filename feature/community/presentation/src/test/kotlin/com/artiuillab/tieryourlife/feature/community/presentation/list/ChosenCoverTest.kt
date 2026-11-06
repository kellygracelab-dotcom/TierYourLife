package com.artiuillab.tieryourlife.feature.community.presentation.list

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which cover a list went out with by its author's choice, read back from what the server shows. */
class ChosenCoverTest {

    private fun list(cover: String? = null, preview: List<String> = emptyList(), art: List<String?> = emptyList()) = PublishedList(
        summary = PublishedListSummary(
            id = "p1",
            title = "Holidays",
            authorUid = "me",
            authorName = "Ann",
            category = ListCategory.Other.id,
            itemCount = art.size,
            coverImageUrl = cover,
            previewImages = preview,
            updatedAtMillis = 0,
        ),
        tiers = emptyList(),
        items = art.mapIndexed { index, url -> TierItem(index.toLong(), "Card $index", url) },
    )

    @Test
    fun aSingleCover_isTheChoice() {
        assertEquals(listOf("https://c/1"), chosenCoverOf(list(cover = "https://c/1", preview = listOf("https://c/1"), art = listOf("https://a/1"))))
    }

    @Test
    fun aCollageUnlikeTheFirstCards_wasChosen_inItsOrder() {
        val chosen = listOf("https://a/2", "https://a/1")

        assertEquals(chosen, chosenCoverOf(list(preview = chosen, art = listOf("https://a/1", "https://a/2"))))
    }

    @Test
    fun theFirstCardsArt_wasNobodysChoice() {
        val art = (1..8).map { "https://a/$it" }

        assertEquals(emptyList<String>(), chosenCoverOf(list(preview = art.take(6), art = art)))
        assertEquals(emptyList<String>(), chosenCoverOf(list(preview = listOf("https://a/2"), art = listOf(null, "https://a/2"))))
        assertEquals(emptyList<String>(), chosenCoverOf(list()))
    }
}
