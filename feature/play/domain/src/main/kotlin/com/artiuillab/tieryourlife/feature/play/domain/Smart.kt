package com.artiuillab.tieryourlife.feature.play.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * Smart (Danylo, 15.11.2026): Losers stay's first rounds, then only the pairs
 * that can still move a card across a tier's edge. Repeats the website's
 * smart.ts and smartPairs bit for bit; docs/play-vectors.json holds the cases
 * both must reach, and _coordination/smart-pairs-2026-11-15 the reasoning.
 */

/** Losers stay's rounds before the edges. */
const val SMART_OPENING = 2

/**
 * A card is contested at an edge while it stands closer to it than this many
 * of its standard errors, the error of a log-strength after `played` duels
 * taken as 2/√(played + 2), the reference card's two duels counted.
 */
const val SMART_MARGIN = 0.25

/** Below this many answered duels a card is contested wherever it stands. */
const val SMART_UNSETTLED = 3

/** From this many answered duels a card is no longer contested anywhere. */
const val SMART_MOST = 8

/** The most duels smart asks: what Losers stay's rounds would, never more. */
fun smartLimit(count: Int): Int = swissRounds(count) * (count / 2)

/** Each card's tier, by index, from a standing cut into [tierCount]. */
internal fun tierOfCards(scored: Standing, tierCount: Int): List<Int> {
    val tierOf = IntArray(scored.order.size)
    tiers(scored, tierCount).forEachIndexed { tier, row -> row.forEach { tierOf[it] = tier } }
    return tierOf.toList()
}

private class Contested(val card: Int, val edge: Int, val across: List<Int>, val closeness: Double)

/**
 * One batch of pairs across the tiers' edges, each card at most once. Each
 * edge between two filled tiers sits halfway in log-strength between the last
 * card above it and the first below; a card of either tier is contested there
 * when it has played fewer than [SMART_UNSETTLED] duels, or fewer than
 * [SMART_MOST] and stands closer than [SMART_MARGIN] of its standard errors.
 * Closest first, the better placed on a tie, then the earlier edge, each meets
 * the free card across it never shown with it that is nearest in strength.
 */
internal fun edgePairs(scored: Standing, tierCount: Int, met: (Int, Int) -> Boolean): List<Pair<Int, Int>> {
    val place = IntArray(scored.order.size)
    scored.order.forEachIndexed { index, card -> place[card] = index }
    val contested = contestedAtEdges(scored, tiers(scored, tierCount).filter { it.isNotEmpty() })
    contested.sortWith(compareBy<Contested> { it.closeness }.thenBy { place[it.card] }.thenBy { it.edge })
    val taken = BooleanArray(scored.order.size)
    val pairs = mutableListOf<Pair<Int, Int>>()
    for (entry in contested) {
        val card = entry.card
        val partner = if (taken[card]) -1 else nearestAcross(card, entry.across, scored.logStrength, place) { taken[it] || met(card, it) }
        if (partner == -1) continue
        taken[card] = true
        taken[partner] = true
        pairs += if (place[card] < place[partner]) card to partner else partner to card
    }
    return pairs
}

/** Every card weighed at each edge it stands next to, in the rows' order. */
private fun contestedAtEdges(scored: Standing, rows: List<List<Int>>): MutableList<Contested> {
    val logStrength = scored.logStrength
    val contested = mutableListOf<Contested>()
    for (edge in 0 until rows.size - 1) {
        val above = rows[edge]
        val below = rows[edge + 1]
        val line = (logStrength[above.last()] + logStrength[below.first()]) / 2
        fun weigh(card: Int, across: List<Int>) {
            val duels = scored.played[card]
            val closeness = (abs(logStrength[card] - line) * sqrt((duels + 2).toDouble())) / 2
            if (duels < SMART_UNSETTLED || (duels < SMART_MOST && closeness < SMART_MARGIN)) {
                contested += Contested(card, edge, across, closeness)
            }
        }
        above.forEach { weigh(it, below) }
        below.forEach { weigh(it, above) }
    }
    return contested
}

