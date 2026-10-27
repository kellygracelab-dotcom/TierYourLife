package com.artiuillab.tieryourlife.feature.community.presentation.explore

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory

internal object ExploreTestTags {
    const val SCREEN = "explore_screen"
    const val TAB_EXPLORE = "explore_tab_explore"
    const val TAB_FOLLOWING = "explore_tab_following"
    const val SKELETON = "explore_skeleton"
    const val FAILED = "explore_failed"
    const val OFFLINE_STRIP = "explore_offline_strip"
    const val NEW_LISTS_PILL = "explore_new_lists_pill"
    const val TILE_MENU_FOLLOW = "explore_tile_menu_follow"
    const val FOLLOW_BUTTON = "explore_follow_button"
    const val FOLLOWING_BUTTON = "explore_following_button"
    const val HEADING = "explore_heading"
    const val SEARCH = "explore_search"
    const val SEARCH_RESULTS = "explore_search_results"
    const val STRIP = "following_strip"
    const val STRIP_MANAGE = "following_strip_manage"
    const val FOLLOWING_EMPTY = "following_empty"
    const val FOLLOWING_NOTHING_PUBLISHED = "following_nothing_published"
    const val COVER_HOME = "explore_cover_home"

    fun tile(section: ListCategory) = "explore_tile_${section.id}"
    fun miniTile(section: ListCategory) = "following_mini_tile_${section.id}"
    fun card(id: String) = "explore_card_$id"
    fun reason(id: String) = "explore_reason_$id"
    fun stripItem(key: String) = "following_strip_$key"
}
