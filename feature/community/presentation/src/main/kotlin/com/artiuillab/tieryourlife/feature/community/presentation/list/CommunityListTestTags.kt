package com.artiuillab.tieryourlife.feature.community.presentation.list

internal object CommunityListTestTags {
    fun showing(which: Showing): String = "community_showing_${which.name.lowercase()}"
    const val SCREEN = "community_list_screen"
    const val SKELETON = "community_list_skeleton"
    const val FOLLOW = "community_list_follow"
    const val SAVE = "community_list_save"
    const val SAVE_CHOICE = "community_list_save_choice"
    const val SAVE_THEIRS = "community_list_save_theirs"
    const val SAVE_MINE = "community_list_save_mine"
    const val OPEN_SAVED = "community_list_open_saved"
    const val STATUS = "community_list_status"
    const val ERROR = "community_list_error"
    const val MORE = "community_list_more"
}
