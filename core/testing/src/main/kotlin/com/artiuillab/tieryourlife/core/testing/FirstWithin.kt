package com.artiuillab.tieryourlife.core.testing

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val DEFAULT_WAIT: Duration = 10.seconds

/**
 * [first] that gives up. A state that never arrives fails this test with the
 * last value seen, instead of hanging the CI job until its hour runs out.
 */
suspend fun <T> Flow<T>.firstWithin(
    timeout: Duration = DEFAULT_WAIT,
    predicate: suspend (T) -> Boolean,
): T {
    var last: Any? = NOTHING_YET
    try {
        return withTimeout(timeout) {
            first { value ->
                last = value
                predicate(value)
            }
        }
    } catch (_: TimeoutCancellationException) {
        val seen = if (last === NOTHING_YET) "nothing was emitted" else "the last value was $last"
        throw AssertionError("No value matched within $timeout; $seen")
    }
}

private val NOTHING_YET = Any()
