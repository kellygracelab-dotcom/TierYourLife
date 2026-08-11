package com.artiuillab.tieryourlife.feature.aistudio.data.library

import com.artiuillab.tieryourlife.feature.aistudio.domain.library.GeneratedCardSaver
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItemSource
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import javax.inject.Inject

class PoolGeneratedCardSaver @Inject constructor(
    private val tierRepository: TierRepository,
) : GeneratedCardSaver {

    override suspend fun save(tierListId: Long, title: String, imageUri: String): Long {
        val itemId = tierRepository.addItemToPool(
            tierListId = tierListId,
            title = title,
            imageUrl = null,
            source = TierItemSource.GENERATED,
        )
        tierRepository.attachImageToItem(itemId, imageUri)
        return itemId
    }
}
