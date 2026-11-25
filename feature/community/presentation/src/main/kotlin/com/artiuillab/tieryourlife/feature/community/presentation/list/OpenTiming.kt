package com.artiuillab.tieryourlife.feature.community.presentation.list

import timber.log.Timber

/**
 * How long a list took from the tap to its real cards. Past [SLOW_MILLIS] it
 * goes to Crashlytics with what the wait was spent on, since a phone's long
 * blank cards (Danylo's Every US president, 25.11) never showed on the
 * emulator.
 */
internal class OpenTiming(private val now: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }) {
    private val start = now()
    private val phases = mutableListOf<String>()
    private var told = false

    suspend fun <T> phase(name: String, block: suspend () -> T): T {
        val began = now()
        try {
            return block()
        } finally {
            phases += "$name ${now() - began} ms"
        }
    }

    /** The real list is on screen: the report when that was slow, once. */
    fun shown(): String? {
        if (told) return null
        told = true
        val total = now() - start
        if (total < SLOW_MILLIS) return null
        val report = "List open took $total ms: ${phases.joinToString()}"
        Timber.w(SlowListOpen(report), report)
        return report
    }

    companion object {
        const val SLOW_MILLIS = 5_000L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}

internal class SlowListOpen(message: String) : Exception(message)
