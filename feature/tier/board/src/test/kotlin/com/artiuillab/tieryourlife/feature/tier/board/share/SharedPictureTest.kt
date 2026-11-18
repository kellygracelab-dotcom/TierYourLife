package com.artiuillab.tieryourlife.feature.tier.board.share

import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SharedPictureTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val palette = BoardPalette(Color.White, Color.Black, Color.DarkGray, Color.LightGray, Color.White, Color.Gray, isDark = false)

    private fun board(title: String = "Films", inDeck: Int = 0) = TierList(
        id = 1,
        title = title,
        tiers = listOf(
            Tier(1, "S", "#B03A32", "#F1948C", listOf(TierItem(1, "Heat", null), TierItem(2, "Alien", null))),
            Tier(9, "Pool", "#DAD7E0", "#46464F", List(inDeck) { TierItem(10L + it, "Card $it", null) }, isPool = true),
        ),
    )

    @Test
    fun theCounts_nameTheAuthorOnlyWhenGiven() {
        assertEquals("5 cards · 2 placed · by Danylo", sharedCounts(resources, board(inDeck = 3), author = "Danylo"))
        assertEquals("2 cards · 2 placed", sharedCounts(resources, board(), author = null))
    }

    @Test
    fun theDeck_isOneLine_notARowOfCards() {
        val ranked = BoardPicture.render(board(), palette, emptyMap(), "Rank it yourself")
        val withDeck = BoardPicture.render(board(inDeck = 40), palette, emptyMap(), "Rank it yourself", notPlaced = "40 more cards not placed yet")

        assertTrue(withDeck.height - ranked.height in 1..100)
    }

    @Test
    fun aLongTitle_takesASecondLine_ratherThanAnEllipsis() {
        val short = BoardPicture.render(board(), palette, emptyMap(), "Rank it yourself")
        val long = BoardPicture.render(
            board("The definitive ranking of every breakfast cereal I have ever eaten since 1999"),
            palette,
            emptyMap(),
            "Rank it yourself",
        )

        assertTrue(long.height > short.height)
    }

    @Test
    fun aPublishedList_linksToItself() {
        assertEquals("https://tieryourlife.web.app/l/a1B2c3", publishedListLink("a1B2c3"))
    }
}
