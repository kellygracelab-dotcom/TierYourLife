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

    fun game(kind: GameKind) = "play_game_${kind.name}"

    fun group(tierId: Long) = "play_group_$tierId"
}
