package com.artiuillab.tieryourlife.feature.community.presentation.topic

internal object TopicTestTags {
    const val SCREEN = "topic_screen"
    const val BACK = "topic_back"
    const val NAME = "topic_name"
    const val FOLLOW = "topic_follow"
    const val WAITING = "topic_waiting"

    fun sort(id: String): String = "topic_sort_$id"
}
