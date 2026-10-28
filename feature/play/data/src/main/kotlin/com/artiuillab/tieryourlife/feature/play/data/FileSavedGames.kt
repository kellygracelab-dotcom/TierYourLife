package com.artiuillab.tieryourlife.feature.play.data

import com.artiuillab.tieryourlife.feature.play.domain.GameKind
import com.artiuillab.tieryourlife.feature.play.domain.GameSpec
import com.artiuillab.tieryourlife.feature.play.domain.Outcome
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Saved games in one small JSON file of their own: the board database does
 * not change for them, and a file that will not read loses a game, never a
 * board. The history is written as the shared spec writes it,
 * [first, second, winner or null], so the account can take it as it is.
 */
class FileSavedGames(
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : SavedGames {

    private val lock = Mutex()
    private var shelf: Shelf? = null

    override suspend fun load(key: String): SavedGame? = lock.withLock { current() }.games[key]?.toGame()

    override suspend fun save(key: String, game: SavedGame) = change { it.copy(games = it.games + (key to GameRecord.of(game))) }

    override suspend fun discard(key: String) = change { it.copy(games = it.games - key) }

    override suspend fun lastKind(): GameKind? = lock.withLock { current() }.last?.let(::kindNamed)

    override suspend fun rememberKind(kind: GameKind) = change { it.copy(last = kind.name) }

    private suspend fun change(edit: (Shelf) -> Shelf) {
        lock.withLock {
            val next = edit(current())
            shelf = next
            withContext(io) { write(next) }
        }
    }

    private suspend fun current(): Shelf = shelf ?: withContext(io) { read() }.also { shelf = it }

    private fun read(): Shelf {
        if (!file.exists()) return Shelf()
        return try {
            json.decodeFromString(Shelf.serializer(), file.readText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Timber.w(e, "Saved games did not read; starting without them")
            Shelf()
        } catch (e: IOException) {
            Timber.w(e, "Saved games could not be opened")
            Shelf()
        }
    }

    /** Written beside and moved over, so a phone switched off mid-write keeps the last whole file. */
    private fun write(shelf: Shelf) {
        try {
            file.parentFile?.mkdirs()
            val next = File(file.path + ".new")
            next.writeText(json.encodeToString(Shelf.serializer(), shelf))
            if (!next.renameTo(file)) {
                next.copyTo(file, overwrite = true)
                next.delete()
            }
        } catch (e: IOException) {
            Timber.w(e, "Saving the game failed")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class Shelf(
    val last: String? = null,
    val games: Map<String, GameRecord> = emptyMap(),
)

@Serializable
private data class GameRecord(
    val kind: String,
    val count: Int,
    val seed: Int,
    val refine: Boolean,
    val ids: List<Long>,
    val history: List<List<Int?>>,
    val round: Int,
    val startedAt: Long,
    val scope: List<Long>? = null,
    val keepTier: Boolean = false,
    val onBoard: Boolean = false,
) {
    /** Null for a record this version cannot read: a game from a newer one is dropped, not guessed at. */
    fun toGame(): SavedGame? {
        val kind = kindNamed(kind) ?: return null
        val outcomes = history.map { entry ->
            val first = entry.getOrNull(0) ?: return null
            val second = entry.getOrNull(1) ?: return null
            val winner = entry.getOrNull(2)
            if (winner != null && winner != first && winner != second) return null
            Outcome(first, second, winner)
        }
        return SavedGame(
            spec = GameSpec(kind, count, seed, refine),
            ids = ids,
            history = outcomes,
            round = round,
            startedAt = startedAt,
            scope = scope,
            keepTier = keepTier,
            onBoard = onBoard,
        )
    }

    companion object {
        fun of(game: SavedGame) = GameRecord(
            kind = game.spec.kind.name,
            count = game.spec.count,
            seed = game.spec.seed,
            refine = game.spec.refine,
            ids = game.ids,
            history = game.history.map { listOf(it.first, it.second, it.winner) },
            round = game.round,
            startedAt = game.startedAt,
            scope = game.scope,
            keepTier = game.keepTier,
            onBoard = game.onBoard,
        )
    }
}

private fun kindNamed(name: String): GameKind? = GameKind.entries.firstOrNull { it.name == name }
