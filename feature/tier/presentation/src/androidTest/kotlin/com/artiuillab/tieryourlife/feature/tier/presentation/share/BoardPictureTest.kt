package com.artiuillab.tieryourlife.feature.tier.presentation.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.tier.board.share.BoardPicture
import com.artiuillab.tieryourlife.feature.tier.board.share.PictureWords
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The picture is drawn, not photographed, so it can be checked: the right
 * width, a band in the tier's own colour where the band should be, and the
 * surface where nothing is.
 */
@RunWith(AndroidJUnit4::class)
class BoardPictureTest {

    private val words = PictureWords(
        byLine = "by Ann · 1 card ranked",
        author = "Ann",
        footer = "tieryourlife.com/l/abc",
        shortFooter = "tieryourlife.com",
        nothingYet = "nothing here yet",
        cards = { "$it cards" },
        seeAll = { "Open the list to see all $it" },
    )

    @Test
    fun theBoard_isDrawnAtFourByFive_withEachTierInItsDarkColour() {
        val bitmap = BoardPicture.render(board(), words, pictures = emptyMap())
        save("board", bitmap)

        assertEquals(BoardPicture.WIDTH, bitmap.width)
        assertEquals(BoardPicture.HEIGHT, bitmap.height)
        // Just inside the first row's band, away from the letter: the picture is dark, so the dark colour.
        assertEquals(Color(0xFFF1948C).toArgb(), bitmap.getPixel(90, 230))
        // The bottom corner, where nothing is drawn but the background.
        assertEquals(Color(0xFF12151C).toArgb(), bitmap.getPixel(4, bitmap.height - 4))
    }

    // A card's picture lands inside its cell; a card without one does not stop the board being drawn.
    @Test
    fun aCardsPicture_isDrawnIntoItsCell() {
        val red = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.Red.toArgb()) }
        val bitmap = BoardPicture.render(board(), words, pictures = mapOf(1L to red))
        save("board-cover", bitmap)

        // The middle of the first row of a short list's small cells.
        val reds = (0 until bitmap.width).count { x -> bitmap.getPixel(x, 296) == Color.Red.toArgb() }
        assertTrue("a cell's width of red, found $reds px", reds in 90..110)
    }

    // Written out beside the test results so the picture can be looked at,
    // the way the README pictures are. Nothing reads it back.
    private fun save(name: String, bitmap: Bitmap) {
        val outputDir = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath
        val dir = File(outputDir, "share").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun board() = TierList(
        id = 1,
        title = "Films I make people watch",
        tiers = listOf(
            Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", caption = "Masterpiece", items = listOf(TierItem(1, "The Godfather", null))),
            Tier(id = 2, label = "A", colorLight = "#C06A25", colorDark = "#E9A867", caption = "Great", items = emptyList()),
            Tier(id = -1, label = "Unranked", colorLight = "#DAD7E0", colorDark = "#46464F", items = listOf(TierItem(2, "Whiplash", null)), isPool = true),
        ),
    )
}
