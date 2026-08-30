package com.artiuillab.tieryourlife.feature.tier.domain.sync

/**
 * Keeping this account's boards and this phone's boards the same.
 *
 * Deliberately one method. Everything a caller might want to say -- push this,
 * fetch that, we are back online -- is the same run over the same three lists,
 * and a caller that could ask for half of it would be a caller that can get it
 * wrong.
 */
interface BoardSync {

    /**
     * Does whatever the two sides currently disagree about. Safe to call
     * often: a run where nothing changed sends nothing.
     */
    suspend fun sync(): SyncReport
}

/**
 * [signedIn] is false when there is nowhere to sync to, which is not a failure
 * -- most people using this app have never signed in.
 */
data class SyncReport(val signedIn: Boolean, val carried: Int = 0, val refused: Int = 0)
