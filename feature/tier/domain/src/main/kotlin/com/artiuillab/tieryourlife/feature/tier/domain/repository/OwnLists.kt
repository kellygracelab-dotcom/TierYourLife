package com.artiuillab.tieryourlife.feature.tier.domain.repository

/**
 * What the profile needs to know about this phone's lists, and no more. A port
 * rather than the whole repository: the profile has no business reaching further.
 */
interface OwnLists {

    /** How many of this phone's lists are currently in the community feed. */
    suspend fun publishedCount(): Int

    /**
     * How many boards are on this phone. Counted so the offer to keep them can
     * say what it would be keeping: "3 boards and their pictures" is an
     * account doing something, "your boards" is a slogan.
     */
    suspend fun boardCount(): Int

    /**
     * Card pictures that could serve as a face: web addresses only, because a
     * photo from the gallery could never reach anyone else.
     */
    suspend fun cardImages(limit: Int): List<String>
}
