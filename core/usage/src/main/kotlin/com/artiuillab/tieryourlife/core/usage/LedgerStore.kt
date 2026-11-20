package com.artiuillab.tieryourlife.core.usage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.artiuillab.tieryourlife.core.network.networkJson
import timber.log.Timber

/** The ledger in its own file, read whole and written whole: one writer at a time. */
internal class LedgerStore(
    private val prefs: SharedPreferences,
    private val first: () -> Ledger,
) {
    @Synchronized
    fun read(): Ledger = prefs.getString(KEY_LEDGER, null)
        ?.let { stored -> runCatching { networkJson.decodeFromString(Ledger.serializer(), stored) }.onFailure { Timber.w(it, "Usage ledger unreadable") }.getOrNull() }
        ?: first().also(::write)

    /** [now] for a crash: the process is about to end, so the write cannot wait. */
    @Synchronized
    fun update(now: Boolean = false, change: (Ledger) -> Ledger) {
        write(change(read()), now)
    }

    fun quietOn(): Long? = prefs.getLong(KEY_QUIET, -1).takeIf { it >= 0 }

    /** Refused for too many batches today: nothing more until the next day. */
    fun quietUntilTomorrow(day: Long) = prefs.edit { putLong(KEY_QUIET, day) }

    private fun write(ledger: Ledger, now: Boolean = false) {
        prefs.edit(commit = now) { putString(KEY_LEDGER, networkJson.encodeToString(Ledger.serializer(), ledger)) }
    }

    companion object {
        const val FILE = "usage_statistics"
        private const val KEY_LEDGER = "ledger"
        private const val KEY_QUIET = "quiet_day"
    }
}
