package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

internal val previewTrashEntries = listOf(
    TrashEntry.DeletedList(
        id = 1,
        title = "Sci-fi films",
        itemCount = 12,
        deletedAtMillis = 0L,
    ),
    TrashEntry.DeletedList(
        id = 2,
        title = "Board games",
        itemCount = 7,
        deletedAtMillis = 0L,
    ),
)
