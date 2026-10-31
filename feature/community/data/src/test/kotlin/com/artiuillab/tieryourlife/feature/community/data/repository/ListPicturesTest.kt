package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Old lists on the server keep the word "null" as their cover; on the phone that is no cover at all. */
class ListPicturesTest {

    @Test
    fun `a cover stored as the word null is no cover, so the mosaic shows`() {
        val summary = PublishedListSummaryDto(
            id = "a",
            title = "Oscar films",
            authorName = "A",
            itemCount = 3,
            coverImageUrl = "null",
            previewImages = listOf("https://p/1.jpg", "null", ""),
        ).toSummary()

        assertNull(summary.coverImageUrl)
        assertEquals(listOf("https://p/1.jpg"), summary.previewImages)
    }

    @Test
    fun `a web cover stays as it is`() {
        val summary = PublishedListSummaryDto(id = "a", title = "Films", authorName = "A", itemCount = 1, coverImageUrl = "https://i/c.jpg").toSummary()

        assertEquals("https://i/c.jpg", summary.coverImageUrl)
    }

    @Test
    fun `an opened list with the word null for a cover has none`() {
        val list = PublishedListDto(id = "a", title = "Films", authorName = "A", coverImageUrl = "null").toDomain()

        assertNull(list.summary.coverImageUrl)
    }
}
