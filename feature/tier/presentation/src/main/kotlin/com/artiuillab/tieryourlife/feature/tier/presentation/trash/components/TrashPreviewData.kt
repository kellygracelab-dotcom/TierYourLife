package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

internal val previewTrashEntries = listOf(
    TrashEntry.DeletedList(
        id = 1,
        title = "Sci-fi films",
        itemCount = 12,
        deletedAtMillis = 0L,
    ),
    TrashEntry.DeletedItem(
        id = 2,
        title = "Interstellar",
        listTitle = "Sci-fi films",
        wasInPool = false,
        deletedAtMillis = 0L,
        imageUrl = null,
    ),
    TrashEntry.DeletedItem(
        id = 3,
        title = "Arrival",
        listTitle = "Sci-fi films",
        wasInPool = true,
        deletedAtMillis = 0L,
        imageUrl = null,
    ),
)
