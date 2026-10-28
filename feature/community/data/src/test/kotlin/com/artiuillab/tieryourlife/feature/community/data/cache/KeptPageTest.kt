package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.feature.community.domain.model.BasedOn
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A kept feed reads back as the page that was kept, and only its first page is kept. */
class KeptPageTest {

    private fun summary(n: Int) = PublishedListSummary(
        id = "l$n",
        title = "List $n",
        authorUid = "u$n",
        authorName = "Author $n",
        category = ListCategory.Food.id,
        itemCount = n,
        previewImages = listOf("https://p/$n"),
        tierColors = listOf("#B03A32"),
        updatedAtMillis = 1_000L * n,
        takeCount = n,
        topicId = "t",
        topicName = "Noodles",
        rankingCount = 2,
        basedOn = BasedOn("src", "Source", "Olena"),
    )

    @Test
    fun `a page reads back as it was kept`() {
        val page = CommunityPage(lists = listOf(summary(1), summary(2)), nextCursor = "c", followingNobody = false)

        assertEquals(page, decodePage(encodePage(page)))
    }

    @Test
    fun `only the first twenty are kept, and then the cursor would skip, so it goes`() {
        val page = CommunityPage(lists = (1..25).map(::summary), nextCursor = "after-25")

        val kept = decodePage(encodePage(page))

        assertEquals(20, kept.lists.size)
        assertNull(kept.nextCursor)
    }
}
