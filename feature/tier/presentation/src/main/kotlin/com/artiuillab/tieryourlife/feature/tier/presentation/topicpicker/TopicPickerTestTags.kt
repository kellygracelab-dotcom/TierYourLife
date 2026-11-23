package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

object TopicPickerTestTags {
    const val SCREEN = "topic_picker_screen"
    const val BACK = "topic_picker_back"
    const val DONE = "topic_picker_done"
    const val QUERY = "topic_picker_query"
    const val NONE = "topic_picker_none"
    const val PROPOSE = "topic_picker_propose"
    const val PROPOSAL = "topic_picker_proposal"
    const val PROPOSAL_SEND = "topic_picker_proposal_send"
    const val CHOOSE_SECTION = "topic_picker_choose_section"
    const val SUGGESTION = "topic_picker_suggestion"
    const val CURRENT = "topic_picker_current"

    fun topic(id: String): String = "topic_picker_topic_$id"
}
