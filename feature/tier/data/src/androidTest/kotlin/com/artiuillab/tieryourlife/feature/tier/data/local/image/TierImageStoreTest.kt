package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class TierImageStoreTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.cacheDir, "tier_image_store_test_${System.nanoTime()}")
        directory.mkdirs()
    }

    @Test
    fun aCameraSizedPhoto_isStoredWithinTheEdgeLimit() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val original = jpegBytes(width = 4000, height = 3000)
        val store = TierImageStore(directory) { ByteArrayInputStream(original) }

        val storedPath = store.copyToInternalStorage("content://gallery/photo")

        val stored = BitmapFactory.decodeFile(storedPath)
        assertTrue(
            "longest edge was ${maxOf(stored.width, stored.height)}",
            maxOf(stored.width, stored.height) <= MAX_STORED_EDGE_PX,
        )
        assertTrue(
            "stored ${File(storedPath).length()} bytes vs original ${original.size}",
            File(storedPath).length() < original.size,
        )
    }

    // A small picture must not be re-encoded into something bigger than it started.
    @Test
    fun anAlreadySmallPicture_isNeverGrownByBeingStored() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        val original = jpegBytes(width = 120, height = 160)
        val store = TierImageStore(directory) { ByteArrayInputStream(original) }

        val storedPath = store.copyToInternalStorage("content://gallery/small")

        assertTrue(File(storedPath).length() <= original.size.toLong())
    }

    // Bytes this cannot decode are still the user's file: they are stored verbatim rather
    // than rejected. Several other tests in this module rely on that by feeding plain text.
    @Test
    fun undecodableBytes_areStoredExactlyAsTheyArrived() {
        val original = "not an image at all".toByteArray()
        val store = TierImageStore(directory) { ByteArrayInputStream(original) }

        val storedPath = store.copyToInternalStorage("content://gallery/nonsense")

        assertArrayEquals(original, File(storedPath).readBytes())
    }

    @Test
    fun sampleSize_staysAtOrAboveTheLimitRatherThanOvershooting() {
        assertEquals(1, sampleSizeFor(MAX_STORED_EDGE_PX))
        assertEquals(1, sampleSizeFor(MAX_STORED_EDGE_PX * 2 - 1))
        assertEquals(2, sampleSizeFor(MAX_STORED_EDGE_PX * 2))
        assertEquals(4, sampleSizeFor(MAX_STORED_EDGE_PX * 4))
    }

    private fun jpegBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // A flat fill compresses to almost nothing, which would make the size assertions
        // meaningless; noise keeps the encoded original genuinely large.
        val pixels = IntArray(width * height) { (it * 2654435761u.toInt()) or 0xFF000000.toInt() }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}
