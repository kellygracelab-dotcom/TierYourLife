package com.artiuillab.tieryourlife.feature.community.data.cache

import android.content.Context
import android.util.AtomicFile
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDrafts
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
                    RankingDraft(record.version, record.rows, record.pool)
                }.getOrNull()
            }
        }
    }

    override suspend fun keep(listId: String, draft: RankingDraft, owner: String?) {
        val file = fileFor(owner, listId)
        val encoded = json.encodeToString(DraftRecord(draft.version, draft.rows, draft.pool)).toByteArray()
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

    private fun fileFor(account: String?, listId: String): AtomicFile {
        val key = MessageDigest.getInstance("SHA-256").digest("$account\u0000$listId".toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$key.json"))
    }
}

@Serializable
private data class DraftRecord(val version: Long, val rows: List<List<Int>>, val pool: List<Int>)
