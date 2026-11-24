package com.artiuillab.tieryourlife.feature.tier.board.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ImageRequest
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

/** A tile in Your lists shows its board's pictures as one, glued on the phone; until then, or when it cannot be, the mosaic. */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ListArtBoardCollageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val asked = mutableListOf<Any?>()
    private var drawings = 0

    private fun store(picturesCome: Boolean = true) = BoardCollages(folder.root, io = Dispatchers.Unconfined) { collage ->
        drawings++
        if (picturesCome) Bitmap.createBitmap(collage.width, collage.height, Bitmap.Config.ARGB_8888) else null
    }

    private fun show(collages: BoardCollages) {
        val pictures = object : AsyncImagePreviewHandler {
            override suspend fun handle(imageLoader: ImageLoader, request: ImageRequest): AsyncImagePainter.State {
                asked += request.data
                return AsyncImagePreviewHandler { ColorImage(Color.Gray.toArgb()) }.handle(imageLoader, request)
            }
        }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides pictures,
                LocalBoardCollages provides collages,
            ) {
                TierYourLifeTheme {
                    ListArt(
                        coverImageUrl = null,
                        previewImages = PICTURES,
                        tierColors = emptyList(),
                        boardId = "7",
                        modifier = Modifier.size(WIDTH.dp, HEIGHT.dp),
                    )
                }
            }
        }
    }

    @Test
    fun theFirstOpen_drawsTheCollage_andShowsIt() {
        show(store())

        composeRule.waitUntil { composeRule.onAllNodes(hasBoardCollage).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(ListArtTestTags.MOSAIC).assertDoesNotExist()
        assertEquals(1, drawings)
    }

    @Test
    fun theSecondOpen_showsOnePicture_withoutAskingForTheSeparateOnes() {
        val kept = keptFromTheFirstOpen()

        show(store())

        composeRule.onNodeWithTag(ListArtTestTags.BOARD_COLLAGE).assertExists()
        composeRule.onNodeWithTag(ListArtTestTags.MOSAIC).assertDoesNotExist()
        composeRule.waitUntil { asked.isNotEmpty() }
        assertEquals(listOf<Any?>(kept), asked.distinct())
        assertEquals(1, drawings)
    }

    @Test
    fun whenItCannotBeDrawn_theMosaicStays() {
        show(store(picturesCome = false))

        composeRule.onNodeWithTag(ListArtTestTags.MOSAIC).assertExists()
        composeRule.onNodeWithTag(ListArtTestTags.BOARD_COLLAGE).assertDoesNotExist()
        composeRule.waitUntil { PICTURES.all { it in asked } }
        assertEquals(1, drawings)
    }

    /** What the first open left on the phone, for a tile this size, as the app opened again finds it. */
    private fun keptFromTheFirstOpen(): File = runBlocking {
        val density = RuntimeEnvironment.getApplication().resources.displayMetrics.density
        val collage = requireNotNull(boardCollageOf("7", PICTURES, emptyMap(), (WIDTH * density).roundToInt(), (HEIGHT * density).roundToInt()))
        requireNotNull(store().make(collage))
    }

    private companion object {
        val PICTURES = listOf("https://p/1.jpg", "https://p/2.jpg", "https://p/3.jpg")
        const val WIDTH = 162
        const val HEIGHT = 120
        val hasBoardCollage = hasTestTag(ListArtTestTags.BOARD_COLLAGE)
    }
}
