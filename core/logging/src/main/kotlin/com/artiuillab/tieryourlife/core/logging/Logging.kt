package com.artiuillab.tieryourlife.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Every module logs through Timber; only this one knows a crash reporter exists.
 *
 * Debug builds print and report nothing — a crash on a developer's phone is not
 * news, and mixing the two makes release numbers meaningless.
 */
object Logging {

    fun install(debug: Boolean) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !debug
        Timber.plant(if (debug) Timber.DebugTree() else CrashlyticsTree(FirebaseCrashReporter))
    }

    /**
     * Ties reports to one install. The uid Firebase issues is pseudonymous and
     * is the only identifier that ever reaches the reporter — no email, no name.
     */
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
