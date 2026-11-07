package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal const val MAX_STORED_EDGE_PX = 1000

private const val JPEG_QUALITY = 85

/** As much as the account's own folder takes (storage.rules). */
internal const val MAX_ACCOUNT_PICTURE_BYTES = 4L * 1024 * 1024

/** Under [MAX_ACCOUNT_PICTURE_BYTES] with room to spare. */
internal const val MAX_STILL_BYTES = 3_500_000L

private const val SMALLEST_HALVED_EDGE_PX = 125

private const val WEB_TIMEOUT_MILLIS = 10_000

@Singleton
class TierImageStore internal constructor(
    private val directory: File,
    private val openSource: (String) -> InputStream?,
    private val openWeb: (String) -> InputStream? = ::openOverHttps,
) {

    @Inject constructor(@ApplicationContext context: Context) : this(
        directory = File(context.filesDir, "tier_images"),
        openSource = { uri -> context.contentResolver.openInputStream(uri.toUri()) },
    )

    /**
     * The file name is a fresh id that means the same on a second phone; the
     * directory does not. A picture on somebody else's server has no id.
     */
    fun pictureIdOf(imageUrl: String?): String? {
        if (imageUrl == null || imageUrl.startsWith("http")) return null
        val file = File(imageUrl)
        return file.name.takeIf { it.isNotEmpty() && file.parentFile?.absoluteFile == directory.absoluteFile }
    }

    /** Where a picture with this id would live, whether or not it is here yet. */
    fun pathFor(pictureId: String): String = File(directory, pictureId).absolutePath

    fun holds(pictureId: String): Boolean = File(directory, pictureId).length() > 0

    fun sizeOf(pictureId: String): Long = File(directory, pictureId).length()

    fun read(pictureId: String): ByteArray? =
        File(directory, pictureId).takeIf { it.length() > 0 }?.readBytes()

    /** Written beside the target and moved into place: half a file would be indistinguishable from a whole one. */
    fun write(pictureId: String, bytes: ByteArray): String {
        directory.mkdirs()
        val target = File(directory, pictureId)
        val partial = File(directory, "$pictureId.part")
        partial.writeBytes(bytes)
        partial.renameTo(target)
        return target.absolutePath
    }

    fun copyToInternalStorage(sourceUri: String): String {
        directory.mkdirs()
        val target = File(directory, UUID.randomUUID().toString())
        val input = openSource(sourceUri)
            ?: throw IOException("Cannot open image source: $sourceUri")
        input.use { source ->
            target.outputStream().use { sink -> source.copyTo(sink) }
        }
        try {
            shrinkInPlace(target)
        } catch (refused: IOException) {
            target.delete()
            throw refused
        }
        return target.absolutePath
    }

    /**
     * A picture from somebody else's site, kept as the phone's own so it
     * stays when the site moves it, and syncs and publishes like a photo.
     * Null when it could not be had: too big, not there, not a picture.
     */
    suspend fun downloadToInternalStorage(url: String): String? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://")) return@withContext null
        directory.mkdirs()
        val target = File(directory, UUID.randomUUID().toString())
        try {
            val input = openWeb(url) ?: return@withContext null
            val written = input.use { source -> target.outputStream().use { sink -> copyAtMost(source, sink, MAX_ACCOUNT_PICTURE_BYTES) } }
            if (!written || !isPicture(target)) {
                target.delete()
                return@withContext null
            }
            shrinkInPlace(target)
            target.absolutePath
        } catch (exception: IOException) {
            Timber.i(exception, "Could not keep a copy of a web picture")
            target.delete()
            null
        }
    }

    fun deleteCopy(path: String) {
        val file = File(path)
        // Never delete a path outside the app-owned image directory.
        if (file.absoluteFile.parentFile == directory.absoluteFile) {
            file.delete()
        }
    }

    fun deleteOrphans(referencedPaths: Collection<String>) {
        val referenced = referencedPaths.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.absolutePath !in referenced) {
                file.delete()
            }
        }
    }
}

private fun openOverHttps(url: String): InputStream? {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = WEB_TIMEOUT_MILLIS
    connection.readTimeout = WEB_TIMEOUT_MILLIS
    connection.instanceFollowRedirects = true
    if (connection.responseCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
        connection.disconnect()
        return null
    }
    return connection.inputStream
}

/** False once more than [limit] bytes would be written: a picture that big is not a card's. */
internal fun copyAtMost(source: InputStream, sink: OutputStream, limit: Long): Boolean {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val read = source.read(buffer)
        if (read < 0) return total > 0
        total += read
        if (total > limit) return false
        sink.write(buffer, 0, read)
    }
}

/** Whether the bytes decode as a picture at all; a site's error page is not one. */
private fun isPicture(file: File): Boolean {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decode(file, bounds)
    return bounds.outWidth > 0 && bounds.outHeight > 0
}

