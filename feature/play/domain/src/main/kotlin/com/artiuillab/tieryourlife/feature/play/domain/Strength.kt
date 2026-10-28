package com.artiuillab.tieryourlife.feature.play.domain

import kotlin.math.floor

/** A duel that was answered. Cards are indices into the game's cards, in their original order. */
data class Duel(val winner: Int, val loser: Int)

/** Cards from best to worst, with what put them there. */
data class Standing(
    /** Card indices, best first. */
    val order: List<Int>,
    /** Per card index: the log of its Bradley–Terry strength, rounded to nine places. */
    val logStrength: List<Double>,
    /** Per card index: duels actually won. */
    val wins: List<Int>,
)

const val STRENGTH_ROUNDS = 200

/**
 * Bradley–Terry by Hunter's MM steps, every card updated from the previous
 * round's values at once, so the order cards are listed in changes nothing.
 * Each card also plays a fixed reference card of strength 1 once each way: an
 * unbeaten card stays finite and a card nobody met sits at 1.
 *
 * The log is StrictMath's, which is fdlibm, as V8's is: the website rounds the
 * same nine places from the same bits.
 */
fun standing(count: Int, duels: List<Duel>): Standing {
    val met = Array(count) { IntArray(count) }
    val wins = IntArray(count)
    for (duel in duels) {
        met[duel.winner][duel.loser]++
        met[duel.loser][duel.winner]++
        wins[duel.winner]++
    }
    var strength = DoubleArray(count) { 1.0 }
    repeat(STRENGTH_ROUNDS) {
        val next = DoubleArray(count)
        for (i in 0 until count) {
            var denominator = 0.0
            for (j in 0 until count) {
                if (j != i && met[i][j] > 0) denominator += met[i][j] / (strength[i] + strength[j])
            }
            denominator += 2.0 / (strength[i] + 1.0)
            next[i] = (wins[i] + 1) / denominator
        }
        strength = next
    }
    val logs = strength.map { roundNine(StrictMath.log(it)) }
    val order = (0 until count).sortedWith(
        compareByDescending<Int> { logs[it] }.thenByDescending { wins[it] }.thenBy { it },
    )
    return Standing(order = order, logStrength = logs, wins = wins.toList())
}

internal fun roundNine(value: Double): Double = floor(value * NINE_PLACES + HALF) / NINE_PLACES

private const val NINE_PLACES = 1e9
private const val HALF = 0.5
