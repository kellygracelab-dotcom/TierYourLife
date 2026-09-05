package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * No "everything is fine" here: a working state needs no announcing, and a
 * screen saying "backed up · 2 minutes ago" teaches people to check it.
 * [stuckSince] is the one case worth interrupting for.
 */
data class BackupSettings(
    val on: Boolean,
    val picturesOnWifiOnly: Boolean,
    /** Bytes this account is holding in pictures, as far as this phone knows. */
    val storedBytes: Long,
    /** When the last run got through, or null if none ever has. */
    val lastSyncedAtMs: Long?,
) {

    /** A day: anything shorter catches a phone that was switched off overnight. */
    fun stuckSince(nowMs: Long): Long? =
        lastSyncedAtMs?.takeIf { on && nowMs - it >= STUCK_AFTER_MS }

    companion object {
        const val STUCK_AFTER_MS = 24 * 60 * 60 * 1000L
    }
}

interface BoardBackup {

    suspend fun settings(): BackupSettings

    fun setPicturesOnWifiOnly(wifiOnly: Boolean)

    /** Nothing else has to happen: the next run sends what the account is missing. */
    fun start()

    /** "Off" has to mean the copy is gone, or the switch is a lie. Nothing local is touched. */
    suspend fun stopAndDelete(): Boolean
}
