package com.artiuillab.tieryourlife.feature.community.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Someone else's lists begun, kept in the account for whoever is signed in, so Continue is the same on the
 * phone and the site. A guest's stay on the device, and join the account's when they sign in.
 */
interface StartedSync {
    /** Sends what changed here and takes what changed elsewhere; offline, it leaves both for later. */
    suspend fun sync(): StartedSyncOutcome

    /** A sync soon, without waiting for it, as after a discard or its Undo. */
    fun request()

    /** Each sync that changed what is kept here. */
    val synced: Flow<StartedSyncOutcome>

    data object None : StartedSync {
        override suspend fun sync() = StartedSyncOutcome()
        override fun request() = Unit
        override val synced: Flow<StartedSyncOutcome> = emptyFlow()
    }
}

/** [replaced]: lists here that another device's later arrangement took the place of, which the person is told. */
data class StartedSyncOutcome(val changed: Boolean = false, val replaced: Int = 0)

/** Where a game of [version] of someone else's list is kept, on any device. */
fun startedGameKey(publishedId: String, version: Long): String = "list:$publishedId@$version"
