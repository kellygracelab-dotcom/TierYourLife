package com.artiuillab.tieryourlife.core.logging

import android.util.Log
import timber.log.Timber

internal interface CrashReporter {
    fun log(message: String)
    fun recordException(error: Throwable)
}

/**
 * What a release build sends and what it keeps to itself.
 *
 * Debug chatter is dropped. Everything from INFO up becomes a breadcrumb, so a
 * crash report reads as the story that led to it. A warning or error carrying an
 * exception also goes up as a non-fatal: those are the failures the app swallows
 * behind a snackbar, which are otherwise invisible once it ships.
 */
internal object LogPolicy {

    fun worthReporting(priority: Int): Boolean = priority >= Log.INFO

    fun isNonFatal(priority: Int, error: Throwable?): Boolean = error != null && priority >= Log.WARN

    fun breadcrumb(tag: String?, message: String): String =
        if (tag.isNullOrEmpty()) message else "$tag: $message"
}

internal class CrashlyticsTree(private val reporter: CrashReporter) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = LogPolicy.worthReporting(priority)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        reporter.log(LogPolicy.breadcrumb(tag, message))
        if (LogPolicy.isNonFatal(priority, t)) {
            reporter.recordException(t!!)
        }
    }
}
