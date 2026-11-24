package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.art
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components.rowArt
import org.junit.Assert.assertEquals
import org.junit.Test

/** The 56dp row shows at most four pictures, 2×2, the same four that lead the grid's tile. */
class RowArtTest {

    private val board = TierListSummary(
        1,
        "Films",
        tiers = emptyList(),
        previewImages = listOf("a", "b", "b", "c", "d", "e", "f"),
    )

    @Test
    fun theRow_takesTheFirstFourDistinct_asTheGridLeadsWith() {
        assertEquals(listOf("a", "b", "c", "d"), board.rowArt().previewImages)
        assertEquals(board.rowArt().previewImages, board.art().previewImages.distinct().take(4))
    }
}
