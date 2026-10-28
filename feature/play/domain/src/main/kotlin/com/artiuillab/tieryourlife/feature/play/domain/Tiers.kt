package com.artiuillab.tieryourlife.feature.play.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Groups are accepted once they explain this much of the spread. */
const val ENOUGH_FIT = 0.9

/**
 * Cards cut into [tierCount] tiers, best first, each tier left to right from
 * best. As few groups as explain the spread (Jenks's goodness of variance fit),
 * and each group goes to the tier its strength points at, so a real gap leaves
 * the tiers in between empty and the weakest still land low. Only the first
 * tier is sure to be filled.
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
    val groups = groupsOf(scaled, maxGroups = min(tierCount, scaled.size))
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
 * The fewest groups of consecutive values whose fit reaches [ENOUGH_FIT], at
 * most [maxGroups]. Fisher's exact method; among equal splits the earliest
 * wins, so both platforms pick the same one.
 */
internal fun groupsOf(values: List<Double>, maxGroups: Int): List<IntRange> {
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
    val total = spread(0, n - 1)
    val cost = Array(maxGroups + 1) { DoubleArray(n) }
    val start = Array(maxGroups + 1) { IntArray(n) }
    for (i in 0 until n) cost[1][i] = spread(0, i)
    for (groups in 2..maxGroups) {
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
    val chosen = (1..maxGroups).firstOrNull { groups ->
        total == 0.0 || 1 - cost[groups][n - 1] / total >= ENOUGH_FIT
    } ?: maxGroups
    val ranges = ArrayDeque<IntRange>()
    var end = n - 1
    for (groups in chosen downTo 1) {
        val from = if (groups == 1) 0 else start[groups][end]
        ranges.addFirst(from..end)
        end = from - 1
    }
    return ranges.toList()
}
