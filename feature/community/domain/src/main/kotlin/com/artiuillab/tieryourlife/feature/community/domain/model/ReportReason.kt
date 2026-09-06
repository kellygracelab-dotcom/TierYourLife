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
}
