package com.artiuillab.tieryourlife.feature.play.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * A game as the account keeps it on its board and the website reads it: the
 * engine's game with cards and tiers named by their uids, which are the same
 * on every device, where [SavedGame] holds this phone's row ids.
 */
data class SharedGame(
    val spec: GameSpec,
    val ids: List<String>,
    val history: List<Outcome>,
    /** The Swiss round being asked, as [progress] says it; 0 in the other games and after the main game. */
    val round: Int,
    val startedAt: Long,
    /** The tiers the cards came from, the pool by its uid too; null for the whole board. */
    val scope: List<String>? = null,
    val keepTier: Boolean = false,
)

/**
 * The JSON both clients write and the proxy checks (readPlay in its sync.ts),
 * in its one normal form: compact, the keys in the order below, `scope` only
 * for part of a board and `keepTier` only when true over a single tier. A
 * game written any other way the proxy writes back differently, and the
 * board's fingerprint would never settle.
 */
object SharedGameJson {

    fun encode(game: SharedGame): String = buildJsonObject {
        put("kind", game.spec.kind.wireName)
        put("count", game.ids.size)
        put("seed", game.spec.seed)
        put("refine", game.spec.refine)
        putJsonArray("ids") { game.ids.forEach { add(it) } }
        putJsonArray("history") {
            game.history.forEach { outcome ->
                addJsonArray {
                    add(outcome.first)
                    add(outcome.second)
                    add(outcome.winner)
                }
            }
        }
        put("round", game.round)
        put("startedAt", game.startedAt)
        game.scope?.let { tiers -> putJsonArray("scope") { tiers.forEach { add(it) } } }
        if (game.keepTier && game.scope?.size == 1) put("keepTier", true)
    }.toString()

    /** Read as strictly as the proxy reads it: anything that is not a whole, valid game is no game. */
    fun decode(raw: String): SharedGame? {
        val game = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject ?: return null
        val kind = GameKind.entries.firstOrNull { it.wireName == game.text("kind") } ?: return null
        val ids = uids(game["ids"], MAX_PLAY_CARDS)?.takeIf { it.size >= MIN_SHARED_CARDS } ?: return null
        val count = game.whole("count")?.takeIf { it == ids.size } ?: return null
        val seed = game.whole("seed") ?: return null
        val refine = (game["refine"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull ?: return null
        val round = game.whole("round")?.takeIf { it >= 0 } ?: return null
        val startedAt = (game["startedAt"] as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull?.takeIf { it >= 1 } ?: return null
        val history = (game["history"] as? JsonArray)?.map { outcome(it, count) ?: return null } ?: return null
        val scope = when (val tiers = game["scope"]) {
            null, JsonNull -> null
            else -> uids(tiers, MAX_PLAY_CARDS)?.takeIf { it.isNotEmpty() } ?: return null
        }
        return SharedGame(
            spec = GameSpec(kind, count, seed, refine),
            ids = ids,
            history = history,
            round = round,
            startedAt = startedAt,
            scope = scope,
            keepTier = game["keepTier"] == JsonPrimitive(true) && scope?.size == 1,
        )
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.whole(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

    /** Distinct, non-blank strings, at most [most] of them; null when the list is anything else. */
    private fun uids(element: JsonElement?, most: Int): List<String>? {
        val list = element as? JsonArray ?: return null
        val uids = list.map { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content?.trim() ?: return null }
        return uids.takeIf { it.size <= most && it.none(String::isEmpty) && it.toSet().size == it.size }
    }

    private fun outcome(element: JsonElement, count: Int): Outcome? {
        val parts = (element as? JsonArray)?.takeIf { it.size == OUTCOME_SIZE } ?: return null
        val (first, second) = parts.take(2).map { (it as? JsonPrimitive)?.takeUnless { value -> value.isString }?.intOrNull ?: return null }
        val winner = parts[2].takeUnless { it is JsonNull }?.let { (it as? JsonPrimitive)?.intOrNull ?: return null }
        val twoCards = first in 0 until count && second in 0 until count && first != second
        val oneOfThem = winner == null || winner == first || winner == second
        return if (twoCards && oneOfThem) Outcome(first, second, winner) else null
    }

    private const val OUTCOME_SIZE = 3

    /** The proxy keeps a game of two cards, one a resumed Swiss game can shrink to. */
    private const val MIN_SHARED_CARDS = 2
}

private val GameKind.wireName: String get() = name.lowercase()

/**
 * The game in the account's words, or null when a card or tier of it has no
 * uid here, which keeps it on this phone alone. It never starts later than [now].
 */
fun SavedGame.shared(itemUid: (Long) -> String?, tierUid: (Long) -> String?, now: Long): SharedGame? {
    val uids = ids.map { id -> itemUid(id)?.takeIf { it.isNotBlank() } ?: return null }
    val tiers = scope?.map { id -> tierUid(id)?.takeIf { it.isNotBlank() } ?: return null }
    return SharedGame(
        spec = spec.copy(count = uids.size),
        ids = uids,
        history = history,
        round = round,
        startedAt = minOf(startedAt, now),
        scope = tiers,
        keepTier = keepTier && tiers?.size == 1,
    )
}

/**
 * The account's game on this phone's board. A card the board does not have
 * takes an id no card has, so the resume rules see it as deleted; a tier it
 * does not have drops out of the scope.
 */
fun SharedGame.local(itemId: (String) -> Long?, tierId: (String) -> Long?): SavedGame = SavedGame(
    spec = spec,
    ids = ids.mapIndexed { at, uid -> itemId(uid) ?: (MISSING_CARD - at) },
    history = history,
    round = round,
    startedAt = startedAt,
    scope = scope?.mapNotNull(tierId),
    keepTier = keepTier,
    onBoard = true,
)

/** Row ids count up from 1; these count down from -1, one per missing card. */
private const val MISSING_CARD = -1L

/**
 * Which copy of a board's game to offer: the one on the board, which the
 * account carries, when it is newer (started later, or as late with more
 * answers); this phone's file otherwise. A file copy that was on the board and
 * is gone from it now ended somewhere else, so it is not offered.
 */
fun latestGame(onBoard: SavedGame?, inFile: SavedGame?): SavedGame? = when {
    onBoard == null -> inFile?.takeUnless { it.onBoard }
    inFile == null -> onBoard
    compareValuesBy(onBoard, inFile, { it.startedAt }, { it.history.size }) > 0 -> onBoard
    else -> inFile
}
