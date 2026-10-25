package com.artiuillab.tieryourlife.feature.community.presentation.moderation

internal object TopicQueueTestTags {
    const val MESSAGE = "moderation_topics_message"
    const val RENAME_DIALOG = "moderation_rename_dialog"
    const val RENAME_FIELD = "moderation_rename_field"
    const val RENAME_CONFIRM = "moderation_rename_confirm"
    const val MERGE_DIALOG = "moderation_merge_dialog"
    const val MERGE_FIELD = "moderation_merge_field"
    const val MODERATORS_EMPTY = "moderation_moderators_empty"
    const val APPOINT = "moderation_appoint"
    const val APPOINT_DIALOG = "moderation_appoint_dialog"
    const val APPOINT_UID = "moderation_appoint_uid"
    const val APPOINT_NAME = "moderation_appoint_name"
    const val APPOINT_CONFIRM = "moderation_appoint_confirm"

    fun topicCard(id: String): String = "moderation_topic_$id"

    fun approveTopic(id: String): String = "moderation_approve_$id"

    fun renameTopic(id: String): String = "moderation_rename_$id"

    fun mergeTopic(id: String): String = "moderation_merge_$id"

    fun rejectTopic(id: String): String = "moderation_reject_$id"

    fun mergeCandidate(id: String): String = "moderation_merge_into_$id"

    fun moderatorRow(uid: String): String = "moderation_moderator_$uid"

    fun removeModerator(uid: String): String = "moderation_remove_moderator_$uid"
}
