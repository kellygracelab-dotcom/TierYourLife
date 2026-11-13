package com.artiuillab.tieryourlife.feature.community.domain.model

/** What the server will publish after the reader explicitly confirms. */
sealed interface PublicationKind {
    data class NewList(val basedOn: BasedOn? = null, val sourceVersion: Long? = null) : PublicationKind
    data class Ranking(val listId: String, val version: Long) : PublicationKind
}
