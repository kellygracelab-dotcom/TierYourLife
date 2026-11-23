package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A published list's mosaic as one picture the website drew; when it does not come, the mosaic as before. */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListArtCollageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(collageServed: Boolean) {
        val served = AsyncImagePreviewHandler { ColorImage(Color.Gray.toArgb()) }
        val pictures = object : AsyncImagePreviewHandler {
            override suspend fun handle(imageLoader: ImageLoader, request: ImageRequest): AsyncImagePainter.State =
                if (collageServed || request.data != COLLAGE) {
                    served.handle(imageLoader, request)
                } else {
                    AsyncImagePainter.State.Error(null, ErrorResult(null, request, IllegalStateException("404")))
                }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides pictures) {
                TierYourLifeTheme {
                    ListArt(
                        coverImageUrl = null,
                        previewImages = listOf("https://p/1.jpg", "https://p/2.jpg"),
                        tierColors = emptyList(),
                        collageUrl = COLLAGE,
                        modifier = Modifier.size(160.dp, 100.dp),
                    )
                }
            }
        }
    }

    @Test
    fun aCollage_isTheOnePictureShown() {
        show(collageServed = true)

        composeRule.onNodeWithTag(ListArtTestTags.COLLAGE).assertExists()
        composeRule.onNodeWithTag(ListArtTestTags.MOSAIC).assertDoesNotExist()
    }

    @Test
    fun aCollageThatFails_givesWayToTheMosaic() {
        show(collageServed = false)

        composeRule.onNodeWithTag(ListArtTestTags.MOSAIC).assertExists()
        composeRule.onNodeWithTag(ListArtTestTags.COLLAGE).assertDoesNotExist()
    }

    @Test
    fun aCollage_isWarmedAsOnePicture() {
        val requests = artRequests(ArtPictures(null, listOf("a", "b", "c"), collageUrl = COLLAGE), width = 400, height = 300)

        assertEquals(listOf(ArtRequest(COLLAGE, 400, 300)), requests)
    }

    private companion object {
        const val COLLAGE = "https://tieryourlife.com/img/collage/pub-1.webp?v=7"
    }
}
