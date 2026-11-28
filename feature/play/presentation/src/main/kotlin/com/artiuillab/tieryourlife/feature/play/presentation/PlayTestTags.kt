package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.feature.play.domain.GameKind

object PlayTestTags {
    const val PICKER = "play_picker"
    const val CHOOSE_CARDS = "play_choose_cards"
    const val RESUME_ROW = "play_resume_row"
    const val RESUME = "play_resume"
    const val DISCARD_SAVED = "play_discard_saved"
    const val DISCARDED_ROW = "play_discarded_row"
    const val UNDO_DISCARD = "play_undo_discard"
    const val START = "play_start"
    const val PLACED_WARNING = "play_placed_warning"
    const val CARDS_PAGE = "play_cards_page"
    const val EVERYTHING = "play_everything"
    const val RANKED = "play_ranked"
    const val COUNTER = "play_counter"
    const val CONTINUE = "play_continue"
    const val DUEL = "play_duel"
    const val FIRST_CARD = "play_first_card"
    const val SECOND_CARD = "play_second_card"
    const val SKIP = "play_skip"
    const val UNDO = "play_undo"
    const val QUIT = "play_quit"
    const val READY = "play_ready"
    const val READY_SUMMARY = "play_ready_summary"
    const val SEE_MY_LIST = "play_see_my_list"
    const val SHARE_RESULT = "play_share_result"
    const val READY_RANKINGS = "play_ready_rankings"
    const val PLAY_AGAIN = "play_play_again"

    fun game(kind: GameKind) = "play_game_${kind.name}"

    fun group(tierId: Long) = "play_group_$tierId"
}
