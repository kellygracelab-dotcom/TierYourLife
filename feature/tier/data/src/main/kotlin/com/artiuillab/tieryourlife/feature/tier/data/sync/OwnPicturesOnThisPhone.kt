package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import javax.inject.Inject

class OwnPicturesOnThisPhone @Inject constructor(
    private val images: TierImageStore,
    private val pictures: PictureSync,
    private val accounts: AccountRepository,
) : OwnPictures {
    override fun pictureIdOf(imageUrl: String?): String? = images.pictureIdOf(imageUrl)

    /** Nothing goes up without an account to keep it. */
    override suspend fun sendNow(pictureIds: List<String>): Set<String> {
        val account = (accounts.current() as? Account.SignedIn)?.recordKey ?: return emptySet()
        return pictures.sendNow(account, pictureIds)
    }
}
