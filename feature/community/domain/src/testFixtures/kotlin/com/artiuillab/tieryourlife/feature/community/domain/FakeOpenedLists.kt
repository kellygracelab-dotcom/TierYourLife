package com.artiuillab.tieryourlife.feature.community.domain

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.repository.OpenedLists

/** Opened lists in memory: what a test puts in is what the page opens with. */
class FakeOpenedLists(vararg initial: PublishedList) : OpenedLists {
    val kept = initial.associateBy { it.summary.id }.toMutableMap()
    val forgotten = mutableListOf<String>()

    override suspend fun read(id: String): PublishedList? = kept[id]

    override suspend fun keep(list: PublishedList) {
        kept[list.summary.id] = list
    }

    override suspend fun forget(id: String) {
        kept.remove(id)
        forgotten += id
    }
}
