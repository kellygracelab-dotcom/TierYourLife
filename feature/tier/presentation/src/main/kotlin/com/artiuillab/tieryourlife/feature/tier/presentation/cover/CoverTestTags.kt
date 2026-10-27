package com.artiuillab.tieryourlife.feature.tier.presentation.cover

object CoverTestTags {
    const val LISTS = "cover_lists"
    const val COMMUNITY = "cover_community"
    const val OPEN_MAIN = "cover_open_main"
    const val LIST = "cover_list"
    const val OPEN_LIST_ON_MAIN = "cover_list_open_main"
    const val RANK_ON_MAIN = "cover_rank_on_main"
    const val NEXT_LIST = "cover_next_list"
    const val PEEK = "cover_peek"
    const val PEEK_OPEN_MAIN = "cover_peek_open_main"
    const val HAND_OFF = "cover_hand_off"
    const val HAND_OFF_CANCEL = "cover_hand_off_cancel"

    fun listRow(id: Long): String = "cover_list_row_$id"

    fun tierRow(id: Long): String = "cover_tier_row_$id"

    fun more(tierId: Long): String = "cover_more_$tierId"

    fun card(id: Long): String = "cover_card_$id"
}
