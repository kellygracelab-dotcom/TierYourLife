package com.artiuillab.tieryourlife.feature.account.domain

import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos

/** Answers every upload with [answer] and remembers what it was handed. */
class FakeFacePhotos(
    private val answer: Result<String> = Result.success(PICTURE_ID),
) : FacePhotos {

    val uploaded = mutableListOf<String>()

    override suspend fun upload(sourceUri: String): Result<String> {
        uploaded += sourceUri
        return answer
    }

    companion object {
        const val PICTURE_ID = "picture-1"
    }
}
