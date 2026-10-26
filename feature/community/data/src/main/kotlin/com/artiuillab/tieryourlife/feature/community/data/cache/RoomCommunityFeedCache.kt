package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedFreshness
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCommunityFeedCache @Inject constructor(
    private val dao: FeedCacheDao,
) : CommunityFeedCache {

    override suspend fun read(key: String): CachedFeed? = runSuspendCatching {
        val now = System.currentTimeMillis()
        val row = dao.get(key) ?: return@runSuspendCatching null
        dao.touch(key, now)
        CachedFeed(page = decodePage(row.json), fetchedAtMillis = row.fetchedAt)
    }.onFailure { Timber.w(it, "Reading a kept feed failed") }.getOrNull()

    override suspend fun write(key: String, page: CommunityPage, fetchedAtMillis: Long) {
        runSuspendCatching {
            val now = System.currentTimeMillis()
            dao.put(FeedCacheEntity(key = key, json = encodePage(page), fetchedAt = fetchedAtMillis, openedAt = now))
            dao.dropOpenedBefore(now - FeedFreshness.KEEP_UNOPENED_MILLIS)
        }.onFailure { Timber.w(it, "Keeping a feed failed") }
    }
}

@Serializable
internal data class KeptPage(
    val lists: List<PublishedListSummaryDto> = emptyList(),
    val nextCursor: String? = null,
    val followingNobody: Boolean = false,
)

private val keptJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

/** Only the first page is kept: it is what a screen opens on, and the rest comes as the person scrolls. */
internal fun encodePage(page: CommunityPage): String = keptJson.encodeToString(
    KeptPage.serializer(),
    KeptPage(
        lists = page.lists.take(FeedFreshness.KEPT_LISTS).map { it.toDto() },
        nextCursor = page.nextCursor.takeIf { page.lists.size <= FeedFreshness.KEPT_LISTS },
        followingNobody = page.followingNobody,
    ),
)

internal fun decodePage(json: String): CommunityPage {
    val kept = keptJson.decodeFromString(KeptPage.serializer(), json)
    return CommunityPage(lists = kept.lists.map { it.toSummary() }, nextCursor = kept.nextCursor, followingNobody = kept.followingNobody)
}
