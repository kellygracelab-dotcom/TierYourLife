package com.artiuillab.tieryourlife.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Only this module knows a crash reporter exists. Only the build Play ships
 * reports: debug, fast and every build from a worktree are ours, on emulators
 * and test devices, and would drown the people who use the app. Debug also
 * prints; release never does.
 */
object Logging {

    /** [report]: whether this is the build Play ships. The manifest keeps the others quiet before this runs. */
    fun install(debug: Boolean, report: Boolean) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = report
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
