package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * When a kept feed is old enough to say so, and how often a failed one may
 * try again on its own. It is never good enough to skip asking the server.
 */
object FeedFreshness {

    const val OFFLINE_STRIP_AFTER_MILLIS: Long = 60 * 60 * 1000L

    const val RETRY_GAP_MILLIS: Long = 10 * 1000L

    /** A feed nobody has opened for this long is not kept. */
    const val KEEP_UNOPENED_MILLIS: Long = 14 * 24 * 60 * 60 * 1000L

    /** How many lists of a feed are kept: the first page, which is what a screen opens on. */
    const val KEPT_LISTS: Int = 20

    /** Said only when the last attempt failed and what is on screen is older than an hour. */
    fun showsOfflineStrip(lastAttemptFailed: Boolean, fetchedAtMillis: Long, nowMillis: Long): Boolean =
        lastAttemptFailed && nowMillis - fetchedAtMillis > OFFLINE_STRIP_AFTER_MILLIS

    fun mayRetry(lastRetryMillis: Long?, nowMillis: Long): Boolean =
        lastRetryMillis == null || nowMillis - lastRetryMillis >= RETRY_GAP_MILLIS
}

/** The names feeds are kept under: one per screen that opens on a feed. */
object FeedKeys {
    const val EXPLORE = "explore"
    const val FOLLOWING = "following"

    fun section(sectionId: String, topicId: String? = null): String =
        if (topicId == null) "section:$sectionId" else "section:$sectionId:topic:$topicId"
}
