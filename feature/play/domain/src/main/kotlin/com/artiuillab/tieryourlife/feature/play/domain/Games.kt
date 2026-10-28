package com.artiuillab.tieryourlife.feature.play.domain

import kotlin.math.min

enum class GameKind {
    /** The winner stays and meets the next card. */
    Hill,

    /** Pairs; winners go on until one is left. */
    Bracket,

    /** Swiss rounds: a lost duel knocks nobody out, cards with equal records meet. */
    Swiss,
}

/** What a game is: which, over how many cards, from which shuffle, and whether close calls are asked at the end. */
data class GameSpec(val kind: GameKind, val count: Int, val seed: Int, val refine: Boolean = false)

/** One asked duel: the pair shown, and who won it, or null when it was skipped. */
data class Outcome(val first: Int, val second: Int, val winner: Int?) {
    init {
        require(winner == null || winner == first || winner == second) { "The winner is one of the pair" }
    }

    val duel: Duel? get() = winner?.let { Duel(it, if (it == first) second else first) }

    /** Who goes on when the game needs somebody to: a skip lets the card listed earlier through. */
    val goesOn: Int get() = winner ?: min(first, second)
}

/** Only answered duels count towards strength; a skip is no evidence either way. */
fun List<Outcome>.duels(): List<Duel> = mapNotNull { it.duel }

/**
 * The next pair to ask, or null when the game is over. A game is its spec and
 * its history alone: nothing else is kept, so it resumes anywhere, on either
 * platform, from the same answers.
 */
fun nextPair(spec: GameSpec, history: List<Outcome>): Pair<Int, Int>? {
    if (spec.count < 2) return null
    val main = mainProgress(spec, history)
    if (main.next != null || !spec.refine) return main.next
    val refining = history.drop(main.used)
    return refinePairs(spec.count, history.take(main.used)).getOrNull(refining.size)
}

/** Where a game stands, for the screen that asks it. Read from the spec and history, as everything else is. */
data class GameProgress(
    /** Duels the main game has asked, skips included. */
    val played: Int,
    /** The Swiss round the next pair belongs to, from 1; 0 in the other games and once the main game is over. */
    val round: Int,
    val mainOver: Boolean,
    /** Close calls fixed for refining, and how many of them have been asked; both 0 until refining starts. */
    val refineTotal: Int = 0,
    val refineAsked: Int = 0,
)

fun progress(spec: GameSpec, history: List<Outcome>): GameProgress {
    if (spec.count < 2) return GameProgress(played = 0, round = 0, mainOver = true)
    val main = mainProgress(spec, history)
    val over = main.next == null
    val refineTotal = if (over && spec.refine) refinePairs(spec.count, history.take(main.used)).size else 0
    return GameProgress(
        played = main.used,
        round = if (over) 0 else main.round,
        mainOver = over,
        refineTotal = refineTotal,
        refineAsked = minOf(history.size - main.used, refineTotal),
    )
}

/** How many close calls Refine would ask after this history: what the result offers before it is chosen. */
fun refineDuels(count: Int, history: List<Outcome>): Int = if (count < 2) 0 else refinePairs(count, history).size

private fun mainProgress(spec: GameSpec, history: List<Outcome>): Progress = when (spec.kind) {
    GameKind.Hill -> hillPairs(spec, history)
    GameKind.Bracket -> bracketPairs(spec, history)
    GameKind.Swiss -> swissPairs(spec, history)
}

/** How many duels the main game took, the next one if it is not over, and in Swiss the round that one is in. */
private data class Progress(val used: Int, val next: Pair<Int, Int>?, val round: Int = 0)

private fun hillPairs(spec: GameSpec, history: List<Outcome>): Progress {
    val order = shuffled(spec.count, spec.seed)
    var champion = order[0]
    var challenger = 1
    for (outcome in history) {
        if (challenger >= order.size) break
        champion = outcome.goesOn
        challenger++
    }
    val used = challenger - 1
    return Progress(used, if (challenger < order.size) champion to order[challenger] else null)
}

private fun bracketPairs(spec: GameSpec, history: List<Outcome>): Progress {
    var round = shuffled(spec.count, spec.seed)
    var used = 0
    while (round.size > 1) {
        val next = mutableListOf<Int>()
        for (at in 0 until round.size - 1 step 2) {
            val outcome = history.getOrNull(used) ?: return Progress(used, round[at] to round[at + 1])
            next += outcome.goesOn
            used++
        }
        if (round.size % 2 == 1) next += round.last()
        round = next
    }
    return Progress(used, null)
}

/** ceil(log2 n) + 2 rounds, counted in integers: the smallest r with 2^r >= n, plus two. */
fun swissRounds(count: Int): Int = (Int.SIZE_BITS - Integer.numberOfLeadingZeros(count - 1)) + EXTRA_SWISS_ROUNDS

