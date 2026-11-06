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
}

/** [signedIn] is false when there is nowhere to sync to, which is not a failure. */
data class SyncReport(val signedIn: Boolean, val carried: Int = 0, val refused: Int = 0)
