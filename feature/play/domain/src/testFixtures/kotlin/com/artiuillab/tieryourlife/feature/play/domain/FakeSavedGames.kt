package com.artiuillab.tieryourlife.feature.play.domain

/** Games kept in a map, for tests on either side of the store. */
class FakeSavedGames(
    games: Map<String, SavedGame> = emptyMap(),
    var last: GameKind? = null,
) : SavedGames {

    val games = games.toMutableMap()

    override suspend fun load(key: String): SavedGame? = games[key]

    override suspend fun save(key: String, game: SavedGame) {
        games[key] = game
    }

    override suspend fun discard(key: String) {
        games.remove(key)
    }

    override suspend fun lastKind(): GameKind? = last

    override suspend fun rememberKind(kind: GameKind) {
        last = kind
    }
}