/**
 * Compression is best effort, and the original stays when it could not be
 * smaller. Not for a GIF or a WebP: either may move, so it always becomes a
 * still of its first frame, larger or not, and one that cannot is an [IOException].
 */
internal fun shrinkInPlace(file: File, stillLimit: Long = MAX_STILL_BYTES) {
    val mayMove = mayMove(file)
    val bitmap = if (mayMove) decodeFirstFrame(file) else decodeWithinLimit(file)
    if (bitmap == null) {
        if (mayMove) throw IOException("A GIF or WebP that would not decode")
        return
    }
    val temp = File(file.parentFile, file.name + ".tmp")
    try {
        val written = if (mayMove) writeStill(bitmap, temp, stillLimit) else temp.compressFrom(bitmap, Bitmap.CompressFormat.JPEG)
        val replaced = written &&
            (mayMove || temp.length() < file.length()) &&
            runCatching { temp.copyTo(file, overwrite = true) }.isSuccess
        if (mayMove && !replaced) throw IOException("A GIF or WebP that would not become a still")
    } finally {
        temp.delete()
        bitmap.recycle()
    }
}

/**
 * A JPEG has no see-through parts, so a frame with them is a PNG, halved
 * until the account takes it; one that never fits is flattened onto white.
 */
private fun writeStill(frame: Bitmap, temp: File, limit: Long): Boolean {
    if (!frame.hasAlpha()) return temp.compressFrom(frame, Bitmap.CompressFormat.JPEG)
    var still = frame
    try {
        var fits = temp.compressFrom(still, Bitmap.CompressFormat.PNG) && temp.length() <= limit
        while (!fits && maxOf(still.width, still.height) / 2 >= SMALLEST_HALVED_EDGE_PX) {
            val half = Bitmap.createScaledBitmap(still, still.width / 2, still.height / 2, true)
            if (still !== frame) still.recycle()
            still = half
            fits = temp.compressFrom(still, Bitmap.CompressFormat.PNG) && temp.length() <= limit
        }
        return fits || temp.compressOnWhite(still)
    } finally {
        if (still !== frame) still.recycle()
    }
}

private fun File.compressOnWhite(still: Bitmap): Boolean {
    val flat = Bitmap.createBitmap(still.width, still.height, Bitmap.Config.ARGB_8888)
    return try {
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(still, 0f, 0f, null)
        }
        compressFrom(flat, Bitmap.CompressFormat.JPEG)
    } finally {
        flat.recycle()
    }
}

private fun File.compressFrom(bitmap: Bitmap, format: Bitmap.CompressFormat): Boolean =
    runCatching { outputStream().use { out -> bitmap.compress(format, JPEG_QUALITY, out) } }.getOrDefault(false) && length() > 0

private val GIF = "GIF8".toByteArray(Charsets.US_ASCII)
private val RIFF = "RIFF".toByteArray(Charsets.US_ASCII)
private val WEBP = "WEBP".toByteArray(Charsets.US_ASCII)
private const val WEBP_TAG_AT = 8
private const val HEADER_BYTES = 12

/** By the bytes, as the server judges them: a name or a MIME type says nothing about what is inside. */
private fun mayMove(file: File): Boolean {
    val head = ByteArray(HEADER_BYTES)
    val read = file.inputStream().use { it.read(head) }
    if (read < HEADER_BYTES) return read >= GIF.size && head.startsWith(GIF)
    return head.startsWith(GIF) || (head.startsWith(RIFF) && head.copyOfRange(WEBP_TAG_AT, HEADER_BYTES).contentEquals(WEBP))
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean = copyOfRange(0, prefix.size).contentEquals(prefix)

private fun decodeWithinLimit(file: File): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

    return runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val longestEdge = maxOf(info.size.width, info.size.height)
            if (longestEdge > MAX_STORED_EDGE_PX) {
                decoder.setTargetSampleSize(sampleSizeFor(longestEdge))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()
}

/** BitmapFactory, on every API level: it reads the first frame of a GIF or a WebP. */
private fun decodeFirstFrame(file: File): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    decode(file, bounds)
    val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
    if (longestEdge <= 0) return null
    return decode(file, BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(longestEdge) })
}

/** Through a stream closed here: a file still held open could be neither replaced nor deleted. */
private fun decode(file: File, options: BitmapFactory.Options): Bitmap? =
    runCatching { file.inputStream().use { BitmapFactory.decodeStream(it, null, options) } }.getOrNull()

internal fun sampleSizeFor(longestEdge: Int): Int {
    var sample = 1
    while (longestEdge / (sample * 2) >= MAX_STORED_EDGE_PX) {
        sample *= 2
    }
    return sample
}
