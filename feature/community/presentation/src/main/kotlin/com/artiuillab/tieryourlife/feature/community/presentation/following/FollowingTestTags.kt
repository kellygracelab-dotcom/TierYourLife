package com.artiuillab.tieryourlife.feature.community.presentation.following

internal object FollowingTestTags {
    const val SCREEN = "following_screen"
    const val BACK = "following_back"
    const val EMPTY = "following_empty"
    const val FAILED = "following_failed"

    fun row(uid: String): String = "following_row_$uid"

    fun unfollow(uid: String): String = "following_unfollow_$uid"
}
