package com.artiuillab.tieryourlife.feature.tier.presentation.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private val palette = BoardPalette(
        surface = Color(0xFFFBF8FF),
        onSurface = Color(0xFF1B1B21),
        onSurfaceVariant = Color(0xFF46464F),
        outlineVariant = Color(0xFFE4E1E9),
        onBand = Color.White,
        unrankedBand = Color(0xFFDAD7E0),
        isDark = false,
    )

    @Test
    fun theBoard_isDrawnAtOneWidth_withEachTierInItsOwnColour() {
        val bitmap = BoardPicture.render(board(), palette, pictures = emptyMap(), footer = "Made in TierYourLife")
        save("board-light", bitmap)

        assertEquals(BoardPicture.WIDTH, bitmap.width)
        assertTrue("taller than its header alone", bitmap.height > 400)
        // Just inside the first row's band, away from the letter.
        assertEquals(Color(0xFFB03A32).toArgb(), bitmap.getPixel(60, 260))
        // The corner, where nothing is drawn but the surface.
        assertEquals(palette.surface.toArgb(), bitmap.getPixel(4, 4))
    }

    // A dark board picks the tier's dark colour, so a picture shared at night
    // is the board the person was looking at.
    @Test
    fun aDarkPalette_drawsTheDarkBand() {
        val dark = palette.copy(surface = Color(0xFF121318), isDark = true)
        val bitmap = BoardPicture.render(board(), dark, pictures = emptyMap(), footer = "Made in TierYourLife")

        assertEquals(Color(0xFFF1948C).toArgb(), bitmap.getPixel(60, 260))
    }

    // A card's picture lands inside its tile; a card without one does not
    // stop the board being drawn.
    @Test
    fun aCardsPicture_isDrawnIntoItsTile() {
        val red = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.Red.toArgb()) }
        val bitmap = BoardPicture.render(board(), palette, pictures = mapOf(1L to red), footer = "Made in TierYourLife")

        // Somewhere along the middle of the first row of tiles there is a
        // tile's width of red. Where exactly depends on the band, which is as
        // wide as the longest caption, so the test does not pretend to know.
        val tileMiddleY = 48 + 56 + 12 + 34 + 48 + 24 + 192 / 2
        val reds = (0 until bitmap.width).count { x -> bitmap.getPixel(x, tileMiddleY) == Color.Red.toArgb() }
        assertTrue("a tile's width of red, found $reds px", reds in 100..140)
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
