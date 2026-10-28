package com.artiuillab.tieryourlife.feature.community.data.picture

import android.content.Context
import androidx.core.net.toUri
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionPictures
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
 * Into the folder board pictures and faces already use, so `storage.rules`
 * lets it in: this person's own, images only, under four megabytes. The
 * proxy copies it out of reach of the daily sweep once it has the proposal.
 */
@Singleton
class StorageSectionPictures @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) : SectionPictures {

    override suspend fun measure(sourceUri: String): Result<PictureSize> = runSuspendCatching {
        withContext(Dispatchers.IO) { uprightSize(context.contentResolver, sourceUri.toUri()) }
            ?: throw IOException("The picture could not be read")
    }

    override suspend fun upload(sourceUri: String, crop: PictureCrop): Result<String> = runSuspendCatching {
        val user = auth.currentUser?.takeIf { !it.isAnonymous } ?: throw ProposalRefused(ProposalError.NotSignedIn)
        val bytes = withContext(Dispatchers.Default) { sectionPictureWebp(context.contentResolver, sourceUri.toUri(), crop) }
            ?: throw ProposalRefused(ProposalError.BadPicture(PictureProblem.TooSmall))
        val pictureId = UUID.randomUUID().toString()
        val metadata = StorageMetadata.Builder().setContentType("image/webp").build()
        storage.reference.child("users/${user.uid}/pictures/$pictureId").putBytes(bytes, metadata).await()
        pictureId
    }
}
