package com.artiuillab.tieryourlife.feature.play.presentation

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.play.domain.SavedGame
import com.artiuillab.tieryourlife.feature.play.domain.SavedGames
import com.artiuillab.tieryourlife.feature.play.domain.SharedGameJson
import com.artiuillab.tieryourlife.feature.play.domain.isExpired
import com.artiuillab.tieryourlife.feature.play.domain.latestGame
import com.artiuillab.tieryourlife.feature.play.domain.local
import com.artiuillab.tieryourlife.feature.play.domain.progress
import com.artiuillab.tieryourlife.feature.play.domain.shared
import com.artiuillab.tieryourlife.feature.tier.board.play.PlayRequest
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import timber.log.Timber

/**
 * Where a game is put down: always in this phone's file, and on your own board
 * as well, whose `play` travels with the board on the next sync, so the website
 * and your other phones can pick it up. Someone else's list keeps its game in
 * the file alone. Sync runs on its own schedule, never on a write, so a pick
 * costs one small row update and nothing goes out until then.
 */
internal class GameShelf(
    private val games: SavedGames,
    private val boards: TierRepository,
    private val now: () -> Long,
) {

    /** A game with nothing answered is not worth keeping. Returns whether one is kept. */
    suspend fun put(session: Session): Boolean {
        val game = session.game.copy(round = progress(session.game.spec, session.game.history).round)
        if (game.history.isEmpty()) {
            clear(session)
            return false
        }
        val play = if (session.own) encoded(session.board, game) else null
        val onBoard = play != null && write(session.board.id, play)
        games.save(session.key, game.copy(onBoard = onBoard))
        return true
    }

    suspend fun clear(session: Session) {
        games.discard(session.key)
        if (session.own) write(session.board.id, null)
    }

    /**
     * The game put down for [request]'s board: the board's copy when it is the
     * newer, the file's otherwise. A month-old one goes without a word, from both.
     */
    suspend fun take(request: PlayRequest): SavedGame? {
        val inFile = games.load(request.key)
        val board = if (request.intoMine) null else runSuspendCatching { boards.getTierListById(request.board.id) }.getOrNull()
        val game = latestGame(onBoard = board?.let(::gameOn), inFile = inFile)
        if (game != null && game.isExpired(now())) {
            clear(Session(request.key, board ?: request.board, game, own = !request.intoMine))
            return null
        }
        // On the board once and gone from it: it ended elsewhere.
        if (game == null && inFile != null) games.discard(request.key)
        return game
    }

    /** What the file alone says, for the dot on the button before the board is at hand. */
    suspend fun peek(key: String): SavedGame? = games.load(key)?.takeUnless { it.isExpired(now()) }

    private fun gameOn(board: TierList): SavedGame? {
        val shared = board.play?.let(SharedGameJson::decode) ?: return null
        val itemIds = board.tiers.flatMap { it.items }.filter { it.uid.isNotEmpty() }.associate { it.uid to it.id }
        val tierIds = board.tiers.filter { it.uid.isNotEmpty() }.associate { it.uid to it.id }
        return shared.local(itemIds::get, tierIds::get)
    }

    /** Null when a card or tier of the game has no uid here: such a game stays in the file. */
    private fun encoded(board: TierList, game: SavedGame): String? {
        val itemUids = board.tiers.flatMap { it.items }.associate { it.id to it.uid }
        val tierUids = board.tiers.associate { it.id to it.uid }
        return game.shared(itemUids::get, tierUids::get, now())?.let(SharedGameJson::encode)
    }

    /** False when the row did not take it: then only the file has the game. */
    private suspend fun write(boardId: Long, play: String?): Boolean =
        runSuspendCatching { boards.setPlay(boardId, play) }
            .onFailure { Timber.w(it, "Putting the game on the board failed") }
            .isSuccess
}
