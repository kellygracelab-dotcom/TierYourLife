package com.artiuillab.tieryourlife.feature.community.presentation.components

import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

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
    const val REPORT_DIALOG = "community_report_dialog"
    const val REPORT_NOTE = "community_report_note"
    const val REPORT_SEND = "community_report_send"
    fun communityCard(id: String): String = "community_card_$id"
    fun communityCardAuthor(id: String): String = "community_card_author_$id"
    fun categoryFilter(category: ListCategory?): String = "home_category_${category?.id ?: "all"}"
    fun reportReason(reason: ReportReason): String = "community_report_reason_${reason.id}"
}
