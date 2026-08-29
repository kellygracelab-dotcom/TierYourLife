package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

internal object CatalogueSearchTestTags {
    const val ITEM_SEARCH_FIELD = "tier_detail_item_search_field"
    const val ITEM_SEARCH_CLOSE = "tier_detail_item_search_close"
    const val ITEM_SEARCH_CLEAR = "tier_detail_item_search_clear"
    const val ITEM_SEARCH_SELECTED_COUNT = "tier_detail_item_search_selected_count"
    const val ITEM_SEARCH_CONFIRM = "tier_detail_item_search_confirm"
    const val ITEM_SEARCH_TRY_AGAIN = "tier_detail_item_search_try_again"
    const val ITEM_SEARCH_RESULTS_LIST = "tier_detail_item_search_results_list"
    const val ITEM_SEARCH_LOADING_MORE = "tier_detail_item_search_loading_more"
    const val ITEM_SEARCH_BOTTOM_BAR = "tier_detail_item_search_bottom_bar"
    fun itemSearchResult(id: String): String = "tier_detail_item_search_result_$id"
}
