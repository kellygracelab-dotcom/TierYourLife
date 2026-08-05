package com.artiuillab.tieryourlife.feature.tier.domain.model

// One row of the trash screen. Entries stay until the user deletes them
// permanently; deletedAtMillis drives the newest-first ordering.
sealed interface TrashEntry {
    val title: String
    val deletedAtMillis: Long

    data class DeletedList(
        val id: Long,
        override val title: String,
        val itemCount: Int,
        override val deletedAtMillis: Long,
    ) : TrashEntry

    data class DeletedItem(
        val id: Long,
        override val title: String,
        val listTitle: String,
        val wasInPool: Boolean,
        override val deletedAtMillis: Long,
        val imageUrl: String? = null,
    ) : TrashEntry
}
