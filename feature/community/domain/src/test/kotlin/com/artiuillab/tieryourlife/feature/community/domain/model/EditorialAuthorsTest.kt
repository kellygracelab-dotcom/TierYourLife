package com.artiuillab.tieryourlife.feature.community.domain.model

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorialAuthorsTest {

    @Test
    fun theEditorialAccount_standsWithoutAnAuthor() {
        assertTrue(EditorialAuthors.isEditorial("85RRieyLyUPCEqHjdYCdxVlWxHx2"))
    }

    @Test
    fun anyoneElse_keepsTheirName() {
        assertFalse(EditorialAuthors.isEditorial("some-other-uid"))
    }

    @Test
    fun aListTheServerMarks_standsWithoutAnAuthor_whoeverPublishedIt() {
        assertTrue(EditorialAuthors.isEditorial("some-other-uid", anonymous = true))
    }

    // What the server sends for such lists once it marks them itself: no uid
    // at all. A profile asked for with no uid would come back as the whole feed.
    @Test
    fun noUid_standsWithoutAnAuthor() {
        assertTrue(EditorialAuthors.isEditorial(""))
        assertTrue(EditorialAuthors.isEditorial("   "))
    }

    @Test
    fun aSummary_answersFromItsOwnFields() {
        val editorial = summary(authorUid = "85RRieyLyUPCEqHjdYCdxVlWxHx2")
        val flagged = summary(authorUid = "some-other-uid", anonymous = true)
        val ordinary = summary(authorUid = "some-other-uid")

        assertTrue(editorial.editorial)
        assertTrue(flagged.editorial)
        assertFalse(ordinary.editorial)
    }

    private fun summary(authorUid: String, anonymous: Boolean = false) = PublishedListSummary(
        id = "1",
        title = "A list",
        authorUid = authorUid,
        authorName = "Someone",
        category = ListCategory.Other,
        itemCount = 1,
        updatedAtMillis = 0,
        anonymous = anonymous,
    )
}
