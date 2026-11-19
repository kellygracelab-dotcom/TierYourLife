package com.artiuillab.tieryourlife.feature.account.data.settings

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.account.data.remote.FacePhotoApi
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsApi
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotoSlot
import com.artiuillab.tieryourlife.feature.account.domain.repository.RemovedPhoto
import com.artiuillab.tieryourlife.feature.account.domain.repository.RestoredPhoto
import retrofit2.HttpException
import javax.inject.Inject

/** The slot as the Proxy keeps it (CONTRACT.md, lists, "The account's photo", DELETE /lists/face and its restore). */
class ProxyFacePhotoSlot @Inject constructor(
    private val api: SettingsApi,
    private val photos: FacePhotoApi,
) : FacePhotoSlot {

    override suspend fun photo(): Result<String?> = runSuspendCatching {
        api.settings().photo?.url?.takeIf { it.startsWith("https://") }
    }

    override suspend fun remove(): Result<RemovedPhoto> = runSuspendCatching { RemovedPhoto(photos.removePhoto().wasWorn) }

    override suspend fun restore(): Result<RestoredPhoto> = runSuspendCatching {
        try {
            val answer = photos.restorePhoto()
            answer.photo?.url?.let { RestoredPhoto.Back(it, answer.wasWorn) } ?: RestoredPhoto.Gone
        } catch (e: HttpException) {
            when (e.code()) {
                HTTP_NOT_FOUND -> RestoredPhoto.Gone
                // A new photo since: the account's, read as it is now.
                HTTP_CONFLICT -> api.settings().photo?.url?.let(RestoredPhoto::Replaced) ?: RestoredPhoto.Gone
                else -> throw e
            }
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
    }
}
