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
    /** A board the account keeps turning away, said at once: unlike a lost connection, waiting does not fix it. */
    val refusal: BackupRefusal? = null,
) {

    /** A day: anything shorter catches a phone that was switched off overnight. */
    fun stuckSince(nowMs: Long): Long? =
        lastSyncedAtMs?.takeIf { on && nowMs - it >= STUCK_AFTER_MS }

    companion object {
        const val STUCK_AFTER_MS = 24 * 60 * 60 * 1000L
    }
}

/** Turned away for what the board is, so sending it again changes nothing until the board changes. */
data class BackupRefusal(val boardTitle: String, val reason: Reason) {

    enum class Reason {
        /** Over the account's size for one board: too many cards or bytes. */
        TooLarge,

        /** More tiers than the account keeps on a board, which only taking a tier off mends. */
        TooManyTiers,

        /** The account already keeps as many boards as it takes. */
        TooManyBoards,

        /** Something on it the account does not accept. */
        NotAccepted,
    }
}

interface BoardBackup {

    suspend fun settings(): BackupSettings

    /** The same, from what this phone already knows, for a screen's first frame; [settings] measures afresh. */
    fun settingsNow(): BackupSettings

    fun setPicturesOnWifiOnly(wifiOnly: Boolean)

    /** Nothing else has to happen: the next run sends what the account is missing. */
    fun start()

    /** "Off" has to mean the copy is gone, or the switch is a lie. Nothing local is touched. */
    suspend fun stopAndDelete(): Boolean
}
