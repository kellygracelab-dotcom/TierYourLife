package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists

internal object TierListsTestTags {
    const val LOADING = "tier_lists_loading"
    const val LISTS = "tier_lists"
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
    fun tierListCard(id: Long): String = "tier_list_card_$id"
}
