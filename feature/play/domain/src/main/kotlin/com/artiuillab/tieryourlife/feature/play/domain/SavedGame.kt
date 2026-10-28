package com.artiuillab.tieryourlife.feature.play.domain

/**
 * A game put down part way. The engine needs only the spec and the history;
 * [ids] say which card each index stands for, so the same game can be picked
 * up after the board has changed under it.
 */
data class SavedGame(
    val spec: GameSpec,
    /** Card ids in game order: the engine's index i is ids[i]. */
    val ids: List<Long>,
    val history: List<Outcome>,
    /** The Swiss round it had reached, to say so before it is resumed; 0 in the other games. */
    val round: Int,
    val startedAt: Long,
    /** The tiers its cards came from, the pool among them; null for the whole board. New cards join only from these. */
    val scope: List<Long>? = null,
    /** One tier's cards, only being ordered inside it. */
    val keepTier: Boolean = false,
)

const val SAVED_GAME_DAYS = 30
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** Gone without a word after a month: nobody remembers which half of a game they were in by then. */
fun SavedGame.isExpired(now: Long): Boolean = now - startedAt > SAVED_GAME_DAYS * DAY_MILLIS

/** What a saved game can be on the board as it is now. */
sealed interface Resumption {
    /** It goes on as [game], renumbered onto the cards there are; [joined] cards are new to it. */
    data class Ready(val game: SavedGame, val joined: Int = 0) : Resumption

    /** A hill or a bracket whose cards changed: the order it was drawn from is gone, so it starts again. */
    data object Changed : Resumption
}

/**
 * The saved game on [cards], the ids in its scope now, in board order. Swiss
 * drops what deleted cards played, renumbers the rest and lets new cards in
 * behind them, up to [MAX_PLAY_CARDS]. Hill and bracket go on only over the
 * very same cards.
 */
fun SavedGame.resumeOn(cards: List<Long>): Resumption {
    val saved = ids.toSet()
    if (spec.kind != GameKind.Swiss) {
        return if (cards.toSet() == saved) Resumption.Ready(this) else Resumption.Changed
    }
    val present = cards.toSet()
    val kept = ids.filter { it in present }
    val joining = cards.filterNot { it in saved }.take((MAX_PLAY_CARDS - kept.size).coerceAtLeast(0))
    if (kept.size == ids.size && joining.isEmpty()) return Resumption.Ready(this)
    val order = kept + joining
    if (order.size < 2) return Resumption.Changed
    val at = order.withIndex().associate { it.value to it.index }
    fun renumbered(card: Int): Int? = at[ids[card]]
    val renumbered = history.mapNotNull { outcome ->
        val first = renumbered(outcome.first)
        val second = renumbered(outcome.second)
        if (first == null || second == null) null else Outcome(first, second, outcome.winner?.let(::renumbered))
    }
    val respec = spec.copy(count = order.size)
    return Resumption.Ready(
        copy(spec = respec, ids = order, history = renumbered, round = progress(respec, renumbered).round),
        joined = joining.size,
    )
}

/**
 * Where games wait between sittings, one per board. On this device for now;
 * the account's board document is where they go next, behind the same door.
 */
interface SavedGames {
    suspend fun load(key: String): SavedGame?

    suspend fun save(key: String, game: SavedGame)

    suspend fun discard(key: String)

    /** The game last started on this device, which the picker offers first. */
    suspend fun lastKind(): GameKind?

    suspend fun rememberKind(kind: GameKind)
}
