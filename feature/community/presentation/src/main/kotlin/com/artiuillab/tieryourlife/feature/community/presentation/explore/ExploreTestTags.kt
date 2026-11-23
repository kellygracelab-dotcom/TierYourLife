package com.artiuillab.tieryourlife.feature.community.presentation.explore

internal object ExploreTestTags {
    const val SCREEN = "explore_screen"
    const val FOLLOWING_FEED = "following_feed"
    const val SKELETON = "explore_skeleton"
    const val FAILED = "explore_failed"
    const val OFFLINE_STRIP = "explore_offline_strip"
    const val NEW_LISTS_PILL = "explore_new_lists_pill"
    const val TILE_MENU_FOLLOW = "explore_tile_menu_follow"
    const val FOLLOW_BUTTON = "explore_follow_button"
    const val FOLLOWING_BUTTON = "explore_following_button"
    const val SEARCH = "explore_search"
    const val SEARCH_BROWSE_SECTIONS = "explore_search_browse_sections"
    const val SEARCH_BROWSE_TOPICS = "explore_search_browse_topics"
    const val SEARCH_RESULTS = "explore_search_results"
    const val SEARCH_NOTHING = "explore_search_nothing"
    const val STRIP = "following_strip"
    const val STRIP_MANAGE = "following_strip_manage"
    const val FOLLOWING_EMPTY = "following_empty"
    const val FOLLOWING_NOTHING_PUBLISHED = "following_nothing_published"
    const val COVER_HOME = "explore_cover_home"
    const val ALL_SECTIONS_TILE = "explore_all_sections"
    const val FOLLOW_PICKED = "following_follow_picked"

    /** explore_chip_popular, explore_chip_new, explore_chip_following. */
    fun chip(chip: HomeChip) = "explore_chip_${chip.name.lowercase()}"

    fun tile(sectionId: String) = "explore_tile_$sectionId"
    fun browseSection(sectionId: String) = "explore_search_section_$sectionId"
    fun browseTopic(topicId: String) = "explore_search_topic_$topicId"
    fun miniTile(sectionId: String) = "following_mini_tile_$sectionId"
    fun card(id: String) = "explore_card_$id"

    fun cardAuthor(id: String) = "explore_card_author_$id"
    fun reason(id: String) = "explore_reason_$id"
    fun newBadge(id: String) = "explore_new_badge_$id"
    fun stripItem(key: String) = "following_strip_$key"
    const val TOPIC_SQUARE = "topic_square"
    const val TOPIC_PICTURE = "topic_picture"
}
