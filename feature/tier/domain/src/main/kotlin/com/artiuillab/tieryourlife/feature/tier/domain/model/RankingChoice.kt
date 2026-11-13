package com.artiuillab.tieryourlife.feature.tier.domain.model

/** Remembers the one-time offer/decision; current public state comes from Rankings. */
enum class RankingChoice(val wireName: String) {
    Offered("offered"),
    Private("private"),
    Ranking("ranking"),
    ;

    companion object {
        fun fromWire(value: String?): RankingChoice? = entries.firstOrNull { it.wireName == value }
    }
}
