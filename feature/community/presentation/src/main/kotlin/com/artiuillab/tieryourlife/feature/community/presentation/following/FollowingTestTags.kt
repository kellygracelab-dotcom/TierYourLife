package com.artiuillab.tieryourlife.feature.community.presentation.following

internal object FollowingTestTags {
    const val SCREEN = "following_screen"
    const val BACK = "following_back"
    const val EMPTY = "following_empty"
    const val FAILED = "following_failed"
    const val TOPICS = "following_topics"

    /** By the row's key: [FollowKeys]. */
    fun row(key: String): String = "following_row_$key"

    fun toggle(key: String): String = "following_toggle_$key"
}
