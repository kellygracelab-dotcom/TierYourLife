package com.artiuillab.tieryourlife.core.settings

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Something a module keeps on this phone about whoever used it. */
fun interface PersonalData {
    suspend fun forget()
}

/**
 * Whatever ends a person's session on this phone forgets them through here,
 * so a module that keeps something of its own cannot be left out of one path.
 */
@Singleton
class ThePerson @Inject constructor(
    private val preferences: AppPreferences,
    private val kept: Set<@JvmSuppressWildcards PersonalData>,
) {

    suspend fun forget() {
        preferences.forgetThePerson()
        // One that will not forget must not keep the rest from it.
        kept.forEach { data ->
            runSuspendCatching { data.forget() }.onFailure { Timber.w(it, "Could not forget what %s kept", data) }
        }
    }
}
