package com.artiuillab.tieryourlife.feature.tier.domain.repository

/** What the profile needs to know about this phone's lists: a port, not the whole repository. */
interface OwnLists {

    /** How many of this phone's lists are currently in the community feed. */
    suspend fun publishedCount(): Int

    /** Counted so the offer can say "3 boards and their pictures" rather than a slogan. */
    suspend fun boardCount(): Int

    /** Pictures that could serve as a face: web addresses only, since a gallery photo could never reach anyone else. */
    suspend fun cardImages(limit: Int): List<String>
}
