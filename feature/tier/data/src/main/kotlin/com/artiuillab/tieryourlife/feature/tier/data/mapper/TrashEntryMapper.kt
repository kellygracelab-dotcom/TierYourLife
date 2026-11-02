package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.dao.DeletedTierListRow
import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

internal fun DeletedTierListRow.toDomain(): TrashEntry.DeletedList =
    TrashEntry.DeletedList(
        id = id,
        title = title,
        itemCount = itemCount,
        deletedAtMillis = deletedAt,
        publishedId = publishedId,
        rankingOf = rankingOf,
    )
