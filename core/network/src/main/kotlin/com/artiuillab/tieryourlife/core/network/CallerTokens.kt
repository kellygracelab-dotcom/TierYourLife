package com.artiuillab.tieryourlife.core.network

import com.google.android.gms.tasks.Task
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * The two tokens a proxy call carries. Neither needs the other, so the call
 * that asks for the App Check token starts the ID token at the same moment: a
 * cold start waits for the slower of the two rather than for both in turn.
 * Calls that arrive while a token is on its way share that fetch, so a burst
 * at launch makes one exchange, and one guest. Firebase keeps a token while it
 * is valid and hands it back without going to the network.
 */
class CallerTokens(
    appCheck: () -> CompletableFuture<String?>,
    idToken: () -> CompletableFuture<String?>,
) {
    private val appCheck = SharedFetch(appCheck)
    private val idToken = SharedFetch(idToken)

    fun appCheckToken(): String? {
        idToken.startAhead()
        return appCheck.fetch().waited("App Check token")
    }

    fun idToken(): String? = idToken.fetch().waited("ID token")

    private fun CompletableFuture<String?>.waited(what: String): String? =
        runCatching { noteSlow(what) { get() } }
            .onFailure { Timber.w((it as? ExecutionException)?.cause ?: it, "%s unavailable", what) }
            .getOrNull()
}

/**
 * One fetch at a time: a caller while one is on its way waits for that one.
 * A fetch started ahead of time is the next caller's even if it has finished.
 */
private class SharedFetch(private val start: () -> CompletableFuture<String?>) {
    private var current: CompletableFuture<String?>? = null
    private var ahead = false

    @Synchronized
    fun startAhead() {
        if (current?.isDone != false) current = started()
        ahead = true
    }

    @Synchronized
    fun fetch(): CompletableFuture<String?> {
        val reused = current?.takeIf { !it.isDone || ahead }
        ahead = false
        return reused ?: started().also { current = it }
    }

    private fun started(): CompletableFuture<String?> =
        runCatching { start() }.getOrElse { failed -> CompletableFuture<String?>().apply { completeExceptionally(failed) } }
}

/** Completed where the task completes, never on the main thread the listener would otherwise need. */
internal fun <T> Task<T>.asFuture(): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    addOnCompleteListener(Executor(Runnable::run)) { task ->
        if (task.isSuccessful) {
            future.complete(task.result)
        } else {
            future.completeExceptionally(task.exception ?: IllegalStateException("Task failed without an exception"))
        }
    }
    return future
}
