package com.artiuillab.tieryourlife.feature.play.domain

/** Play is offered for this many cards at most; a bigger board picks some first. */
const val MAX_PLAY_CARDS = 64

/** Below this there is nothing for duels to sort out. */
const val MIN_PLAY_CARDS = 3

/**
 * Duels a game asks before any refining. Hill and bracket take exactly one
 * fewer than the cards (a bye is no duel); Swiss at most its rounds times the
 * pairs a round holds, and fewer when a round runs out of cards who have not met.
 */
fun estimatedDuels(kind: GameKind, count: Int): Int = when {
    count < 2 -> 0
    kind == GameKind.Swiss -> swissRounds(count) * (count / 2)
    else -> count - 1
}

/** At 2.2 seconds a duel, in whole minutes rounded half up, never under one. */
fun estimatedMinutes(duels: Int): Int =
    maxOf(1, (duels * TENTHS_PER_DUEL + TENTHS_PER_MINUTE / 2) / TENTHS_PER_MINUTE)

private const val TENTHS_PER_DUEL = 22
private const val TENTHS_PER_MINUTE = 600
