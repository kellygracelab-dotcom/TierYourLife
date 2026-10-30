package com.artiuillab.tieryourlife.feature.play.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Below this many answered duels per card the evidence is thin: at most [THIN_GROUPS] groups, and a card shows dashed. */
const val SOLID_DUELS = 2

const val THIN_GROUPS = 3

/** A card that met fewer than [SOLID_DUELS] others is drawn dashed in the result. */
fun Standing.isDashed(card: Int): Boolean = played[card] < SOLID_DUELS

/**
 * The result says it rests on little: more than a quarter of the cards dashed.
 * An empty tier between filled ones is not thin; it is the tiering showing a
 * real gap.
 */
fun isThin(standing: Standing): Boolean =
    standing.played.indices.count { standing.isDashed(it) } * QUARTERS > standing.played.size

private const val QUARTERS = 4

/**
 * Cards cut into [tierCount] tiers, best first, each tier left to right from
 * best. On solid evidence there are as many groups as tiers, or as distinct
 * strengths when there are fewer; on thin evidence at most [THIN_GROUPS].
 * Equal strengths never split. With fewer groups than tiers each group goes to
 * the tier its strength points at, so a real gap leaves the tiers between
 * empty and the weakest still land low.
 */
fun tiers(standing: Standing, tierCount: Int): List<List<Int>> {
    require(tierCount >= 1) { "A board has at least one tier" }
    val result = List(tierCount) { mutableListOf<Int>() }
    val order = standing.order
    val values = order.map { standing.logStrength[it] }
    val top = values.firstOrNull() ?: 0.0
    val bottom = values.lastOrNull() ?: 0.0
    if (tierCount == 1 || top == bottom) {
        result[0].addAll(order)
        return result
    }
    val scaled = values.map { (it - bottom) / (top - bottom) }
    val fits = min(tierCount, scaled.distinct().size)
    val groups = groupsOf(scaled, if (standing.duelsPerCard < SOLID_DUELS) min(fits, THIN_GROUPS) else fits)
    var previous = -1
    val placed = groups.mapIndexed { index, range ->
        val mean = range.sumOf { scaled[it] } / range.count()
        val aimed = floor((1 - mean) * (tierCount - 1) + 0.5).toInt()
        val tier = if (index == 0) 0 else max(aimed, previous + 1)
        previous = tier
        tier
    }.toMutableList()
    for (index in placed.indices.reversed()) {
        placed[index] = min(placed[index], tierCount - groups.size + index)
    }
    groups.forEachIndexed { index, range -> range.forEach { result[placed[index]].add(order[it]) } }
    return result
}

/**
 * Exactly [count] groups of consecutive values with the least spread within
 * them, by Fisher's exact method; among equal splits the earliest wins, so
 * both platforms pick the same one.
 */
internal fun groupsOf(values: List<Double>, count: Int): List<IntRange> {
    val n = values.size
    val sums = DoubleArray(n + 1)
    val squares = DoubleArray(n + 1)
    for (i in 0 until n) {
        sums[i + 1] = sums[i] + values[i]
        squares[i + 1] = squares[i] + values[i] * values[i]
    }
    fun spread(from: Int, to: Int): Double {
        val size = to - from + 1
        val sum = sums[to + 1] - sums[from]
        return max(0.0, squares[to + 1] - squares[from] - sum * sum / size)
    }
    val cost = Array(count + 1) { DoubleArray(n) }
    val start = Array(count + 1) { IntArray(n) }
    for (i in 0 until n) cost[1][i] = spread(0, i)
    for (groups in 2..count) {
        for (i in groups - 1 until n) {
            var best = Double.MAX_VALUE
            var bestStart = groups - 1
            for (s in groups - 1..i) {
                val candidate = cost[groups - 1][s - 1] + spread(s, i)
                if (candidate < best) {
                    best = candidate
                    bestStart = s
                }
            }
            cost[groups][i] = best
            start[groups][i] = bestStart
        }
    }
    val ranges = ArrayDeque<IntRange>()
    var end = n - 1
    for (groups in count downTo 1) {
        val from = if (groups == 1) 0 else start[groups][end]
        ranges.addFirst(from..end)
        end = from - 1
    }
    return ranges.toList()
}
