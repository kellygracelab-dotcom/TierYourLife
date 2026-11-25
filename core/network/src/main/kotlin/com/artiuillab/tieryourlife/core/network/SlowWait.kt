package com.artiuillab.tieryourlife.core.network

import timber.log.Timber

internal const val SLOW_WAIT_MILLIS = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L

/**
 * [block], with a breadcrumb when it held the call past [SLOW_WAIT_MILLIS]:
 * a slow list's report then says whether the wait was for a token.
 */
internal fun <T> noteSlow(what: String, now: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }, block: () -> T): T {
    val start = now()
    try {
        return block()
    } finally {
        val waited = now() - start
        if (waited >= SLOW_WAIT_MILLIS) Timber.i("%s took %d ms", what, waited)
    }
}
