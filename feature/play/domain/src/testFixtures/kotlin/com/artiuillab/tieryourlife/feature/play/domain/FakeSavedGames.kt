package com.artiuillab.tieryourlife.feature.play.domain

import java.util.concurrent.ConcurrentHashMap

/**
 * Games kept in a map, for tests on either side of the store. A test reads it
 * from its own thread while the view model writes on the main one, so it is
 * safe to read between writes, as the file it stands for is.
 */
class FakeSavedGames(
    games: Map<String, SavedGame> = emptyMap(),
    @Volatile var last: GameKind? = null,
) : SavedGames {

    val games: MutableMap<String, SavedGame> = ConcurrentHashMap(games)

    /** False for a full or broken disk: nothing more is written. */
    @Volatile
    var writes = true

    override suspend fun load(key: String): SavedGame? = games[key]

    override suspend fun save(key: String, game: SavedGame): Boolean {
        if (writes) games[key] = game
        return writes
    }

    override suspend fun discard(key: String) {
        games.remove(key)
    }

    override suspend fun keys(): Set<String> = games.keys.toSet()

    override suspend fun lastKind(): GameKind? = last

    override suspend fun rememberKind(kind: GameKind) {
        last = kind
    }
}
