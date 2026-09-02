package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

import com.artiuillab.tieryourlife.feature.tier.domain.lists.BoardSort
import com.artiuillab.tieryourlife.feature.tier.domain.lists.PublishedFilter
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason

internal object TierListsTestTags {
    const val LOADING = "tier_lists_loading"
    const val LISTS = "tier_lists"
    const val VIEW_TOGGLE = "tier_lists_view_toggle"
    const val BOARD_CONTROLS = "tier_lists_controls"
    const val BOARD_SORT = "tier_lists_sort"
    const val BOARD_FILTER_BUTTON = "tier_lists_filter_button"
    const val BOARD_FILTER_SHEET = "tier_lists_filter_sheet"
    const val FAVOURITES_HEADING = "tier_lists_favourites_heading"
    const val OTHERS_HEADING = "tier_lists_others_heading"
    const val SEARCH_FIELD = "home_search_field"
    const val SEARCH_CLOSE = "home_search_close"
    const val SEARCH_CLEAR = "home_search_clear"
    const val SEARCH_RESULTS_COUNT = "home_search_results_count"
    const val SEARCH_NO_RESULTS = "home_search_no_results"
    const val SELECTION_BAR = "home_selection_bar"
    const val SELECTION_CLOSE = "home_selection_close"
    const val SELECTION_DELETE = "home_selection_delete"
    const val FAB = "home_fab"
    const val EMPTY_STATE = "home_empty_state"
    const val COMMUNITY_LOADING = "home_community_loading"
    const val COMMUNITY_FAILED = "home_community_failed"
    const val COMMUNITY_EMPTY = "home_community_empty"
    const val COMMUNITY_LOADING_MORE = "home_community_loading_more"
    const val COMMUNITY_HIDDEN_TILE = "home_community_hidden_tile"
    const val CATEGORY_FILTERS = "home_category_filters"
    const val FEED_SOURCE_FOLLOWING = "home_feed_following"
    const val FEED_SOURCE_EVERYONE = "home_feed_everyone"
    const val FEED_SORT = "home_feed_sort"
    const val FOLLOWING_NOBODY = "home_following_nobody"
    const val SUGGESTED_AUTHOR = "home_suggested_author"
    const val LOCAL_ONLY_CARD = "home_local_only_card"
    const val LOCAL_ONLY_DISMISS = "home_local_only_dismiss"
    const val LOCAL_ONLY_SIGN_IN = "home_local_only_sign_in"
    const val LOCAL_ONLY_FOOTER = "home_local_only_footer"
    const val RESTORING_PICTURES = "home_restoring_pictures"
    const val CONFLICT_BANNER = "home_conflict_banner"
    const val CONFLICT_GOT_IT = "home_conflict_got_it"
    const val CONFLICT_TAG = "home_conflict_tag"
    const val LIST_ACTIONS_SHEET = "community_list_actions"
    const val ACTION_VIEW_AUTHOR = "community_action_view_author"
    const val ACTION_HIDE = "community_action_hide"
    const val ACTION_REPORT = "community_action_report"
    const val REPORT_DIALOG = "community_report_dialog"
    const val REPORT_NOTE = "community_report_note"
    const val REPORT_SEND = "community_report_send"
    fun tierListCard(id: Long): String = "tier_list_card_$id"
    fun tile(id: Long): String = "tier_list_tile_$id"

    fun star(id: Long): String = "tier_list_star_$id"

    fun boardSortOption(sort: BoardSort): String = "tier_lists_sort_${sort.name.lowercase()}"

    fun appliedFilter(key: String): String = "tier_lists_applied_$key"

    fun filterCategory(category: ListCategory?): String =
        "tier_lists_filter_category_${category?.id ?: "any"}"

    fun filterPublished(published: PublishedFilter?): String =
        "tier_lists_filter_published_${published?.name?.lowercase() ?: "any"}"
    fun communityCard(id: String): String = "community_card_$id"

    fun communityCardAuthor(id: String): String = "community_card_author_$id"

    fun categoryFilter(category: ListCategory?): String = "home_category_${category?.id ?: "all"}"
    fun tab(tab: HomeTab): String = "home_tab_${tab.name.lowercase()}"
    fun suggestion(index: Int): String = "home_suggestion_$index"

    fun reportReason(reason: ReportReason): String = "community_report_reason_${reason.id}"
}
