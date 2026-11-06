package com.artiuillab.tieryourlife.feature.play.domain

/** Games kept in a map, for tests on either side of the store. */
class FakeSavedGames(
    games: Map<String, SavedGame> = emptyMap(),
    var last: GameKind? = null,
) : SavedGames {

    val games = games.toMutableMap()

    /** False for a full or broken disk: nothing more is written. */
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
