package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList

/**
 * Lists this phone has opened, kept by id, so opening one again draws it at
 * once while the server is asked behind it.
 */
interface OpenedLists {

    /** Null when it was never kept, or was dropped for not being opened. Reading counts as opening. */
    suspend fun read(id: String): PublishedList?

    suspend fun keep(list: PublishedList)

    /** Gone from the server, so gone from here. */
    suspend fun forget(id: String)
}
