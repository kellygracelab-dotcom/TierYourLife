package com.artiuillab.tieryourlife.core.logging

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import timber.log.Timber
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
 * behind a snackbar, which are otherwise invisible once it ships. A phone
 * without a connection is not one: the person is told so, and hundreds of
 * reports of it would only bury the failures that are ours.
 */
internal object LogPolicy {

    fun worthReporting(priority: Int): Boolean = priority >= Log.INFO

    fun isNonFatal(priority: Int, error: Throwable?): Boolean =
        error != null && priority >= Log.WARN && !error.isNoConnection()

    /** No DNS, a dropped or timed-out socket, or Firebase saying the same, anywhere down the causes. */
    fun Throwable.isNoConnection(): Boolean =
        generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }.take(MAX_CAUSES).any {
            it is UnknownHostException ||
                it is SocketException ||
                it is SocketTimeoutException ||
                it is FirebaseNetworkException ||
                it.javaClass.name == NO_ADDRESS
        }

    // The platform's own, so never renamed by R8; not public API, so named rather than referenced.
    private const val NO_ADDRESS = "android.system.GaiException"
    private const val MAX_CAUSES = 8

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
