package com.artiuillab.tieryourlife.feature.tier.presentation.community

internal object CommunityTestTags {
    fun showing(which: Showing): String = "community_showing_${which.name.lowercase()}"

    const val SCREEN = "community_list_screen"
    const val SAVE = "community_list_save"
    const val STATUS = "community_list_status"
    const val ERROR = "community_list_error"
    const val MORE = "community_list_more"
}
