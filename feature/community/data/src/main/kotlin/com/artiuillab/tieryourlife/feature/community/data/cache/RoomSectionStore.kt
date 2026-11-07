package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionDto
import com.artiuillab.tieryourlife.feature.community.data.repository.toDomain
import com.artiuillab.tieryourlife.feature.community.data.repository.toDto
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionStore
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth,
) : SectionStore {

    private val me: String? get() = auth.currentUser?.uid

    override suspend fun read(): SectionDirectory? = runSuspendCatching {
        val row = dao.get(KEY) ?: return@runSuspendCatching null
        decodeSections(row.json, row.fetchedAt, me)?.also { dao.touch(KEY, System.currentTimeMillis()) }
    }.onFailure { Timber.w(it, "Reading the kept sections failed") }.getOrNull()

    override suspend fun write(directory: SectionDirectory) {
        val fetchedAt = directory.fetchedAtMillis ?: return
        runSuspendCatching {
            val now = System.currentTimeMillis()
            dao.put(FeedCacheEntity(key = KEY, json = encodeSections(directory, keptBy = me), fetchedAt = fetchedAt, openedAt = now))
        }.onFailure { Timber.w(it, "Keeping the sections failed") }
    }

    private companion object {
        /** No feed key looks like this: theirs are screens and `section:` ids. */
        const val KEY = "sections"
    }
}

@Serializable
internal data class KeptSections(
    /** Whose answer it was: what they follow and what they proposed are theirs alone. */
    val keptBy: String? = null,
    val sections: List<SectionDto> = emptyList(),
    val extra: List<SectionDto> = emptyList(),
    val merged: Map<String, String> = emptyMap(),
    val mergedTopics: Map<String, String> = emptyMap(),
    val mergedNoTopic: Set<String> = emptySet(),
    /** The ids of this person's own proposals: the server sends the proposer, not "yours". */
    val mine: List<String> = emptyList(),
)

private val keptJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

/** Ids nobody had heard of are not kept: by the next run one of them may have been approved. */
internal fun encodeSections(directory: SectionDirectory, keptBy: String? = null): String = keptJson.encodeToString(
    KeptSections.serializer(),
    KeptSections(
        keptBy = keptBy,
        sections = directory.sections.map { it.toDto() },
        extra = directory.extra.values.map { it.toDto() },
        merged = directory.merged,
        mergedTopics = directory.mergedTopics,
        mergedNoTopic = directory.mergedNoTopic,
        mine = directory.sections.filter { it.mine }.map { it.id },
    ),
)

/** Somebody else's answer reads as nothing kept. */
internal fun decodeSections(json: String, fetchedAtMillis: Long, me: String? = null): SectionDirectory? {
    val kept = keptJson.decodeFromString(KeptSections.serializer(), json)
    if (kept.sections.isEmpty() || kept.keptBy != me) return null
    return SectionDirectory(
        sections = kept.sections.map { it.toDomain().copy(mine = it.id in kept.mine) },
        extra = kept.extra.map { it.toDomain() }.associateBy { it.id },
        merged = kept.merged,
        mergedTopics = kept.mergedTopics,
        mergedNoTopic = kept.mergedNoTopic,
        fetchedAtMillis = fetchedAtMillis,
    )
}
