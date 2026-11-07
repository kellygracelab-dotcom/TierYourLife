package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/** A see-through still is a PNG, and a PNG of 1999 px can be several times what the account's folder takes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SeeThroughStillSizeTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun aLargeSeeThroughStill_isHalvedUntilTheAccountTakesIt() {
        val file = seeThroughNoise(edge = 1999)

        shrinkInPlace(file)

        val stored = file.readBytes()
        assertEquals("PNG", String(stored, 1, 3, Charsets.US_ASCII))
        assertTrue("${stored.size} bytes", stored.size <= MAX_STILL_BYTES)
        assertTrue(edgeOf(file) < 1999)
    }

    @Test
    fun oneThatFitsAtNoSize_isFlattenedOntoWhite_asAJpeg() {
        val file = seeThroughNoise(edge = 400)

        shrinkInPlace(file, stillLimit = 1)

        val stored = file.readBytes()
        assertTrue("a JPEG", stored[0] == 0xFF.toByte() && stored[1] == 0xD8.toByte())
        val corner = BitmapFactory.decodeByteArray(stored, 0, stored.size).getPixel(2, 2)
        assertTrue("white where it was see-through", minOf(Color.red(corner), Color.green(corner), Color.blue(corner)) > 230)
    }

    private fun seeThroughNoise(edge: Int): File {
        val random = Random(7)
        val pixels = IntArray(edge * edge) { at ->
            val x = at % edge
            val y = at / edge
            if (x < CLEAR_CORNER && y < CLEAR_CORNER) {
                Color.TRANSPARENT
            } else {
                Color.argb(random.nextInt(1, 256), random.nextInt(256), random.nextInt(256), random.nextInt(256))
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, edge, edge, Bitmap.Config.ARGB_8888)
        return File(folder.root, "noise.webp").apply {
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, it) }
        }
    }

    private fun edgeOf(file: File): Int {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        file.inputStream().use { BitmapFactory.decodeStream(it, null, bounds) }
        return maxOf(bounds.outWidth, bounds.outHeight)
    }
}

private const val CLEAR_CORNER = 64
