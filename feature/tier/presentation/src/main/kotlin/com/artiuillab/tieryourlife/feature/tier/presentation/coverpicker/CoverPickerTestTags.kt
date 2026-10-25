package com.artiuillab.tieryourlife.feature.tier.presentation.coverpicker

object CoverPickerTestTags {
    const val SCREEN = "cover_picker_screen"
    const val PREVIEW = "cover_picker_preview"
    const val STATUS = "cover_picker_status"
    const val DONE = "cover_picker_done"
    const val RESET = "cover_picker_reset"
    const val TAB_CARDS = "cover_picker_tab_cards"
    const val TAB_SEARCH = "cover_picker_tab_search"
    const val GALLERY = "cover_picker_gallery"
    fun candidate(url: String): String = "cover_picker_candidate_$url"
}
