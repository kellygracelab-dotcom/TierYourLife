package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// The longest side a stored copy may have. A tile is 44x64dp, so even on a very dense screen
// it is drawn at a few hundred pixels; anything beyond this is stored, backed up and decoded
// for nothing.
internal const val MAX_STORED_EDGE_PX = 1000

private const val JPEG_QUALITY = 85

// A user-picked picture is copied into app-internal storage and the database
// stores the path of the copy: a gallery Uri would silently break the moment
// the user deletes the original photo.
@Singleton
class TierImageStore internal constructor(
    private val directory: File,
    private val openSource: (String) -> InputStream?,
) {

    @Inject constructor(@ApplicationContext context: Context) : this(
        directory = File(context.filesDir, "tier_images"),
        openSource = { uri -> context.contentResolver.openInputStream(Uri.parse(uri)) },
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

    // Deletes a copy; paths outside our directory (remote poster URLs) are ignored.
    fun deleteCopy(path: String) {
        val file = File(path)
        if (file.absoluteFile.parentFile == directory.absoluteFile) {
            file.delete()
        }
    }

    // Crash safety net: removes files no database row points at any more.
    fun deleteOrphans(referencedPaths: Collection<String>) {
        val referenced = referencedPaths.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.absolutePath !in referenced) {
                file.delete()
            }
        }
    }
}

// Auto Backup gives an app 25 MB in total and then stops backing it up altogether, silently,
// so a handful of full-resolution camera photos would cost the user the backup of everything
// else — the database included. Storing what is actually drawn rather than what the camera
// produced keeps hundreds of items inside that quota.
//
// Anything that fails to decode is left exactly as copied. A file this cannot read is still
// the user's file, and the existing copy on disk is untouched until a smaller one is proven.
internal fun shrinkInPlace(file: File) {
    val bitmap = decodeWithinLimit(file) ?: return
    val temp = File(file.parentFile, file.name + ".tmp")

    try {
        val written = temp.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        // Re-encoding a picture that was already small can make it bigger, so keep whichever
        // file is smaller rather than assuming this step always improves things.
        if (written && temp.length() > 0 && temp.length() < file.length()) {
            temp.copyTo(file, overwrite = true)
        }
    } catch (_: IOException) {
        // The copy is already on disk and untouched, so there is nothing to roll back.
    } finally {
        temp.delete()
        bitmap.recycle()
    }
}

// ImageDecoder applies EXIF orientation itself, which is what makes re-encoding safe at all:
// a verbatim copy carries its EXIF along for the image loader to apply, while a re-encoded one
// comes out rotated unless the rotation is baked in here. Below API 28 there is no
// ImageDecoder, and rather than hand-roll EXIF handling the copy is left at full size — those
// devices keep exactly the behaviour they have today.
private fun decodeWithinLimit(file: File): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

    return runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val longestEdge = maxOf(info.size.width, info.size.height)
            if (longestEdge > MAX_STORED_EDGE_PX) {
                decoder.setTargetSampleSize(sampleSizeFor(longestEdge))
            }
            // compress() cannot read a hardware bitmap, which is what the default allocator
            // returns for an immutable decode.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()
}

// setTargetSampleSize only honours powers of two, so this picks the largest one that still
// leaves the longest edge at or above the limit rather than overshooting below it.
internal fun sampleSizeFor(longestEdge: Int): Int {
    var sample = 1
    while (longestEdge / (sample * 2) >= MAX_STORED_EDGE_PX) {
        sample *= 2
    }
    return sample
}
