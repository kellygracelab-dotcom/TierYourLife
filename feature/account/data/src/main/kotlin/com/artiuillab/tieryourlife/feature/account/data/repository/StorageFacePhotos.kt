package com.artiuillab.tieryourlife.feature.account.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.account.data.picture.squareFaceJpeg
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val FACES_FOLDER = "faces"

/**
 * Into the same folder the board pictures use, so `storage.rules` already
 * allows it: this person's own, images only, under four megabytes. The face
 * function reads it from there.
 */
@Singleton
class StorageFacePhotos @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : FacePhotos {

    override suspend fun prepare(sourceUri: String, crop: FaceCrop?): Result<String> = runSuspendCatching {
        withContext(Dispatchers.IO) {
            val folder = File(context.cacheDir, FACES_FOLDER).apply { mkdirs() }
            // Only the face about to be shown is worth keeping; earlier ones were shown and are done with.
            folder.listFiles()?.forEach { it.delete() }
            val download = if (sourceUri.startsWith("https://")) downloaded(sourceUri, folder) else null
            val readable = download?.let(Uri::fromFile) ?: sourceUri.toUri()
            val bytes = try {
                squareFaceJpeg(context.contentResolver, readable, crop)
            } finally {
                download?.delete()
            } ?: throw IOException("The picture could not be decoded")
            val face = File(folder, "${UUID.randomUUID()}.jpg").apply { writeBytes(bytes) }
            Uri.fromFile(face).toString()
        }
    }

    override suspend fun upload(preparedUri: String): Result<String> = runSuspendCatching {
        val user = auth.currentUser?.takeIf { !it.isAnonymous } ?: throw IllegalStateException("Nobody is signed in")
        val bytes = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(preparedUri.toUri())?.use { it.readBytes() }
        } ?: throw IOException("The prepared face is gone")
        val pictureId = UUID.randomUUID().toString()
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        storage.reference.child("users/${user.uid}/pictures/$pictureId").putBytes(bytes, metadata).await()
        pictureId
    }

    /** A poster lives on the web; cutting it needs the bytes here first. */
    private fun downloaded(address: String, folder: File): File {
        val file = File(folder, "source-${UUID.randomUUID()}")
        URL(address).openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        return file
    }
}
