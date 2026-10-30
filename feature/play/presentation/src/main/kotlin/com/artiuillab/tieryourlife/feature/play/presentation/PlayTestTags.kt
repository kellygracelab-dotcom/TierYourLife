package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.play.domain.GameKind

object PlayTestTags {
    const val PICKER = "play_picker"
    const val CHOOSE_CARDS = "play_choose_cards"
    const val RESUME_ROW = "play_resume_row"
    const val RESUME = "play_resume"
    const val START = "play_start"
    const val CARDS_PAGE = "play_cards_page"
    const val EVERYTHING = "play_everything"
    const val KEEP_IN_TIER = "play_keep_in_tier"
    const val COUNTER = "play_counter"
    const val CONTINUE = "play_continue"
    const val DUEL = "play_duel"
    const val FIRST_CARD = "play_first_card"
    const val SECOND_CARD = "play_second_card"
    const val SKIP = "play_skip"
    const val UNDO = "play_undo"
    const val QUIT = "play_quit"
    const val RESULT = "play_result"
    const val THIN = "play_thin"
    const val NOTHING_TO_APPLY = "play_nothing_to_apply"
    const val APPLY = "play_apply"
    const val REFINE = "play_refine"
    const val PLAY_AGAIN = "play_again"
    const val CANCEL = "play_cancel"

    fun game(kind: GameKind) = "play_game_${kind.name}"

    fun group(tierId: Long) = "play_group_$tierId"
}
