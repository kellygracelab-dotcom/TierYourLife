package com.artiuillab.tieryourlife.feature.tier.domain.repository

/**
 * The one thing the account screen needs to know about lists. A port rather
 * than the whole repository: the profile has no business reaching further.
 */
interface PublishedLists {

    /** How many of this phone's lists are currently in the community feed. */
    suspend fun count(): Int
}
