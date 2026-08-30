package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal const val MAX_STORED_EDGE_PX = 1000

private const val JPEG_QUALITY = 85

@Singleton
class TierImageStore internal constructor(
    private val directory: File,
    private val openSource: (String) -> InputStream?,
) {

    @Inject constructor(@ApplicationContext context: Context) : this(
        directory = File(context.filesDir, "tier_images"),
        openSource = { uri -> context.contentResolver.openInputStream(uri.toUri()) },
    )

    /**
     * What a picture is called, wherever it happens to sit.
     *
     * The file name is already a fresh id and already means the same thing on
     * a second phone; the directory around it does not, so this is the only
     * part of a path worth sending anywhere. A picture on somebody else's
     * server is not one of ours and has no id.
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

    /**
     * Written beside the target and moved into place, so a download that stops
     * halfway leaves nothing behind that looks like a picture. Half a file
     * would be indistinguishable from a whole one that is simply here.
     */
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
        shrinkInPlace(target)
        return target.absolutePath
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

internal fun shrinkInPlace(file: File) {
    val bitmap = decodeWithinLimit(file) ?: return
    val temp = File(file.parentFile, file.name + ".tmp")

    // Compression is best effort; the original copy remains usable on failure.
    try {
        val written = temp.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        if (written && temp.length() > 0 && temp.length() < file.length()) {
            temp.copyTo(file, overwrite = true)
        }
    } catch (_: IOException) {
    } finally {
        temp.delete()
        bitmap.recycle()
    }
}

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

internal fun sampleSizeFor(longestEdge: Int): Int {
    var sample = 1
    while (longestEdge / (sample * 2) >= MAX_STORED_EDGE_PX) {
        sample *= 2
    }
    return sample
}
