package com.artiuillab.tieryourlife.feature.account.domain

import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotoSlot
import com.artiuillab.tieryourlife.feature.account.domain.repository.RemovedPhoto
import com.artiuillab.tieryourlife.feature.account.domain.repository.RestoredPhoto
import java.io.IOException

/** The server's slot, as the contract has it: one photo, removed and put back while it is kept. */
class FakeFacePhotoSlot(
    var photo: String? = null,
    var worn: () -> String? = { null },
    private val unreachable: Boolean = false,
) : FacePhotoSlot {

    var removals = 0
    var restores = 0
    private var removed: Pair<String, Boolean>? = null

    /** A new upload since the removal: restore answers it, as the server's 409. */
    var uploadedSince: String? = null

    override suspend fun photo(): Result<String?> = if (unreachable) failure() else Result.success(photo)

    override suspend fun remove(): Result<RemovedPhoto> {
        removals++
        if (unreachable) return failure()
        val gone = photo ?: return Result.success(RemovedPhoto(wasWorn = false))
        val wasWorn = worn() == gone
        removed = gone to wasWorn
        photo = null
        return Result.success(RemovedPhoto(wasWorn))
    }

    override suspend fun restore(): Result<RestoredPhoto> {
        restores++
        if (unreachable) return failure()
        uploadedSince?.let { return Result.success(RestoredPhoto.Replaced(it)) }
        val (url, wasWorn) = removed ?: return Result.success(RestoredPhoto.Gone)
        removed = null
        photo = url
        return Result.success(RestoredPhoto.Back(url, wasWorn))
    }

    private fun <T> failure(): Result<T> = Result.failure(IOException("offline"))
}
