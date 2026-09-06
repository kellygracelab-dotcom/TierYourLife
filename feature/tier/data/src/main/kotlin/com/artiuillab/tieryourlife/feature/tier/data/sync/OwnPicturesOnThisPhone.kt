package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import javax.inject.Inject

class OwnPicturesOnThisPhone @Inject constructor(
    private val images: TierImageStore,
    private val pictures: PictureSync,
) : OwnPictures {
    override fun pictureIdOf(imageUrl: String?): String? = images.pictureIdOf(imageUrl)

    override suspend fun sendNow(pictureIds: List<String>): Set<String> = pictures.sendNow(pictureIds)
}
