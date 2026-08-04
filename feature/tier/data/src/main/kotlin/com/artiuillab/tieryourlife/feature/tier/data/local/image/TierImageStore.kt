package com.artiuillab.tieryourlife.feature.tier.data.local.image

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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

    // Copies the source into internal storage and returns the path of the copy.
    fun copyToInternalStorage(sourceUri: String): String {
        directory.mkdirs()
        val target = File(directory, UUID.randomUUID().toString())
        val input = openSource(sourceUri)
            ?: throw IOException("Cannot open image source: $sourceUri")
        input.use { source ->
            target.outputStream().use { sink -> source.copyTo(sink) }
        }
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
