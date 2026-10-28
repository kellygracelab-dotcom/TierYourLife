package com.artiuillab.tieryourlife.feature.play.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

/** The vectors both platforms check: docs/play-vectors.json at the repository's root. */
internal val vectorsFile: File = generateSequence(File("").absoluteFile) { it.parentFile }
    .map { File(it, "docs/play-vectors.json") }
    .first { it.parentFile.isDirectory && File(it.parentFile.parentFile, "settings.gradle.kts").isFile }

internal data class StandingCase(val name: String, val count: Int, val tierCount: Int, val duels: List<Duel>)

internal data class GameCase(
    val name: String,
    val spec: GameSpec,
    val tierCount: Int,
    val answers: String,
    val script: List<String> = emptyList(),
)

internal val standingCases = listOf(
    StandingCase("nothing to rank", 0, 5, emptyList()),
    StandingCase("one card", 1, 5, emptyList()),
    StandingCase("no duels yet", 4, 5, emptyList()),
    StandingCase("a chain", 5, 5, listOf(Duel(0, 1), Duel(1, 2), Duel(2, 3), Duel(3, 4))),
    StandingCase(
        "two clear groups leave the middle empty",
        6,
        5,
        (0..2).flatMap { top -> (3..5).map { bottom -> Duel(top, bottom) } },
    ),
    StandingCase("an unbeaten card stays finite", 3, 3, listOf(Duel(0, 1), Duel(0, 1), Duel(0, 2), Duel(0, 2), Duel(1, 2))),
    StandingCase("a cycle is a tie, broken by original order", 3, 4, listOf(Duel(0, 1), Duel(1, 2), Duel(2, 0))),
    StandingCase("a single tier holds everything", 4, 1, listOf(Duel(3, 2), Duel(2, 1), Duel(1, 0))),
    StandingCase("more tiers than cards", 3, 6, listOf(Duel(2, 1), Duel(2, 0), Duel(1, 0), Duel(2, 1))),
)

internal val gameCases = listOf(
    GameCase("hill, earlier card wins", GameSpec(GameKind.Hill, 7, seed = 7), 5, "lowerWins"),
    GameCase("hill, later card wins, then close calls", GameSpec(GameKind.Hill, 7, seed = 7, refine = true), 5, "higherWins"),
    GameCase("bracket of seven, earlier card wins", GameSpec(GameKind.Bracket, 7, seed = 42), 5, "lowerWins"),
    GameCase("bracket of eight with skips", GameSpec(GameKind.Bracket, 8, seed = 3), 4, "script", listOf("first", "skip", "second")),
    GameCase("swiss of five, earlier card wins", GameSpec(GameKind.Swiss, 5, seed = 1), 5, "lowerWins"),
    GameCase("swiss of nine, later card wins, then close calls", GameSpec(GameKind.Swiss, 9, seed = 99, refine = true), 5, "higherWins"),
    GameCase("swiss of two runs out of new pairs", GameSpec(GameKind.Swiss, 2, seed = 5), 3, "lowerWins"),
)

/** Plays a case to the end with its answer rule. */
internal fun play(case: GameCase): List<Outcome> {
    val history = mutableListOf<Outcome>()
    var pair = nextPair(case.spec, history)
    while (pair != null) {
        val (first, second) = pair
        val winner = when (case.answers) {
            "lowerWins" -> minOf(first, second)
            "higherWins" -> maxOf(first, second)
            else -> when (case.script[history.size % case.script.size]) {
                "first" -> first
                "second" -> second
                else -> null
            }
        }
        history += Outcome(first, second, winner)
        pair = nextPair(case.spec, history)
    }
    return history
}

internal fun buildVectors(): JsonObject = buildJsonObject {
    put("version", 1)
    put("mulberry32", randomVectors())
    put("shuffle", shuffleVectors())
    put("standings", standingVectors())
    put("games", gameVectors())
}

private fun randomVectors() = buildJsonArray {
    listOf(42, 0, -1).forEach { seed ->
        val random = Mulberry32(seed)
        add(
            buildJsonObject {
                put("seed", seed)
                putJsonArray("first") { repeat(5) { add(random.nextUInt()) } }
            },
        )
    }
}

private fun shuffleVectors() = buildJsonArray {
    listOf(7 to 7, 8 to 3, 10 to 123).forEach { (count, seed) ->
        add(
            buildJsonObject {
                put("count", count)
                put("seed", seed)
                putJsonArray("order") { shuffled(count, seed).forEach { add(it) } }
            },
        )
    }
}

private fun standingVectors() = buildJsonArray {
    standingCases.forEach { case ->
        val standing = standing(case.count, case.duels)
        add(
            buildJsonObject {
                put("name", case.name)
                put("count", case.count)
                put("tierCount", case.tierCount)
                putJsonArray("duels") {
                    case.duels.forEach { duel ->
                        addJsonArray {
                            add(duel.winner)
                            add(duel.loser)
                        }
                    }
                }
                putJsonObject("expect") {
                    putJsonArray("order") { standing.order.forEach { add(it) } }
                    putJsonArray("logStrength") { standing.logStrength.forEach { add(it) } }
                    putJsonArray("wins") { standing.wins.forEach { add(it) } }
                    putJsonArray("played") { standing.played.forEach { add(it) } }
                    val placed = tiers(standing, case.tierCount)
                    put("tiers", tiersJson(placed))
                    put("thin", isThin(standing))
                }
            },
        )
    }
}

private fun gameVectors() = buildJsonArray {
    gameCases.forEach { case ->
        val history = play(case)
        add(
            buildJsonObject {
                put("name", case.name)
                put("kind", case.spec.kind.name.lowercase())
                put("count", case.spec.count)
                put("seed", case.spec.seed)
                put("refine", case.spec.refine)
                put("tierCount", case.tierCount)
                put("answers", case.answers)
                if (case.script.isNotEmpty()) putJsonArray("script") { case.script.forEach { add(it) } }
                putJsonObject("expect") {
                    put("history", historyJson(history))
                    val placed = result(case.spec, history, case.tierCount)
                    put("tiers", tiersJson(placed))
                    put("thin", isThin(standing(case.spec.count, history.duels())))
                }
            },
        )
    }
}

private fun historyJson(history: List<Outcome>): JsonElement = buildJsonArray {
    history.forEach { outcome ->
        addJsonArray {
            add(outcome.first)
            add(outcome.second)
            add(outcome.winner?.let { JsonPrimitive(it) } ?: JsonNull)
        }
    }
}

private fun tiersJson(tiers: List<List<Int>>): JsonElement = buildJsonArray {
    tiers.forEach { tier -> add(JsonArray(tier.map { JsonPrimitive(it) })) }
}
