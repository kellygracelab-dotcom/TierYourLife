package com.artiuillab.tieryourlife.feature.community.presentation.feed

internal object CommunityFeedTestTags {
    const val FEED_SOURCE_FOLLOWING = "home_feed_following"
    const val FEED_SOURCE_EVERYONE = "home_feed_everyone"
    const val FEED_SORT = "home_feed_sort"
    const val FOLLOWING_NOBODY = "home_following_nobody"
    const val SUGGESTED_AUTHOR = "home_suggested_author"
    const val FOLLOWED_STRIP = "home_followed_strip"
    const val MANAGE_FOLLOWING = "home_manage_following"
    const val DAY_HEADER = "home_day_header"
    const val YOUR_TOPICS = "home_your_topics"

    fun yourTopic(id: String): String = "home_your_topic_$id"

    fun followedTopic(id: String): String = "home_followed_topic_$id"

    fun followingFilter(filter: FollowingFilter): String = "home_following_filter_${filter.name.lowercase()}"
}