/** The card of [across] nearest [card] in strength that is not [closed], the better placed on a tie; -1 when none is open. */
private fun nearestAcross(card: Int, across: List<Int>, logStrength: List<Double>, place: IntArray, closed: (Int) -> Boolean): Int {
    val strength = logStrength[card]
    var partner = -1
    var gap = Double.POSITIVE_INFINITY
    for (other in across) {
        if (closed(other)) continue
        val distance = abs(logStrength[other] - strength)
        if (distance < gap || (distance == gap && place[other] < place[partner])) {
            partner = other
            gap = distance
        }
    }
    return partner
}

/** Whether two cards were ever shown together, skips included. */
internal fun meetings(count: Int, history: List<Outcome>): (Int, Int) -> Boolean {
    val met = HashSet<Long>()
    for (outcome in history) {
        met += outcome.first.toLong() * count + outcome.second
        met += outcome.second.toLong() * count + outcome.first
    }
    return { a, b -> a.toLong() * count + b in met }
}

/** Where a smart game stood when a batch began: the next call goes on from there. */
private class SmartState(val used: Int, val round: Int, val scored: Standing, val placed: List<Int>)

private class SmartMemo(val count: Int, val tierCount: Int, val prefix: List<Outcome>, val state: SmartState)

/**
 * The last batch start worked out, so each answer costs one batch rather than
 * the whole game again. Only a speed-up: a history that does not begin with
 * the remembered one starts over, and both reach the same pairs.
 */
@Volatile
private var smartMemo: SmartMemo? = null

private fun resumed(count: Int, tierCount: Int, history: List<Outcome>): SmartState? {
    val memo = smartMemo ?: return null
    if (memo.count != count || memo.tierCount != tierCount || memo.prefix.size > history.size) return null
    return memo.state.takeIf { memo.prefix == history.subList(0, memo.prefix.size) }
}

/**
 * Swiss's opening rounds, then batches of [edgePairs] from the standing so
 * far, each batch fixed when it starts. The game ends when a batch finds no
 * pair, when a batch's answers move no card to another tier, or at
 * [smartLimit], the last batch cut short there.
 */
internal fun smartPairs(spec: GameSpec, history: List<Outcome>): Progress {
    val count = spec.count
    val tierCount = max(1, min(spec.tiers ?: 1, count))
    val limit = smartLimit(count)
    val state = resumed(count, tierCount, history) ?: opening(count, limit, history).let { start ->
        start.progress?.let { return it }
        val scored = standing(count, history.take(start.used).duels())
        SmartState(start.used, SMART_OPENING, scored, tierOfCards(scored, tierCount))
    }
    var used = state.used
    var round = state.round
    var scored = state.scored
    var placed = state.placed
    // Ends when a batch's answers move no card to another tier.
    var moving = true
    while (moving && used < limit) {
        smartMemo = SmartMemo(count, tierCount, history.take(used), SmartState(used, round, scored, placed))
        round++
        val pairs = edgePairs(scored, tierCount, meetings(count, history.take(used))).take(limit - used)
        if (pairs.isEmpty()) break
        val answered = minOf(pairs.size, history.size - used)
        pairs.getOrNull(answered)?.let { next ->
            return Progress(used + answered, next, round, min(limit, used + 2 * pairs.size))
        }
        used += answered
        scored = standing(count, history.take(used).duels())
        val now = tierOfCards(scored, tierCount)
        moving = now != placed
        placed = now
    }
    return Progress(used, null)
}

private class Opening(val used: Int, val progress: Progress?)

private fun opening(count: Int, limit: Int, history: List<Outcome>): Opening {
    var used = 0
    for (round in 0 until SMART_OPENING) {
        val pairs = swissRound(count, history.take(used))
        if (pairs.isEmpty()) break
        val answered = minOf(pairs.size, history.size - used)
        val next = pairs.getOrNull(answered)
        if (next != null) {
            val rest = (SMART_OPENING - round - 1) * (count / 2)
            return Opening(used, Progress(used + answered, next, round + 1, min(limit, used + pairs.size + rest)))
        }
        used += answered
    }
    return Opening(used, null)
}
