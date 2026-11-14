package com.artiuillab.tieryourlife.feature.community.domain.model

/** What the server will publish after the reader explicitly confirms. */
sealed interface PublicationKind {
    data class NewList(val basedOn: BasedOn? = null, val sourceVersion: Long? = null) : PublicationKind

    /** [ownCards]: the board has cards the list lacks; it goes to Rankings as the reader's version with them (SPEC list-flow К3). */
    data class Ranking(val listId: String, val version: Long, val ownCards: Boolean = false) : PublicationKind
}
