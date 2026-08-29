package com.artiuillab.tieryourlife.feature.tier.domain.model

sealed interface TrashEntry {
    val title: String
    val deletedAtMillis: Long

    data class DeletedList(
        val id: Long,
        override val title: String,
        val itemCount: Int,
        override val deletedAtMillis: Long,
        /** Set on a list trashed before deleting also took it out of the community. */
        val publishedId: String? = null,
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
