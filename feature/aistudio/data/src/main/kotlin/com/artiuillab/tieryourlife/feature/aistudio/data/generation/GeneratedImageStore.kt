package com.artiuillab.tieryourlife.feature.aistudio.data.generation

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_SUBDIRECTORY = "aistudio"

@Singleton
class GeneratedImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageBytesStore {

    override fun save(bytes: ByteArray): String {
        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { output -> output.write(bytes) }
        return file.absolutePath
    }

    override fun clear() {
        File(context.cacheDir, CACHE_SUBDIRECTORY).listFiles()?.forEach { file -> file.delete() }
    }

    override fun delete(uri: String) {
        val path = uri.toUri().path ?: return
        val file = File(path)
        val directory = File(context.cacheDir, CACHE_SUBDIRECTORY)
        if (file.absoluteFile.parentFile == directory.absoluteFile) {
            file.delete()
        }
    }
}
