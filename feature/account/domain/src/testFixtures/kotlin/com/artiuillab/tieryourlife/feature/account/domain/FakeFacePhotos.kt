package com.artiuillab.tieryourlife.feature.account.domain

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import kotlinx.coroutines.CompletableDeferred

/**
 * Prepares every picture into [PREPARED_URI] and answers every upload with
 * [answer], remembering what it was handed. With [holdUploads] an upload waits
 * for [releaseUploads], so a test can look at the screen while it is on its way.
 */
class FakeFacePhotos(
    private val answer: Result<String> = Result.success(PICTURE_ID),
    private val prepareAnswer: Result<String> = Result.success(PREPARED_URI),
    holdUploads: Boolean = false,
) : FacePhotos {

    val prepared = mutableListOf<Pair<String, FaceCrop?>>()
    val uploaded = mutableListOf<String>()
    private val gate = CompletableDeferred<Unit>().apply { if (!holdUploads) complete(Unit) }

    fun releaseUploads() {
        gate.complete(Unit)
    }

    override suspend fun prepare(sourceUri: String, crop: FaceCrop?): Result<String> {
        prepared += sourceUri to crop
        return prepareAnswer
    }

    override suspend fun upload(preparedUri: String): Result<String> {
        uploaded += preparedUri
        gate.await()
        return answer
    }

    companion object {
        const val PICTURE_ID = "picture-1"
        const val PREPARED_URI = "file:///cache/faces/prepared-1.jpg"
    }
}
