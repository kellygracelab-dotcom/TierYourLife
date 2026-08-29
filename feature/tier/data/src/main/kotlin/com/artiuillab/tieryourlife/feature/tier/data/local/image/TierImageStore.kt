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
