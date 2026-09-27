package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * Lists that stand without an author, the way a catalogue's templates do: no
 * name, no face, no way to a profile, nothing to follow or hide wholesale.
 *
 * Kept here by uid because the server does not say so yet. Once it does, it
 * sends [PublishedListSummary.anonymous] and an empty author uid, and both
 * already count. The site follows the same rule.
 */
object EditorialAuthors {

    private val uids = setOf(
        // The app's own account: the lists it publishes seed the feed.
        "85RRieyLyUPCEqHjdYCdxVlWxHx2",
    )

    fun isEditorial(authorUid: String, anonymous: Boolean = false): Boolean =
        anonymous || authorUid.isBlank() || authorUid in uids
}
