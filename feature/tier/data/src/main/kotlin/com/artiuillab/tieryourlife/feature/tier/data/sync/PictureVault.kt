package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
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
        return runCatching { reference.putBytes(bytes).await() }.isSuccess
    }

    override suspend fun get(pictureId: String): ByteArray? {
        val reference = referenceTo(pictureId) ?: return null
        return runCatching { reference.getBytes(MAX_PICTURE_BYTES).await() }.getOrNull()
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
