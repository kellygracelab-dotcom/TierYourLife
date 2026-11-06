package com.artiuillab.tieryourlife.feature.tier.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Held by a sync run and by whatever must not happen halfway through one:
 * switching backup off, deleting the account. A run that read the boards
 * before either went on to send them back up.
 */
@Singleton
class SyncLock @Inject constructor() {

    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
