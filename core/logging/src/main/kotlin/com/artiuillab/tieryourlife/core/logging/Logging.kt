package com.artiuillab.tieryourlife.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Every module logs through Timber; only this one knows a crash reporter exists.
 *
 * Both builds report. The usual rule is to keep development crashes out of
 * release figures, but nothing is released yet, so the only runs that exist are
 * the ones on our own phones — silencing those would mean collecting nothing at
 * all. Revisit at release: the debug build should stop reporting the moment
 * real users start.
 *
 * A debug build also prints, which a release build never does.
 */
object Logging {

    fun install(debug: Boolean) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        if (debug) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(CrashlyticsTree(FirebaseCrashReporter))
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
