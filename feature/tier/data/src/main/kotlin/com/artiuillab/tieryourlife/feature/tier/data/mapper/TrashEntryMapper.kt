package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierItemRow
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierListRow
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

internal fun DeletedTierListRow.toDomain(): TrashEntry.DeletedList =
    TrashEntry.DeletedList(
        id = id,
        title = title,
        itemCount = itemCount,
        deletedAtMillis = deletedAt,
        publishedId = publishedId,
    )

internal fun DeletedTierItemRow.toDomain(): TrashEntry.DeletedItem =
    TrashEntry.DeletedItem(
        id = id,
        title = title,
        listTitle = listTitle,
        wasInPool = wasInPool,
        deletedAtMillis = deletedAt,
        imageUrl = imageUrl,
    )
