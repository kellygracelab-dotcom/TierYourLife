package com.artiuillab.tieryourlife.feature.community.data.cache

import android.content.Context
import android.util.AtomicFile
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedRanking
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Atomic local progress, separate from the disposable feed cache and separated by account. */
@Singleton
class FileRankingDrafts internal constructor(
    private val directory: File,
    private val now: () -> Long = System::currentTimeMillis,
) : RankingDrafts {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        File(context.filesDir, "ranking-drafts"),
    )

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(listId: String, owner: String?): RankingDraft? {
        val file = fileFor(owner, listId)
        return mutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val record = json.decodeFromString<DraftRecord>(file.readFully().toString(Charsets.UTF_8))
                    record.draft()
                }.getOrNull()
            }
        }
    }

    override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
        val file = fileFor(owner, listId)
        val record = DraftRecord(
            version = draft.version,
            rows = draft.rows,
            pool = draft.pool,
            choice = draft.choice?.wireName,
            savedBoardUid = draft.savedBoardUid,
            listId = listId,
            owner = owner,
            title = draft.title,
            section = draft.section,
            keptAt = now(),
        )
        val encoded = json.encodeToString(record).toByteArray()
        mutex.withLock {
            withContext(Dispatchers.IO) {
                check(directory.isDirectory || directory.mkdirs())
                val stream = file.startWrite()
                try {
                    stream.write(encoded)
                    file.finishWrite(stream)
                } catch (error: IOException) {
                    file.failWrite(stream)
                    throw error
                }
            }
        }
    }

    override suspend fun started(owner: String?): List<StartedRanking> = mutex.withLock {
        withContext(Dispatchers.IO) {
            directory.listFiles { file -> file.name.endsWith(".json") }.orEmpty().mapNotNull { file ->
                runCatching { json.decodeFromString<DraftRecord>(AtomicFile(file).readFully().toString(Charsets.UTF_8)) }.getOrNull()
            }
        }
    }.mapNotNull { record -> record.started(owner) }.sortedByDescending { it.keptAt }

    private fun fileFor(account: String?, listId: String): AtomicFile {
        val key = MessageDigest.getInstance("SHA-256").digest("$account\u0000$listId".toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$key.json"))
    }
}

@Serializable
private data class DraftRecord(
    val version: Long,
    val rows: List<List<Int>>,
    val pool: List<Int>,
    val choice: String? = null,
    val savedBoardUid: String? = null,
    /** Kept since the list of what was begun: earlier records stay readable, and stay out of that list. */
    val listId: String? = null,
    val owner: String? = null,
    val title: String = "",
    val keptAt: Long = 0,
    val section: String? = null,
) {
    fun draft() = RankingDraft(version, rows, pool, RankingChoice.fromWire(choice), savedBoardUid, title, section)

    fun started(account: String?): StartedRanking? {
        val id = listId ?: return null
        val draft = draft().takeIf { owner == account && savedBoardUid == null } ?: return null
        if (title.isBlank() || draft.placed == 0) return null
        return StartedRanking(id, title, version, draft.placed, draft.total, keptAt, section)
    }
}
