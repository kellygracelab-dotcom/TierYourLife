package com.artiuillab.tieryourlife.feature.tier.board.share

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.layout.LocalBoardLayoutMemory
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shared picture (23.11 mockup): a long list as twelve covers and thin bands, a short one whole, and the sheet that opens at once. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SharedPictureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resources = context.resources

    private fun board(sizes: List<Int>, inDeck: Int = 0, title: String = "Video games I've played: my tier list", publishedId: String? = null): TierList {
        var id = 0L
        fun cards(n: Int) = List(n) { TierItem(++id, "Card $id", "https://img.test/$id.jpg") }
        val labels = listOf("S", "A", "B", "C", "D", "E", "F")
        return TierList(
            id = 1,
            title = title,
            publishedId = publishedId,
            tiers = sizes.mapIndexed { i, n -> Tier(i + 1L, labels[i], "#B03A32", "#F1948C", cards(n)) } +
                Tier(99, "Pool", "#DAD7E0", "#46464F", cards(inDeck), isPool = true),
        )
    }

    private val games = board(listOf(17, 36, 50, 45, 28), inDeck = 5, publishedId = "jl4NPXiawyjQRbp1RT83")
    private val series = board(listOf(0, 2, 7, 2, 0), title = "TV series I've watched: my tier list")

    @Test
    fun aLongList_givesTheTopTiersALineOfCovers_andTheRestThinBands() {
        val plan = BoardPicture.plan(games, titleLineCount = 2)

        val covers = plan.rows.filterIsInstance<BoardPicture.Row.Covers>()
        assertEquals(listOf("S", "A", "B"), covers.map { it.tier.label })
        assertEquals(listOf(13, 32, 46), covers.map { it.more })
        assertEquals(BoardPicture.COVER_BUDGET, covers.sumOf { it.shown.size })
        assertEquals(listOf("C", "D"), plan.rows.filterIsInstance<BoardPicture.Row.Thin>().map { it.tier.label })
        assertEquals(176, plan.ranked)
        assertTrue(plan.left > 0)
    }

    @Test
    fun aShortList_isDrawnWhole_anEmptyTierAThinBand() {
        val plan = BoardPicture.plan(series, titleLineCount = 2)

        assertEquals(0, plan.left)
        assertEquals(listOf("S", "D"), plan.rows.filterIsInstance<BoardPicture.Row.Thin>().map { it.tier.label })
        assertTrue(plan.rows.filterIsInstance<BoardPicture.Row.Covers>().all { it.small && it.more == 0 })
    }

    @Test
    fun onlyTheCoversDrawn_areFetched_neverTheDeck() {
        val drawn = BoardPicture.drawnItems(games)

        assertTrue(drawn.size <= BoardPicture.COVER_BUDGET)
        val deck = games.tiers.last().items.map { it.id }.toSet()
        assertTrue(drawn.none { it.id in deck })
        assertEquals(games.tiers[0].items.take(4), drawn.take(4))
    }

    @Test
    fun theWords_nameTheAuthorOnlyOnAListOutUnderTheirName() {
        assertEquals("by Danylo · 176 cards ranked", pictureWords(resources, games, ownName = "Danylo").byLine)
        assertEquals("11 cards ranked", pictureWords(resources, series, ownName = "Danylo").byLine)
        assertEquals("176 cards ranked", pictureWords(resources, games.copy(anonymous = true), ownName = "Danylo").byLine)
    }

    @Test
    fun theFoot_givesTheListsAddressWhole_orWhereToRankIt() {
        assertEquals("tieryourlife.com/l/jl4NPXiawyjQRbp1RT83", pictureWords(resources, games, null).footer)
        assertEquals("Rank it yourself · tieryourlife.com", pictureWords(resources, series, null).footer)
        assertEquals("tieryourlife.com", pictureWords(resources, games, null).shortFooter)
    }

    @Test
    fun theCaption_isTheLinksOwn_onAPublishedList() {
        val caption = captionFor(resources, games)

        assertTrue(caption.startsWith("My S tier: Card 1, Card 2, Card 3"))
        assertTrue(caption.endsWith("https://tieryourlife.com/l/jl4NPXiawyjQRbp1RT83"))
    }

    @Test
    fun thePicture_isFourByFive_andDarkWhateverTheTheme() {
        val picture = BoardPicture.render(games, pictureWords(resources, games, "Danylo"), emptyMap())

        assertEquals(BoardPicture.WIDTH, picture.width)
        assertEquals(BoardPicture.HEIGHT, picture.height)
        assertEquals(0xFF12151C.toInt(), picture.getPixel(4, picture.height - 4))
    }

    @Test
    fun theBigList_asItLooks() {
        val picture = BoardPicture.render(games, pictureWords(resources, games, "Danylo Petrov"), emptyMap())
        picture.captureRoboImage("src/test/screenshots/shared_picture_big.png")
    }

    @Test
    fun theShortList_asItLooks() {
        val picture = BoardPicture.render(series, pictureWords(resources, series.copy(publishedId = "x"), "Danylo Petrov"), emptyMap())
        picture.captureRoboImage("src/test/screenshots/shared_picture_small.png")
    }

    @Test
    fun theSamePicture_isKeptUntilTheBoardChanges() {
        val uri = "content://test/board.png".toUri()
        SharedPictures.keep(PictureKey(games, false, "by", "foot"), KeptPicture(uri, Bitmap.createBitmap(4, 5, Bitmap.Config.ARGB_8888)))

        assertEquals(uri, SharedPictures.get(PictureKey(games, false, "by", "foot"))?.uri)
        assertNull(SharedPictures.get(PictureKey(games.copy(title = "Changed"), false, "by", "foot")))
        SharedPictures.forget()
    }

    @Test
    fun theTap_opensTheSheetAtOnce_withTheLinkReady_andTheImageNotYet() {
        // Covers that never come: the sheet stays drawing.
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(context).components { add(Interceptor { awaitCancellation() }) }.build())
        lateinit var share: (TierList) -> Unit
        composeRule.setContent { TierYourLifeTheme { share = rememberShareBoard("Danylo") } }

        composeRule.runOnIdle { share(games) }

        composeRule.onNodeWithTag(ShareTestTags.SHEET).assertExists()
        composeRule.onNodeWithTag(ShareTestTags.SHARE_LINK).assertExists()
        composeRule.onNodeWithTag(ShareTestTags.SHARE_IMAGE).assertIsNotEnabled()
        composeRule.onNodeWithTag(ShareTestTags.CANCEL).performClick()
        composeRule.waitUntil(SHEET_GONE_MS) { composeRule.onAllNodes(hasTestTag(ShareTestTags.SHEET)).fetchSemanticsNodes().isEmpty() }
        SingletonImageLoader.reset()
    }

    @Test
    fun closedAndOpenedAgain_thePictureIsThereEachTime() {
        lateinit var share: (TierList) -> Unit
        composeRule.setContent { TierYourLifeTheme { share = rememberShareBoard("Danylo") } }

        repeat(3) {
            composeRule.runOnIdle { share(series) }
            composeRule.waitUntil(SHEET_GONE_MS) { composeRule.onAllNodes(hasTestTag(ShareTestTags.PREVIEW)).fetchSemanticsNodes().isNotEmpty() }
            composeRule.onNodeWithTag(ShareTestTags.SHEET).performTouchInput { swipeDown() }
            composeRule.waitUntil(SHEET_GONE_MS) { composeRule.onAllNodes(hasTestTag(ShareTestTags.SHEET)).fetchSemanticsNodes().isEmpty() }
        }
    }

    @Test
    fun aPictureAlreadyMade_opensShownAndReadyToSend_withoutDrawingAgain() {
        // Covers that never come: only the kept picture can fill the sheet.
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(context).components { add(Interceptor { awaitCancellation() }) }.build())
        lateinit var share: (TierList) -> Unit
        composeRule.setContent {
            TierYourLifeTheme {
                val names = LocalBoardLayoutMemory.current.namesOnCards
                val words = pictureWords(resources, games, "Danylo")
                SharedPictures.keep(
                    PictureKey(games, names, words.byLine, words.footer),
                    KeptPicture("content://test/board.png".toUri(), Bitmap.createBitmap(BoardPicture.WIDTH, BoardPicture.HEIGHT, Bitmap.Config.ARGB_8888)),
                )
                share = rememberShareBoard("Danylo")
            }
        }

        composeRule.runOnIdle { share(games) }

        composeRule.onNodeWithTag(ShareTestTags.PREVIEW).assertExists()
        composeRule.onNodeWithTag(ShareTestTags.SHARE_IMAGE).assertIsEnabled()
        composeRule.onAllNodes(hasTestTag(ShareTestTags.PROGRESS)).assertCountEquals(0)
        SharedPictures.forget()
        SingletonImageLoader.reset()
    }

    @Test
    fun aBoardNotOut_hasNoLinkToShare() {
        lateinit var share: (TierList) -> Unit
        composeRule.setContent { TierYourLifeTheme { share = rememberShareBoard() } }

        composeRule.runOnIdle { share(series) }
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasTestTag(ShareTestTags.SHEET)).fetchSemanticsNodes().isNotEmpty() }
        assertFalse(composeRule.onAllNodes(hasTestTag(ShareTestTags.SHARE_LINK)).fetchSemanticsNodes().isNotEmpty())
    }

    private companion object {
        const val SHEET_GONE_MS = 5_000L
    }
}
