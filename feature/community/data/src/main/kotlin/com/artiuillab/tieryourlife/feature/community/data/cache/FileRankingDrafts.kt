package com.artiuillab.tieryourlife.feature.community.data.cache

import android.content.Context
import android.util.AtomicFile
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedRanking
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Each keep made here, for the account's copy to follow; what the account itself writes is not one. */
    val changes: Flow<Unit> = changed

    override suspend fun read(listId: String, owner: String?): RankingDraft? = record(owner, listId)?.draft()

    override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
        val at = now()
        // The account's version stays: the next write to the account says which one it began from.
        val known = record(owner, listId)
        write(
            DraftRecord(
                version = draft.version,
                rows = draft.rows,
                pool = draft.pool,
                choice = draft.choice?.wireName,
                savedBoardUid = draft.savedBoardUid,
                listId = listId,
                owner = owner,
                title = draft.title,
                keptAt = at,
                section = draft.section,
                updatedAt = known?.updatedAt,
                changedAt = at,
            ),
        )
        changed.tryEmit(Unit)
    }

    override suspend fun started(owner: String?): List<StartedRanking> =
        records(owner).mapNotNull { record -> record.started(owner) }.sortedByDescending { it.keptAt }

    internal suspend fun record(owner: String?, listId: String): DraftRecord? = mutex.withLock {
        withContext(Dispatchers.IO) { decode(fileFor(owner, listId)) }
    }

    /** [owner]'s records, the ones from before list ids were stored left out. */
    internal suspend fun records(owner: String?): List<DraftRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            directory.listFiles { file -> file.name.endsWith(".json") }.orEmpty().mapNotNull { decode(AtomicFile(it)) }
        }
    }.filter { it.owner == owner && it.listId != null }

    internal suspend fun write(record: DraftRecord) {
        val file = fileFor(record.owner, checkNotNull(record.listId))
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

    internal suspend fun forget(owner: String?, listId: String) {
        mutex.withLock { withContext(Dispatchers.IO) { fileFor(owner, listId).delete() } }
    }

    /** The last account version [owner] has seen, for asking only what is newer. */
    internal suspend fun cursor(owner: String): Long? = mutex.withLock {
        withContext(Dispatchers.IO) { cursorFile(owner).takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() }
    }

    internal suspend fun setCursor(owner: String, seen: Long) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                check(directory.isDirectory || directory.mkdirs())
                cursorFile(owner).writeText(seen.toString())
            }
        }
    }

    private fun decode(file: AtomicFile): DraftRecord? =
        runCatching { json.decodeFromString<DraftRecord>(file.readFully().toString(Charsets.UTF_8)) }.getOrNull()

    private fun fileFor(account: String?, listId: String): AtomicFile = AtomicFile(File(directory, "${keyOf("$account\u0000$listId")}.json"))

    private fun cursorFile(account: String): File = File(directory, "${keyOf("since\u0000$account")}.since")

    private fun keyOf(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * One list begun, as kept on this device. Every field added since the first is defaulted, so a file written
 * by an older version reads as one the account has not seen yet ([dirty], no [updatedAt]) and goes up with
 * the next sync.
 */
@Serializable
internal data class DraftRecord(
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
    /** The account's version of this list, the server's time; null until the account has it. */
    val updatedAt: Long? = null,
    /** When it changed on a device, by that device's clock: the later change wins. */
    val changedAt: Long = 0,
    /** Changed here since the account last had it. */
    val dirty: Boolean = true,
    /** Why it left Continue on every device (discard, saved, done, evicted); null while it is in it. */
    val removed: String? = null,
) {
    fun draft() = RankingDraft(version, rows, pool, RankingChoice.fromWire(choice), savedBoardUid, title, section)

    val lastChange: Long get() = changedAt.takeIf { it > 0 } ?: keptAt

    fun started(account: String?): StartedRanking? {
        val id = listId ?: return null
        val draft = draft().takeIf { owner == account && savedBoardUid == null && removed == null } ?: return null
        // Every card placed is a ranking finished, not one to continue.
        if (title.isBlank() || draft.placed == 0 || draft.placed == draft.total) return null
        return StartedRanking(id, title, version, draft.placed, draft.total, lastChange, section)
    }
}
