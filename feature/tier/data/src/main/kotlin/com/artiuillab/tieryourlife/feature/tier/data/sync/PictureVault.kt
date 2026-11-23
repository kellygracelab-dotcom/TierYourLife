package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only thing that touches a Google service directly: a picture is
 * megabytes, and a function moving megabytes it does nothing with is a pipe
 * billed by the second. `storage.rules` opens one door -- this person's
 * folder, images, four megabytes.
 */
interface Pictures {

    suspend fun put(pictureId: String, bytes: ByteArray): Boolean

    suspend fun get(pictureId: String): ByteArray?
}

@Singleton
class PictureVault @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : Pictures {

    override suspend fun put(pictureId: String, bytes: ByteArray): Boolean {
        val reference = referenceTo(pictureId) ?: return false
        return runSuspendCatching { reference.putBytes(bytes, pictureMetadata()).await() }
            .onFailure { Timber.w(it, "Uploading a picture failed") }
            .isSuccess
    }

    override suspend fun get(pictureId: String): ByteArray? {
        val reference = referenceTo(pictureId) ?: return null
        return runSuspendCatching { reference.getBytes(MAX_PICTURE_BYTES).await() }
            .onFailure { error ->
                val failure = downloadFailureOf(error)
                if (failure != null) Timber.w(failure, "Downloading a picture failed") else Timber.i("Downloading a picture missed")
            }
            .getOrNull()
    }

    /** Null for a guest: boards are only kept for somebody signed in, and asking is not an error. */
    private fun referenceTo(pictureId: String) = auth.currentUser
        ?.takeIf { !it.isAnonymous }
        ?.let { user -> storage.reference.child("users/${user.uid}/pictures/$pictureId") }

    private companion object {
        /** The same ceiling the rules enforce on the way in. */
        const val MAX_PICTURE_BYTES = 4L * 1024 * 1024
    }
}

/**
 * A download the server answered with an error, as Crashlytics may see it: the
 * HTTP and Storage codes only, since its message becomes the issue's subtitle
 * on the owner's screen and no path, uid or picture id belongs there. Null for
 * an ordinary miss -- a picture not uploaded yet, or no connection at all.
 */
internal fun downloadFailureOf(error: Throwable): PictureDownloadFailed? {
    val storage = error as? StorageException ?: return null
    if (storage.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND || storage.httpResultCode <= 0) return null
    return PictureDownloadFailed(storage.httpResultCode, storage.errorCode)
}

internal class PictureDownloadFailed(http: Int, storage: Int) : RuntimeException("Storage answered HTTP $http, error $storage")

/** What the store writes. The rules admit images only, and bytes sent without a type arrive as an octet stream and are turned away. */
internal const val PICTURE_CONTENT_TYPE = "image/jpeg"

internal fun pictureMetadata(): StorageMetadata = StorageMetadata.Builder().setContentType(PICTURE_CONTENT_TYPE).build()
