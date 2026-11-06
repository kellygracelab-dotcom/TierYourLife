package com.artiuillab.tieryourlife.feature.tier.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Held by a sync run and by whatever must not happen halfway through one:
 * switching backup off, deleting the account, signing out. A run that read
 * the boards before either went on to send them back up.
 */
@Singleton
class SyncLock @Inject constructor() {

    private val mutex = Mutex()
    private val waitingAhead = AtomicInteger()

    /** Something is waiting that ends the person's session; a run stops at its next step for it. */
    val stopAsked: Boolean get() = waitingAhead.get() > 0

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }

    /** As [withLock], but a run under way stops at its next step rather than finishing first. */
    suspend fun <T> ahead(block: suspend () -> T): T {
        waitingAhead.incrementAndGet()
        try {
            return mutex.withLock { block() }
        } finally {
            waitingAhead.decrementAndGet()
        }
    }
}
