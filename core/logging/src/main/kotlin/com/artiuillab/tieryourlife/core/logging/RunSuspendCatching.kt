package com.artiuillab.tieryourlife.core.logging

import kotlin.coroutines.cancellation.CancellationException

/**
 * runCatching for code that suspends. A cancelled coroutine stays cancelled:
 * plain runCatching hands its CancellationException back as a failure, which
 * then gets logged as a non-fatal and shown as an error nobody had (a search
 * cancelled by the next keystroke flashed the feed's error screen).
 */
inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    runCatching(block).onFailure { if (it is CancellationException) throw it }
