package com.artiuillab.tieryourlife.feature.tier.domain.repository

/**
 * How many own pictures may still go up today (Proxy CONTRACT.md, `GET /lists/pictures/uploads`): the server
 * removes every one past the day's limit after it lands, so whatever sends them asks first. Asking counts nothing.
 */
fun interface UploadAllowance {

    /** For [count] pictures about to go up, 1-100; null asks only how many are left. */
    suspend fun ask(count: Int?): UploadRoom

    companion object {
        /** Asks nobody: every picture goes, as before the limit. */
        val None = UploadAllowance { UploadRoom.Unknown }
    }
}

sealed interface UploadRoom {
    /** [left] more today; the day starts again at [resetsAt]. */
    data class Left(val left: Int, val resetsAt: Long) : UploadRoom

    /** Nothing more today, or not as many as asked: again at [retryAt]. A [guest] may sign in for more. */
    data class Full(val retryAt: Long, val guest: Boolean) : UploadRoom

    /** No answer: the server is not reached, or knows no limit yet. */
    data object Unknown : UploadRoom
}

/** Whether [count] more go up today; unknown is taken as yes, as before the limit. */
fun UploadRoom.fits(count: Int): Boolean = when (this) {
    is UploadRoom.Left -> left >= count
    is UploadRoom.Full -> false
    UploadRoom.Unknown -> true
}

/** How many of those waiting may go up now. */
fun UploadRoom.budget(): Int = when (this) {
    is UploadRoom.Left -> left.coerceAtLeast(0)
    is UploadRoom.Full -> 0
    UploadRoom.Unknown -> Int.MAX_VALUE
}
