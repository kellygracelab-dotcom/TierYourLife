package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * Lists that stand without an author, the way a catalogue's templates do: no
 * name, no face, no way to a profile, nothing to follow or hide wholesale.
 *
 * The server says which: a house list comes out [PublishedListSummary.anonymous]
 * with no author uid, and its account is [HOUSE]. Nobody else publishes without
 * a name since 16.11, so such a list is always the app's own. The site follows
 * the same rule. A person's lists, Danylo's included, keep their name.
 */
object EditorialAuthors {

    /** The account the server keeps house lists under. */
    const val HOUSE = "tyl-editorial"

    fun isEditorial(authorUid: String, anonymous: Boolean = false): Boolean =
        anonymous || authorUid.isBlank() || authorUid == HOUSE
}
