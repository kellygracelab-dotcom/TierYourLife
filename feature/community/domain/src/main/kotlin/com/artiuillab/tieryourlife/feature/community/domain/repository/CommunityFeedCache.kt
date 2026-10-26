package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import kotlinx.coroutines.flow.Flow

/** The last good first page of a feed, kept on the phone so the screen opens with it rather than with a wait. */
data class CachedFeed(val page: CommunityPage, val fetchedAtMillis: Long)

interface CommunityFeedCache {

    /** Null when the feed was never kept, or was dropped for not being opened. Reading counts as opening. */
    suspend fun read(key: String): CachedFeed?

    suspend fun write(key: String, page: CommunityPage, fetchedAtMillis: Long)
}

/** When the phone gets a usable network back: what lets a failed feed try again without a button. */
interface CommunityNetwork {
    val available: Flow<Unit>
}
