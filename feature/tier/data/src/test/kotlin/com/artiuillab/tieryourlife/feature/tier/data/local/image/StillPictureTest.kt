package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Base64

/** A picked GIF or WebP may move; what goes up to the account is always a still, on every API level. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StillPictureTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    @Config(sdk = [26])
    fun anAnimatedGif_belowPie_becomesAStill() = assertStill(ANIMATED_GIF)

    @Test
    @Config(sdk = [35])
    fun anAnimatedGif_becomesAStill_evenWhenTheStillIsLarger() = assertStill(ANIMATED_GIF)

    @Test
    @Config(sdk = [26])
    fun anAnimatedWebp_belowPie_becomesAStill() = assertStill(ANIMATED_WEBP)

    @Test
    @Config(sdk = [35])
    fun anAnimatedWebp_becomesAStill() = assertStill(ANIMATED_WEBP)

    @Test
    @Config(sdk = [35])
    fun aSeeThroughWebp_staysSeeThrough_asAPng() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        val webp = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it) }.toByteArray()

        val stored = File(store(webp).copyToInternalStorage("content://gallery/clear")).readBytes()

        assertEquals("PNG", String(stored, 1, 3, Charsets.US_ASCII))
    }

    // Better no picture than a moving one: the copy is not kept, and the web address stays on the card.
    @Test
    @Config(sdk = [35])
    fun aGifThatWillNotDecode_isNotKept() = runBlocking {
        val broken = "GIF89a, and then nothing a decoder could read".toByteArray()

        val failed = runCatching { store(broken).copyToInternalStorage("content://gallery/broken") }.exceptionOrNull()

        assertTrue(failed is IOException)
        assertNull(store(broken, web = broken).downloadToInternalStorage("https://example.test/broken.gif"))
        assertEquals("nothing left behind", emptyList<String>(), File(folder.root, "pictures").list().orEmpty().toList())
    }

    @Test
    @Config(sdk = [26])
    fun aJpegBelowPie_isLeftAsItCame() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()

        val stored = File(store(jpeg).copyToInternalStorage("content://gallery/photo")).readBytes()

        assertTrue(jpeg.contentEquals(stored))
    }

    private fun assertStill(original: ByteArray) {
        val stored = File(store(original).copyToInternalStorage("content://gallery/moving")).readBytes()

        assertTrue("a JPEG or a PNG", stored.isJpeg() || String(stored, 1, 3, Charsets.US_ASCII) == "PNG")
        assertNotNull("decodes", BitmapFactory.decodeByteArray(stored, 0, stored.size))
    }

    private fun store(source: ByteArray, web: ByteArray? = null) = TierImageStore(
        directory = File(folder.root, "pictures"),
        openSource = { ByteArrayInputStream(source) },
        openWeb = { web?.let(::ByteArrayInputStream) },
    )
}

private fun ByteArray.isJpeg() = size > 2 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte()

// Two frames of 8 by 8, red then blue.
private val ANIMATED_GIF = Base64.getDecoder().decode(
    "R0lGODlhCAAIAIEAANweHgAAAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQACgAAACwAAAAACAAIAAAIDwABCBxIsKDBgwgTKkwYEAAh" +
        "+QQBCgABACwAAAAACAAIAIEeHtwAAAAAAAAAAAAIDwABCBxIsKDBgwgTKkwYEAA7",
)

private val ANIMATED_WEBP = Base64.getDecoder().decode(
    "UklGRogAAABXRUJQVlA4WAoAAAACAAAABwAABwAAQU5JTQYAAAAAAAAAAABBTk1GKgAAAAAAAAAAAAcAAAcAAGQAAAJWUDhMEQAAAC8HwAEAB1CP" +
        "ctej/4GI6H8AAEFOTUYqAAAAAAAAAAAABwAABwAAZAAAAFZQOEwRAAAALwfAAQAHUI96lLv/gYjofwAA",
)
