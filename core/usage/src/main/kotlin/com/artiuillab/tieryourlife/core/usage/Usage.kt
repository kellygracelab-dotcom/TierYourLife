package com.artiuillab.tieryourlife.core.usage

/**
 * Anonymous counts for the owner's Statistics screen (proxy CONTRACT.md, Usage statistics):
 * daily sums only, and nothing that names the person or the device ever leaves the phone.
 * Off in Settings, nothing is kept and nothing is sent.
 */
interface Usage {
    /** A newcomer's step: sent once per install, ever. */
    fun step(step: UsageStep)

    fun count(feature: UsageFeature)

    object None : Usage {
        override fun step(step: UsageStep) = Unit

        override fun count(feature: UsageFeature) = Unit
    }
}

/** In the contract's order. */
enum class UsageStep(val wire: String) {
    FirstLaunch("first_launch"),

    /** Opened somebody's list or a house list. */
    OpenList("open_list"),
    FirstBoard("first_board"),
    FirstCard("first_card"),

    /** A board with five or more cards, every one placed. */
    FullList("full_list"),

    /** A game played to its result. */
    FirstGame("first_game"),
    Shared("shared"),
    SignedIn("signed_in"),
    Published("published"),
}

enum class GameMode(val wire: String) {
    Smart("smart"),
    King("king"),
    Bracket("bracket"),
    Losers("losers"),
}

/** Only the names the server counts; anything else it would drop. */
@JvmInline
value class UsageFeature private constructor(val wire: String) {
    companion object {
        val Session = UsageFeature("session")
        val ShareImage = UsageFeature("share.image")
        val ShareLink = UsageFeature("share.link")
        val PictureOwn = UsageFeature("picture.own")
        val PictureLink = UsageFeature("picture.link")
        val BoardTier = UsageFeature("board.tier")
        val BoardRanked = UsageFeature("board.ranked")

        fun game(mode: GameMode, done: Boolean) = UsageFeature("game.${mode.wire}.${if (done) "done" else "start"}")

        /** A picture chosen from search, by where it came from. */
        fun pickedFrom(source: String?) = UsageFeature("picture.pick.${source?.lowercase()?.takeIf { it in SOURCES } ?: "other"}")

        /** Its place in the results, from 0. */
        fun pickedAt(index: Int) = UsageFeature(
            "picture.pos." + when {
                index < 1 -> "1"
                index < FIRST_THREE -> "2-3"
                index < FIRST_TEN -> "4-10"
                else -> "11+"
            },
        )

        private const val FIRST_THREE = 3
        private const val FIRST_TEN = 10
        private val SOURCES = setOf("commons", "tmdb", "igdb", "inaturalist", "wikidata", "deezer", "anilist", "openlibrary", "web")
    }
}

/** A board just finished, every card placed: from five cards it is the funnel's full list. */
fun Usage.listFinished(cards: Int) {
    if (cards >= FULL_LIST_CARDS) step(UsageStep.FullList)
}

/** A board of one's own, made or taken: the funnel's first board, and a count by its kind. */
fun Usage.boardMade(ranked: Boolean) {
    step(UsageStep.FirstBoard)
    count(if (ranked) UsageFeature.BoardRanked else UsageFeature.BoardTier)
}

/** A share, of a picture or a link. */
fun Usage.shared(feature: UsageFeature) {
    step(UsageStep.Shared)
    count(feature)
}

private const val FULL_LIST_CARDS = 5
