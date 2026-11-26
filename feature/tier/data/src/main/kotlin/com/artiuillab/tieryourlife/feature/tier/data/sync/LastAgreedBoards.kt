package com.artiuillab.tieryourlife.feature.tier.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LastAgreed] per account and board, as files rather than a table: only
 * this phone's sync reads them, a lost one only means the old second board,
 * and the database schema stays as it is. Out of system backup, as they mean
 * nothing without the records of agreeing beside them.
 */
@Singleton
class LastAgreedBoards(private val directory: File) {

    @Inject constructor(@ApplicationContext context: Context) : this(File(context.noBackupFilesDir, "agreed_boards"))

    private val json = Json { ignoreUnknownKeys = true }

    internal fun read(account: String, uid: String): LastAgreed? {
        val file = fileOf(account, uid).takeIf { it.isFile } ?: return null
        return runCatching { json.decodeFromString<LastAgreed>(file.readText()) }
            .onFailure { failure -> Timber.w(failure, "A last agreed board no longer reads") }
            .getOrNull()
    }

    internal fun write(account: String, uid: String, agreed: LastAgreed) {
        runCatching {
            val file = fileOf(account, uid)
            file.parentFile?.mkdirs()
            // Whole or not at all: a half-written one would be read as the last agreement.
            val partial = File(file.parentFile, "${file.name}.part")
            partial.writeText(json.encodeToString(LastAgreed.serializer(), agreed))
            if (!partial.renameTo(file)) {
                file.delete()
                partial.renameTo(file)
            }
        }.onFailure { failure -> Timber.w(failure, "Could not keep what was last agreed") }
    }

    internal fun holds(account: String, uid: String): Boolean = fileOf(account, uid).isFile

    internal fun forget(account: String, uid: String) {
        fileOf(account, uid).delete()
    }

    /** Only the boards [account] still has a record of agreeing for. */
    internal fun keepOnly(account: String, uids: Collection<String>) {
        val wanted = uids.mapTo(mutableSetOf()) { nameOf(it) }
        folderOf(account).listFiles()?.filterNot { it.name in wanted }?.forEach { it.delete() }
    }

    private fun folderOf(account: String) = File(directory, nameOf(account))

    private fun fileOf(account: String, uid: String) = File(folderOf(account), nameOf(uid))

    /** Any text as a file name, the same every time. */
    private fun nameOf(text: String) = UUID.nameUUIDFromBytes(text.toByteArray()).toString()
}
