package com.artiuillab.tieryourlife.feature.community.presentation.explore

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.board.components.ListArtTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Designer, 23.11: the server's collage is drawn 16:10, so the card's picture
 * is 16:10 too, and no column of it is cut, at a phone's two widths and a tablet's.
 */
@OptIn(ExperimentalCoilApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp")
class CollageShapeTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** The website's collage: [columns] equal columns, each its own colour, 480×300. */
    private fun collage(columns: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(COLLAGE_WIDTH, COLLAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val column = COLLAGE_WIDTH / columns.toFloat()
        repeat(columns) { i ->
            canvas.drawRect(i * column, 0f, (i + 1) * column, COLLAGE_HEIGHT.toFloat(), Paint().apply { color = COLOURS[i] })
        }
        return bitmap
    }

    private fun columnsSeen(columns: Int, cardWidth: Int): List<Int> {
        val served = AsyncImagePreviewHandler { collage(columns).asImage() }
        val summary = PublishedListSummary(
            id = "l-1",
            title = "Films",
            authorUid = "u",
            authorName = "A",
            category = "film-tv",
            itemCount = 6,
            previewImages = List(columns) { "https://p/$it.jpg" },
            collageUrl = "https://tieryourlife.com/img/collage/l-1.webp",
            updatedAtMillis = 0,
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true, LocalAsyncImagePreviewHandler provides served) {
                TierYourLifeTheme { FeedCard(summary, onClick = {}, onLongClick = {}, modifier = Modifier.width(cardWidth.dp)) }
            }
        }
        val picture = composeRule.onNodeWithTag(ListArtTestTags.COLLAGE, useUnmergedTree = true).captureToImage().asAndroidBitmap()
        assertEquals(LIST_ART_ASPECT, picture.width / picture.height.toFloat(), ASPECT_SLACK)
        // The width each colour takes along the middle line: the columns as drawn.
        val row = IntArray(picture.width) { picture.getPixel(it, picture.height / 2) }
        return COLOURS.take(columns).map { colour -> row.count { close(it, colour) } }
    }

    private fun assertEqualColumns(columns: Int, cardWidth: Int) {
        val seen = columnsSeen(columns, cardWidth)
        val each = seen.sum() / columns.toFloat()
        assertTrue("$columns columns at ${cardWidth}dp: $seen", seen.all { abs(it - each) <= COLUMN_SLACK_PX })
        assertTrue("$columns columns at ${cardWidth}dp: $seen", seen.first() > 0 && seen.last() > 0)
    }

    // A two-column grid at 320 dp and 411 dp, and four columns on a tablet.
    @Test fun six_at320() = assertEqualColumns(6, PHONE_SMALL_CARD)

    @Test fun four_at320() = assertEqualColumns(4, PHONE_SMALL_CARD)

    @Test fun two_at320() = assertEqualColumns(2, PHONE_SMALL_CARD)

    @Test fun six_at411() = assertEqualColumns(6, PHONE_CARD)

    @Test fun four_at411() = assertEqualColumns(4, PHONE_CARD)

    @Test fun two_at411() = assertEqualColumns(2, PHONE_CARD)

    @Test fun six_onATablet() = assertEqualColumns(6, TABLET_CARD)

    @Test fun four_onATablet() = assertEqualColumns(4, TABLET_CARD)

    @Test fun two_onATablet() = assertEqualColumns(2, TABLET_CARD)

    private companion object {
        const val COLLAGE_WIDTH = 480
        const val COLLAGE_HEIGHT = 300
        const val PHONE_SMALL_CARD = 146
        const val PHONE_CARD = 190
        const val TABLET_CARD = 240
        const val ASPECT_SLACK = 0.02f
        const val COLUMN_SLACK_PX = 3f
        val COLOURS = listOf(0xFFE53935, 0xFF43A047, 0xFF1E88E5, 0xFFFDD835, 0xFF8E24AA, 0xFF00ACC1).map { it.toInt() }

        fun close(a: Int, b: Int): Boolean =
            listOf(16, 8, 0).all { shift -> abs((a shr shift and 0xFF) - (b shr shift and 0xFF)) < 12 }
    }
}
