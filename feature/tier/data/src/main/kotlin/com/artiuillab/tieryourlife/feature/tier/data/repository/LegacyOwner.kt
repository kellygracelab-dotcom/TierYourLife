package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.BoardSyncEntity

/**
 * Whose a board from before boards had owners is, from the accounts this
 * phone agreed on it with ([records], unclaimed ones left out). One account:
 * theirs. Several, which is how one account's boards ended up in another's:
 * the one signed in now if it is among them, else whichever agreed last. None:
 * whoever is signed in now, or this phone's own while nobody is.
 */
internal fun legacyOwner(records: List<BoardSyncEntity>, signedIn: String?): String? {
    val accounts = records.map { it.account }.distinct()
    return when {
        accounts.size == 1 -> accounts.single()
        accounts.isEmpty() -> signedIn
        signedIn in accounts -> signedIn
        else -> records.maxBy { it.syncedAt }.account
    }
}
