package com.artiuillab.tieryourlife.feature.community.domain.model

/**
 * Five, because five is what a person reads before choosing at random, and
 * because these are the categories one human reviewer can decide without
 * interpreting.
 */
enum class ReportReason(val id: String) {
    Sexual("sexual"),
    Violence("violence"),
    Hate("hate"),
    Spam("spam"),
    Other("other"),

    /** Filed under the wrong section or topic. Never hides a list; a moderator moves it. Offered from a topic page, not in the report dialog. */
    WrongCategory("wrong_category"),
    ;

    companion object {
        /** The five a person reads before choosing at random; the sixth has its own button. */
        val offered: List<ReportReason> get() = entries.filterNot { it == WrongCategory }
    }
}
