package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * The two questions somebody can answer about the copy of their boards, and
 * the one fact worth reporting.
 *
 * There is deliberately no "everything is fine" here. A state that is working
 * does not need announcing, and a screen that says "backed up · 2 minutes ago"
 * teaches people to check it. [stuckSince] is the opposite case, and the only
 * one worth interrupting for: silence while nothing has gone up for a week is
 * the silence that costs somebody their boards.
 */
data class BackupSettings(
    val on: Boolean,
    val picturesOnWifiOnly: Boolean,
    /** Bytes this account is holding in pictures, as far as this phone knows. */
    val storedBytes: Long,
    /** When the last run got through, or null if none ever has. */
    val lastSyncedAtMs: Long?,
) {

    /**
     * When the account was last known to be up to date, if that was long
     * enough ago to be worth saying. A day, because anything shorter catches
     * a phone that was simply switched off overnight.
     */
    fun stuckSince(nowMs: Long): Long? =
        lastSyncedAtMs?.takeIf { on && nowMs - it >= STUCK_AFTER_MS }

    companion object {
        const val STUCK_AFTER_MS = 24 * 60 * 60 * 1000L
    }
}

interface BoardBackup {

    suspend fun settings(): BackupSettings

    fun setPicturesOnWifiOnly(wifiOnly: Boolean)

    /**
     * Turns it on. Nothing else has to happen: the next run works out what the
     * account is missing and sends it.
     */
    fun start()

    /**
     * Turns it off and removes what is already up there.
     *
     * "Off" has to mean the copy is gone, or the switch is a lie: somebody who
     * turns this off has decided their boards are not going to be on somebody
     * else's computer, and leaving them there is the opposite of what they
     * asked for. Nothing local is touched.
     */
    suspend fun stopAndDelete(): Boolean
}
