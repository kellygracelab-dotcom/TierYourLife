package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends whatever is waiting the moment a connection comes back; otherwise
 * sync only wakes on the way back to the list of boards. Its own scope rather
 * than a screen's, because nobody is looking at the right screen when it matters.
 */
@Singleton
class SyncOnReconnect @Inject constructor(
    private val connection: Connection,
    private val boardSync: BoardSync,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            connection.available.collect {
                runCatching { boardSync.sync() }
                    .onFailure { failure -> Timber.d(failure, "Sync on reconnect did not finish") }
            }
        }
    }
}
