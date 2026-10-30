package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toDomain
import com.artiuillab.tieryourlife.feature.community.data.repository.toDto
import com.artiuillab.tieryourlife.feature.community.domain.model.FeedFreshness
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.repository.OpenedLists
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opened lists as rows beside the feeds, one per list id, the way the
 * sections are kept: no table of their own, and the same two weeks unopened
 * before one goes. The row's fetch time is when the server last answered;
 * the list's own updatedAt travels in it.
 */
@Singleton
class RoomOpenedLists @Inject constructor(
    private val dao: FeedCacheDao,
) : OpenedLists {

    override suspend fun read(id: String): PublishedList? = runSuspendCatching {
        val row = dao.get(keyOf(id)) ?: return@runSuspendCatching null
        dao.touch(keyOf(id), System.currentTimeMillis())
        decodeOpened(row.json)
    }.onFailure { Timber.w(it, "Reading a kept list failed") }.getOrNull()

    override suspend fun keep(list: PublishedList) {
        runSuspendCatching {
            val now = System.currentTimeMillis()
            dao.put(FeedCacheEntity(key = keyOf(list.summary.id), json = encodeOpened(list), fetchedAt = now, openedAt = now))
            dao.dropOpenedBefore(now - FeedFreshness.KEEP_UNOPENED_MILLIS)
        }.onFailure { Timber.w(it, "Keeping a list failed") }
    }

    override suspend fun forget(id: String) {
        runSuspendCatching { dao.drop(keyOf(id)) }.onFailure { Timber.w(it, "Forgetting a kept list failed") }
    }
}

/** No feed key and not the sections' key looks like this. */
internal fun keyOf(listId: String): String = "list:$listId"

private val keptJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

internal fun encodeOpened(list: PublishedList): String = keptJson.encodeToString(PublishedListDto.serializer(), list.toDto())

/** Whose it is was decided when it was kept. */
internal fun decodeOpened(json: String): PublishedList = keptJson.decodeFromString(PublishedListDto.serializer(), json).toDomain()
