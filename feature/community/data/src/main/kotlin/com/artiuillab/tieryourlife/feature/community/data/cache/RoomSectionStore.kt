package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toDomain
import com.artiuillab.tieryourlife.feature.community.data.repository.toDto
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last sections answer, kept as one more row beside the feeds: the same
 * small database, no table of its own, and the same rule that a kept thing
 * nobody opens for two weeks goes. Reading it at every start counts as opening.
 */
@Singleton
class RoomSectionStore @Inject constructor(
    private val dao: FeedCacheDao,
) : SectionStore {

    override suspend fun read(): SectionDirectory? = runSuspendCatching {
        val row = dao.get(KEY) ?: return@runSuspendCatching null
        dao.touch(KEY, System.currentTimeMillis())
        decodeSections(row.json, row.fetchedAt)
    }.onFailure { Timber.w(it, "Reading the kept sections failed") }.getOrNull()

    override suspend fun write(directory: SectionDirectory) {
        val fetchedAt = directory.fetchedAtMillis ?: return
        runSuspendCatching {
            val now = System.currentTimeMillis()
            dao.put(FeedCacheEntity(key = KEY, json = encodeSections(directory), fetchedAt = fetchedAt, openedAt = now))
        }.onFailure { Timber.w(it, "Keeping the sections failed") }
    }

    private companion object {
        /** No feed key looks like this: theirs are screens and `section:` ids. */
        const val KEY = "sections"
    }
}

@Serializable
internal data class KeptSections(
    val sections: List<SectionDto> = emptyList(),
    val extra: List<SectionDto> = emptyList(),
    val merged: Map<String, String> = emptyMap(),
)

private val keptJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

/** Ids nobody had heard of are not kept: by the next run one of them may have been approved. */
internal fun encodeSections(directory: SectionDirectory): String = keptJson.encodeToString(
    KeptSections.serializer(),
    KeptSections(
        sections = directory.sections.map { it.toDto() },
        extra = directory.extra.values.map { it.toDto() },
        merged = directory.merged,
    ),
)

internal fun decodeSections(json: String, fetchedAtMillis: Long): SectionDirectory? {
    val kept = keptJson.decodeFromString(KeptSections.serializer(), json)
    if (kept.sections.isEmpty()) return null
    return SectionDirectory(
        sections = kept.sections.map { it.toDomain() },
        extra = kept.extra.map { it.toDomain() }.associateBy { it.id },
        merged = kept.merged,
        fetchedAtMillis = fetchedAtMillis,
    )
}
