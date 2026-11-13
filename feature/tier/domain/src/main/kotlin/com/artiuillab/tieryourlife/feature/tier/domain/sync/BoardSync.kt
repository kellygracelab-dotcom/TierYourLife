package com.artiuillab.tieryourlife.feature.tier.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Keeping the account's boards and this phone's the same. Deliberately one
 * method: everything a caller might ask for is the same run over the same
 * three lists, and a caller that could ask for half could get it wrong.
 */
interface BoardSync {

    /** Safe to call often: a run where nothing changed sends nothing. */
    suspend fun sync(): SyncReport

    /** Every run as it ends, whoever started it: one started off screen changes what a screen shows. */
    val finished: Flow<SyncReport> get() = emptyFlow()

    /**
     * Each board here that differs from what the account last agreed to, as
     * its uid and how it looks now; empty when there is nowhere to send. Read
     * locally, so cheap enough to ask after every quiet moment.
     */
    suspend fun unsent(): Set<String> = emptySet()

    companion object {
        /** Nowhere to sync to, for a screen made without one. */
        val None: BoardSync = object : BoardSync {
            override suspend fun sync() = SyncReport(signedIn = false)
        }
    }
}

/** [signedIn] is false when there is nowhere to sync to, which is not a failure. */
data class SyncReport(val signedIn: Boolean, val carried: Int = 0, val refused: Int = 0)