private const val EXTRA_SWISS_ROUNDS = 2

private fun swissPairs(spec: GameSpec, history: List<Outcome>): Progress {
    var used = 0
    var next: Pair<Int, Int>? = null
    var round = 0
    while (next == null && round < swissRounds(spec.count)) {
        val pairs = swissRound(spec.count, history.take(used))
        if (pairs.isEmpty()) break
        val answered = minOf(pairs.size, history.size - used)
        used += answered
        next = pairs.getOrNull(answered)
        round++
    }
    return Progress(used, next, round)
}

/**
 * One round's pairs: by wins so far, then original order; each card meets the
 * next free card it has not met. A card left without one sits the round out.
 */
internal fun swissRound(count: Int, history: List<Outcome>): List<Pair<Int, Int>> {
    val wins = IntArray(count)
    val met = Array(count) { BooleanArray(count) }
    for (outcome in history) {
        outcome.winner?.let { wins[it]++ }
        met[outcome.first][outcome.second] = true
        met[outcome.second][outcome.first] = true
    }
    val standings = (0 until count).sortedWith(compareByDescending<Int> { wins[it] }.thenBy { it })
    val paired = BooleanArray(count)
    val pairs = mutableListOf<Pair<Int, Int>>()
    for ((at, card) in standings.withIndex()) {
        val partner = standings.drop(at + 1).firstOrNull { !paired[it] && !met[card][it] }
        if (!paired[card] && partner != null) {
            paired[card] = true
            paired[partner] = true
            pairs += card to partner
        }
    }
    return pairs
}

/**
 * Close calls after the game, at most half the cards' worth, each card at most
 * once. First every dashed card, best first, meets its nearest free neighbour
 * it has not met, the smaller gap in strength winning and the better-placed on
 * a tie; then neighbours in the standing whose order is least sure, closest
 * first. Pairs are always better-placed first.
 */
internal fun refinePairs(count: Int, history: List<Outcome>): List<Pair<Int, Int>> {
    val standing = standing(count, history.duels())
    val met = history.flatMap { listOf(it.first to it.second, it.second to it.first) }.toSet()
    val picker = PairPicker(count)
    for ((at, card) in standing.order.withIndex()) {
        if (standing.isDashed(card) && picker.isFree(card)) {
            nearestPartner(standing, at, met, picker)?.let(picker::take)
        }
    }
    closeCalls(standing, met).forEach(picker::take)
    return picker.picked
}

/** Each card at most once, at most half the cards' worth of pairs. */
private class PairPicker(count: Int) {
    private val used = BooleanArray(count)
    private val limit = (count + 1) / 2
    val picked = mutableListOf<Pair<Int, Int>>()

    fun isFree(card: Int) = !used[card]

    fun take(pair: Pair<Int, Int>) {
        if (picked.size < limit && isFree(pair.first) && isFree(pair.second)) {
            used[pair.first] = true
            used[pair.second] = true
            picked += pair
        }
    }
}

/** The free card next to the one at [at] that it has not met, above or below, whichever is closer in strength. */
private fun nearestPartner(standing: Standing, at: Int, met: Set<Pair<Int, Int>>, picker: PairPicker): Pair<Int, Int>? {
    val order = standing.order
    val card = order[at]
    val strength = standing.logStrength
    fun open(other: Int) = picker.isFree(other) && (card to other) !in met
    val above = order.subList(0, at).lastOrNull(::open)
    val below = order.subList(at + 1, order.size).firstOrNull(::open)
    val upGap = above?.let { strength[it] - strength[card] } ?: Double.POSITIVE_INFINITY
    val downGap = below?.let { strength[card] - strength[it] } ?: Double.POSITIVE_INFINITY
    return when {
        above != null && upGap <= downGap -> above to card
        below != null -> card to below
        else -> null
    }
}

/** Neighbours in the standing who have not met, the least sure order first. */
private fun closeCalls(standing: Standing, met: Set<Pair<Int, Int>>): List<Pair<Int, Int>> =
    standing.order.zipWithNext().withIndex()
        .filter { (_, pair) -> pair !in met }
        .sortedWith(
            compareBy<IndexedValue<Pair<Int, Int>>> { (_, pair) ->
                standing.logStrength[pair.first] - standing.logStrength[pair.second]
            }.thenBy { it.index },
        )
        .map { it.value }

/** Where a game leaves its cards: tiers best first, each best on the left. */
fun result(spec: GameSpec, history: List<Outcome>, tierCount: Int): List<List<Int>> =
    tiers(standing(spec.count, history.duels()), tierCount)
