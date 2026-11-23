package com.artiuillab.tieryourlife.feature.tier.board.components.rows

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.InMemoryBoardLayoutMemory
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.board.share.BoardPicture
import com.artiuillab.tieryourlife.feature.tier.board.share.PictureWords
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Artboard 36: a picture's name on a strip along its foot, on until turned off, never on a card that is its name already. */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
class NamesOnCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pictures = AsyncImagePreviewHandler { request -> ColorImage(Paints[request.data] ?: Color.Gray.toArgb()) }

    private val cards = listOf(
        TierItem(1, "Spirited Away", POSTER_LIGHT),
        TierItem(2, "Everything Everywhere All at Once", POSTER_DARK),
        TierItem(3, "Up", POSTER_LIGHT),
        TierItem(4, "Coherence", null),
    )

    private fun show(memory: InMemoryBoardLayoutMemory = InMemoryBoardLayoutMemory(), dark: Boolean = false, content: @Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalBoardLayoutMemory provides memory,
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides pictures,
            ) {
                TierYourLifeTheme(darkTheme = dark) { content() }
            }
        }
    }

    @Composable
    private fun Tiles() {
        Row(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(cards[0], cards[1], cards[3]).forEach { ItemTile(it, width = 72.dp, height = 100.dp) }
            ItemTile(cards[2], width = 52.dp, height = 72.dp)
            ItemTile(cards[0], width = 40.dp, height = 56.dp)
        }
    }

    @Test
    fun aPicture_carriesItsName_aTextCardDoesNot() {
        show { Tiles() }
        // Four pictures carry a strip; the card without one is its name already.
        assertEquals(4, strips())
    }

    @Test
    fun turnedOff_noPictureCarriesItsName() {
        show(InMemoryBoardLayoutMemory().apply { namesOnCards = false }) { Tiles() }
        assertEquals(0, strips())
    }

    @Test
    fun theStrip_asItLooks() {
        show { Tiles() }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/names_on_cards.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun theStrip_dark() {
        show(dark = true) { Tiles() }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/names_on_cards_dark.png", roborazziOptions = OPTIONS)
    }

    @Test
    fun sharedAsAPicture_theNamesGoWithIt_whenTheBoardShowsThem() {
        val board = TierList(id = 1, title = "Films", tiers = listOf(Tier(1, "S", "#B03A32", "#F1948C", cards.take(2))))
        val white = createBitmap(10, 10).apply { eraseColor(android.graphics.Color.WHITE) }
        val words = PictureWords("2 cards ranked", null, "footer", "footer", "nothing here yet", { "$it cards" }, { "all $it" })
        val plain = BoardPicture.render(board, words, mapOf(1L to white, 2L to white))
        val named = BoardPicture.render(board, words, mapOf(1L to white, 2L to white), names = true)

        assertEquals(plain.height, named.height)
        assertFalse(plain.sameAs(named))
        assertFalse(named.config == Bitmap.Config.HARDWARE)
    }

    private fun strips(): Int =
        composeRule.onAllNodes(hasTestTag(TierDetailTestTags.NAME_STRIP), useUnmergedTree = true).fetchSemanticsNodes().size

    private companion object {
        const val POSTER_LIGHT = "https://example.invalid/light.jpg"
        const val POSTER_DARK = "https://example.invalid/dark.jpg"
        val Paints = mapOf(POSTER_LIGHT to 0xFFF2E3B3.toInt(), POSTER_DARK to 0xFF1F3B5C.toInt())

        val OPTIONS = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01F))
    }
}
