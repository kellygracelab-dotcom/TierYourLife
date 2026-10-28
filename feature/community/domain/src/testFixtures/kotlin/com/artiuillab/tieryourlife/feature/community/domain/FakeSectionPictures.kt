package com.artiuillab.tieryourlife.feature.community.domain

import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionPictures

/** Pictures of the size a test says, uploaded at once under ids the test can read back. */
class FakeSectionPictures(
    var size: Result<PictureSize> = Result.success(PictureSize(2000, 1500)),
    var uploadAnswer: Result<String> = Result.success("picture-1"),
) : SectionPictures {
    val uploads = mutableListOf<Pair<String, PictureCrop>>()

    override suspend fun measure(sourceUri: String): Result<PictureSize> = size

    override suspend fun upload(sourceUri: String, crop: PictureCrop): Result<String> {
        uploads += sourceUri to crop
        return uploadAnswer
    }
}
