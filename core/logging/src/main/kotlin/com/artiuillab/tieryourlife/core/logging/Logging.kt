package com.artiuillab.tieryourlife.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Only this module knows a crash reporter exists. Both builds report while
 * nothing is released -- the only runs are ours; at release the debug build
 * should stop. Debug also prints; release never does.
 */
object Logging {

    fun install(debug: Boolean) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        if (debug) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(CrashlyticsTree(FirebaseCrashReporter))
    }

    /** The pseudonymous Firebase uid is the only identifier that reaches the reporter. */
    fun setUser(uid: String?) {
        FirebaseCrashlytics.getInstance().setUserId(uid.orEmpty())
    }

    fun setKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
}

object CrashKeys {
    const val SIGNED_IN = "signed_in"
}

private object FirebaseCrashReporter : CrashReporter {
    override fun log(message: String) = FirebaseCrashlytics.getInstance().log(message)
    override fun recordException(error: Throwable) =
        FirebaseCrashlytics.getInstance().recordException(error)
}
