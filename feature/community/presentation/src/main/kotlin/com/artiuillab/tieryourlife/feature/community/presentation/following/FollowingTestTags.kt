package com.artiuillab.tieryourlife.feature.community.presentation.following

internal object FollowingTestTags {
    const val SCREEN = "following_screen"
    const val BACK = "following_back"
    const val EMPTY = "following_empty"
    const val FAILED = "following_failed"
    const val TOPICS = "following_topics"

    fun row(uid: String): String = "following_row_$uid"

    fun unfollow(uid: String): String = "following_unfollow_$uid"

    fun topicRow(id: String): String = "following_topic_$id"

    fun unfollowTopic(id: String): String = "following_unfollow_topic_$id"
}
