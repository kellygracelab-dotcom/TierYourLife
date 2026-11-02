package com.artiuillab.tieryourlife.feature.tier.domain.model

/** Only lists go to the trash: a card removed from a board is gone, with an Undo right after. */
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
        /** The same for a list that was out as a ranking of somebody's. */
        val rankingOf: String? = null,
    ) : TrashEntry
}
