package com.artiuillab.tieryourlife.feature.tier.data.sync

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs [send] once [changes] have stopped for [quietMs]; a stretch that never
 * stops, a long game picked card by card, is sent [longestMs] after it began.
 * Changes made while [send] runs start the next stretch.
 */
internal suspend fun sendWhenQuiet(
    changes: Flow<*>,
    quietMs: Long,
    longestMs: Long,
    now: () -> Long,
    send: suspend () -> Unit,
) = coroutineScope {
    val signal = Channel<Unit>(Channel.CONFLATED)
    launch { changes.collect { signal.trySend(Unit) } }
    while (true) {
        signal.receive()
        val began = now()
        while (now() - began < longestMs) {
            val left = minOf(quietMs, longestMs - (now() - began))
            withTimeoutOrNull(left) { signal.receive() } ?: break
        }
        send()
    }
}
