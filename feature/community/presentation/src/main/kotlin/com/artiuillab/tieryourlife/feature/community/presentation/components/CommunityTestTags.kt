package com.artiuillab.tieryourlife.feature.community.presentation.components

import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason

object CommunityTestTags {
    const val COMMUNITY_LOADING = "home_community_loading"
    const val COMMUNITY_FAILED = "home_community_failed"
    const val COMMUNITY_UNVERIFIED = "home_community_unverified"
    const val COMMUNITY_EMPTY = "home_community_empty"
    const val COMMUNITY_LOADING_MORE = "home_community_loading_more"
    const val COMMUNITY_HIDDEN_TILE = "home_community_hidden_tile"
    const val CATEGORY_FILTERS = "home_category_filters"
    const val LIST_ACTIONS_SHEET = "community_list_actions"
    const val ACTION_VIEW_AUTHOR = "community_action_view_author"
    const val ACTION_HIDE = "community_action_hide"
    const val ACTION_REPORT = "community_action_report"
    const val ACTION_SHOW_RANKING = "community_action_show_ranking"
    const val ACTION_WRONG_TOPIC = "community_action_wrong_topic"
    const val REPORT_DIALOG = "community_report_dialog"
    const val REPORT_NOTE = "community_report_note"
    const val REPORT_SEND = "community_report_send"
    fun communityCard(id: String): String = "community_card_$id"
    fun communityCardAuthor(id: String): String = "community_card_author_$id"
    fun communityCardTopic(id: String): String = "community_card_topic_$id"
    fun categoryFilter(sectionId: String?): String = "home_category_${sectionId ?: "all"}"
    fun reportReason(reason: ReportReason): String = "community_report_reason_${reason.id}"
}
