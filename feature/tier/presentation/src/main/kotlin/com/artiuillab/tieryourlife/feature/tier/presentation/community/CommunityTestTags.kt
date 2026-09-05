package com.artiuillab.tieryourlife.feature.tier.presentation.community

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason

internal object CommunityTestTags {
    fun showing(which: Showing): String = "community_showing_${which.name.lowercase()}"

    const val SCREEN = "community_list_screen"
    const val FOLLOW = "community_list_follow"
    const val SAVE = "community_list_save"
    const val STATUS = "community_list_status"
    const val ERROR = "community_list_error"
    const val MORE = "community_list_more"

    const val COMMUNITY_LOADING = "home_community_loading"
    const val COMMUNITY_FAILED = "home_community_failed"
    const val COMMUNITY_UNVERIFIED = "home_community_unverified"
    const val COMMUNITY_EMPTY = "home_community_empty"
    const val COMMUNITY_LOADING_MORE = "home_community_loading_more"
    const val COMMUNITY_HIDDEN_TILE = "home_community_hidden_tile"
    const val CATEGORY_FILTERS = "home_category_filters"
    const val FEED_SOURCE_FOLLOWING = "home_feed_following"
    const val FEED_SOURCE_EVERYONE = "home_feed_everyone"
    const val FEED_SORT = "home_feed_sort"
    const val FOLLOWING_NOBODY = "home_following_nobody"
    const val SUGGESTED_AUTHOR = "home_suggested_author"
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
