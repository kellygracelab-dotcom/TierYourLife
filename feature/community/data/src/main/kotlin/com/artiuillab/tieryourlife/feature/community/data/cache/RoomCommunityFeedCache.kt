package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListSummaryDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedFreshness
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCommunityFeedCache @Inject constructor(
    private val dao: FeedCacheDao,
    private val auth: FirebaseAuth,
) : CommunityFeedCache {

    private val me: String? get() = auth.currentUser?.uid

    override fun reader(): String? = me

    // Off the main thread: a feed's twenty cards are parsed here while the person is swiping to it.
    override suspend fun read(key: String): CachedFeed? = withContext(Dispatchers.Default) {
        runSuspendCatching { readNow(key) }.onFailure { Timber.w(it, "Reading a kept feed failed") }.getOrNull()
    }

    private suspend fun readNow(key: String): CachedFeed? {
        val now = System.currentTimeMillis()
        val row = dao.get(key) ?: return null
        val page = decodePageFor(row.json, me) ?: return null
        dao.touch(key, now)
        return CachedFeed(page = page, fetchedAtMillis = row.fetchedAt)
    }

    override suspend fun write(key: String, page: CommunityPage, fetchedAtMillis: Long) {
        withContext(Dispatchers.Default) {
            runSuspendCatching {
                val now = System.currentTimeMillis()
                dao.put(FeedCacheEntity(key = key, json = encodePage(page, keptBy = me), fetchedAt = fetchedAtMillis, openedAt = now))
                dao.dropOpenedBefore(now - FeedFreshness.KEEP_UNOPENED_MILLIS)
            }.onFailure { Timber.w(it, "Keeping a feed failed") }
        }
    }
}

@Serializable
internal data class KeptPage(
    /** Whose answer it was: "mine", following and the hidden author's own lists are one person's. */
    val keptBy: String? = null,
    val lists: List<PublishedListSummaryDto> = emptyList(),
    val nextCursor: String? = null,
    val followingNobody: Boolean = false,
)

private val keptJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

/** Only the first page is kept: it is what a screen opens on, and the rest comes as the person scrolls. */
internal fun encodePage(page: CommunityPage, keptBy: String? = null): String = keptJson.encodeToString(
    KeptPage.serializer(),
    KeptPage(
        keptBy = keptBy,
        lists = page.lists.take(FeedFreshness.KEPT_LISTS).map { it.toDto() },
        nextCursor = page.nextCursor.takeIf { page.lists.size <= FeedFreshness.KEPT_LISTS },
        followingNobody = page.followingNobody,
    ),
)

internal fun decodePage(json: String): CommunityPage = keptJson.decodeFromString(KeptPage.serializer(), json).toPage()

/** Somebody else's page reads as never kept: the next person on this phone must not see whose lists were whose. */
internal fun decodePageFor(json: String, me: String?): CommunityPage? =
    keptJson.decodeFromString(KeptPage.serializer(), json).takeIf { it.keptBy == me }?.toPage()

private fun KeptPage.toPage() =
    CommunityPage(lists = lists.map { it.toSummary() }, nextCursor = nextCursor, followingNobody = followingNobody)
