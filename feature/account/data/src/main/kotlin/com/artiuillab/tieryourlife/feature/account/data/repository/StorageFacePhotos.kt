package com.artiuillab.tieryourlife.feature.account.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.account.data.picture.squareFaceJpeg
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun upload(sourceUri: String): Result<String> = runSuspendCatching {
        val user = auth.currentUser?.takeIf { !it.isAnonymous } ?: throw IllegalStateException("Nobody is signed in")
        val bytes = withContext(Dispatchers.IO) { squareFaceJpeg(context.contentResolver, sourceUri.toUri()) }
            ?: throw IOException("The photo could not be decoded")
        val pictureId = UUID.randomUUID().toString()
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        storage.reference.child("users/${user.uid}/pictures/$pictureId").putBytes(bytes, metadata).await()
        pictureId
    }
}
