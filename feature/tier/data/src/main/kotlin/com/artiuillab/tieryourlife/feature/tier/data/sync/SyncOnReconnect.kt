package com.artiuillab.tieryourlife.feature.tier.data.sync

import android.os.SystemClock
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.tier.data.local.database.TierDatabase
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends whatever is waiting the moment a connection comes back, and a few
 * seconds after a board changes here, while it is still open: the list of
 * boards and an opened board start their own runs. Its own scope rather than
 * a screen's, because nobody is looking at the right screen when it matters.
 */
@Singleton
class SyncOnReconnect @Inject constructor(
    private val connection: Connection,
    private val boardSync: BoardSync,
    private val merge: BoardMerge,
    private val session: LostSession,
    private val erasedElsewhere: ErasedElsewhere,
    private val database: TierDatabase,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            // Before any sync: one would run as an account that may be gone.
            runSuspendCatching { merge.settleUnansweredErase() }
                .onFailure { Timber.w(it, "Could not settle an unanswered erase") }
            // After the erase: an account it settled is not a session lost.
            launch { session.watch() }
            launch { erasedElsewhere.watch() }
            launch { sendEdits() }
            connection.available.collect {
                runSuspendCatching { boardSync.sync() }.onFailure { failure ->
                    // A dropped connection is ordinary here; anything else is a bug nobody would see.
                    if (failure is IOException) {
                        Timber.i(failure, "Sync on reconnect lost the connection")
                    } else {
                        Timber.w(failure, "Sync on reconnect failed")
                    }
                }
            }
        }
    }

    /**
     * A run only for something new to send: the run's own writes, and a board
     * the account keeps refusing, look the same as before and start none.
     */
    private suspend fun sendEdits() {
        var leftUnsent = emptySet<String>()
        val changes = database.invalidationTracker.createFlow(*BOARD_TABLES, emitInitialState = false)
        sendWhenQuiet(changes, QUIET_MS, LONGEST_MS, SystemClock::elapsedRealtime) {
            val unsent = runSuspendCatching { boardSync.unsent() }.getOrDefault(emptySet())
            if (unsent.isEmpty() || unsent == leftUnsent) return@sendWhenQuiet
            runSuspendCatching { boardSync.sync() }.onFailure { failure ->
                if (failure is IOException) Timber.i(failure, "Sending an edit lost the connection") else Timber.w(failure, "Sending an edit failed")
            }
            leftUnsent = runSuspendCatching { boardSync.unsent() }.getOrDefault(unsent)
        }
    }

    private companion object {
        val BOARD_TABLES = arrayOf("tier_lists", "tiers", "tier_items")
        const val QUIET_MS = 4_000L
        const val LONGEST_MS = 20_000L
    }
}
