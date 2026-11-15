package com.artiuillab.tieryourlife.feature.play.domain

/** Below this there is nothing for duels to sort out. */
const val MIN_PLAY_CARDS = 3

/**
 * Duels a game asks before any refining. Hill and bracket take exactly one
 * fewer than the cards (a bye is no duel); Swiss at most its rounds times the
 * pairs a round holds, and fewer when a round runs out of cards who have not
 * met; smart about two and a half a card, never more than Swiss.
 */
fun estimatedDuels(kind: GameKind, count: Int): Int = when {
    count < 2 -> 0
    kind == GameKind.Swiss -> swissRounds(count) * (count / 2)
    // Math.round(2.5 * count) on the website: half a duel rounds up.
    kind == GameKind.Smart -> minOf(smartLimit(count), (SMART_DUELS_PER_TWO_CARDS * count + 1) / 2)
    else -> count - 1
}

/** Gives about two and a half duels a card. */
private const val SMART_DUELS_PER_TWO_CARDS = 5

/** Swiss and smart may stop early: their counts are only roughly. */
val GameKind.isRough: Boolean get() = this == GameKind.Swiss || this == GameKind.Smart

/** At 2.2 seconds a duel, in whole minutes rounded half up, never under one. */
fun estimatedMinutes(duels: Int): Int =
    maxOf(1, (duels * TENTHS_PER_DUEL + TENTHS_PER_MINUTE / 2) / TENTHS_PER_MINUTE)

private const val TENTHS_PER_DUEL = 22
private const val TENTHS_PER_MINUTE = 600
