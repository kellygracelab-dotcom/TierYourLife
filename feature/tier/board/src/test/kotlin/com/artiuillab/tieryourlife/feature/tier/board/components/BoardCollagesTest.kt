package com.artiuillab.tieryourlife.feature.tier.board.components

import android.graphics.Bitmap
import android.graphics.Color
import com.artiuillab.tieryourlife.feature.tier.domain.model.CoverFocus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** A board's collage is drawn once, kept on the phone, drawn again only when its key changes, and the space stays bounded. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class BoardCollagesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val drawn = mutableListOf<BoardCollage>()
    private var picturesCome = true

    private fun store(dir: File = folder.root, maxFiles: Int = 300) = BoardCollages(dir, maxFiles = maxFiles, io = Dispatchers.Unconfined) { collage ->
        drawn += collage
        if (picturesCome) Bitmap.createBitmap(collage.width, collage.height, Bitmap.Config.ARGB_8888) else null
    }

    private fun collage(board: String = "7", images: List<String> = listOf("a", "b", "c")) =
        requireNotNull(boardCollageOf(board, images, emptyMap(), 120, 89))

    @Test
    fun theSecondOpen_readsTheKeptPicture_withoutDrawingIt() = runBlocking {
        val first = store().make(collage())
        assertNotNull(first)

        // A new store is the app opened again: only the file is left.
        val reopened = store()
        assertNull(reopened.known(collage()))
        assertEquals(first, reopened.find(collage()))
        assertEquals(first, reopened.make(collage()))
        assertEquals(first, reopened.known(collage()))
        assertEquals(1, drawn.size)
    }

    @Test
    fun changingTheFirstCards_drawsItAgain_andDropsTheOldOne() = runBlocking {
        val collages = store()
        val before = requireNotNull(collages.make(collage()))

        val after = requireNotNull(collages.make(collage(images = listOf("c", "b", "a"))))

        assertEquals(2, drawn.size)
        assertFalse(before.exists())
        assertTrue(after.exists())
        assertNull(collages.known(collage()))
    }

    @Test
    fun anotherShape_ofTheSameBoard_isKeptBesideIt() = runBlocking {
        val collages = store()
        val tile = requireNotNull(collages.make(collage()))
        val row = requireNotNull(collages.make(requireNotNull(boardCollageOf("7", listOf("a", "b", "c"), emptyMap(), 56, 56))))

        assertTrue(tile.exists())
        assertTrue(row.exists())
    }

    @Test
    fun aPictureThatDoesNotCome_keepsNothing_soTheMosaicStays() = runBlocking {
        picturesCome = false
        val collages = store()

        assertNull(collages.make(collage()))
        assertTrue(folder.root.listFiles().orEmpty().isEmpty())

        picturesCome = true
        assertNotNull(collages.make(collage()))
    }

    @Test
    fun pastTheLimit_theLeastRecentlyShownGo() = runBlocking {
        val collages = store(maxFiles = 2)
        val first = requireNotNull(collages.make(collage(board = "1")))
        first.setLastModified(1_000)
        val second = requireNotNull(collages.make(collage(board = "2")))
        second.setLastModified(2_000)

        val third = requireNotNull(collages.make(collage(board = "3")))

        assertFalse(first.exists())
        assertTrue(second.exists())
        assertTrue(third.exists())
        assertNull(collages.known(collage(board = "1")))
    }

    @Test
    fun aKeptPictureThatWillNotOpen_isForgotten_andDrawnAgain() = runBlocking {
        val collages = store()
        val kept = requireNotNull(collages.make(collage()))

        collages.forget(collage())

        assertFalse(kept.exists())
        assertNull(collages.known(collage()))
        assertNotNull(collages.make(collage()))
        assertEquals(2, drawn.size)
    }

    @Test
    fun gluing_cutsEachPictureIntoItsCell_andLeavesTheGapsClear() {
        val collage = requireNotNull(boardCollageOf("7", listOf("red", "blue"), mapOf("blue" to CoverFocus(0f, 0f, 2f)), 200, 100))
        val cells = collageCells(2, 200, 100)
        val red = solid(Color.RED, 300, 300)
        val blue = solid(Color.BLUE, 50, 400)

        val glued = glue(listOf(red, blue), cells, collage)

        assertEquals(200, glued.width)
        assertEquals(100, glued.height)
        assertEquals(Color.RED, glued.getPixel(cells[0].left + 5, 50))
        assertEquals(Color.BLUE, glued.getPixel(cells[1].right - 5, 50))
        assertEquals(0, Color.alpha(glued.getPixel(cells[0].right, 50)))
    }

    private fun solid(color: Int, width: Int, height: Int) =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
}
