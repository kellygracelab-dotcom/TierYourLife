package com.artiuillab.tieryourlife.feature.play.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedGameTest {

    /** The proxy's functions/test/shared-play-example.json, as both clients must write it. */
    private val sharedExample =
        """{"kind":"swiss","count":4,"seed":7,"refine":false,"ids":["a","b","c","d"],"history":[[0,1,0],[2,3,null]],""" +
            """"round":1,"startedAt":1793146000000,"scope":["t-s"],"keepTier":true}"""

    /** The same file as it sits in the proxy, spaced out. */
    private val sharedExampleAsFiled = """
        {
          "kind": "swiss",
          "count": 4,
          "seed": 7,
          "refine": false,
          "ids": ["a", "b", "c", "d"],
          "history": [[0, 1, 0], [2, 3, null]],
          "round": 1,
          "startedAt": 1793146000000,
          "scope": ["t-s"],
          "keepTier": true
        }
    """.trimIndent()

    private val exampleGame = SharedGame(
        spec = GameSpec(GameKind.Swiss, 4, seed = 7, refine = false),
        ids = listOf("a", "b", "c", "d"),
        history = listOf(Outcome(0, 1, winner = 0), Outcome(2, 3, winner = null)),
        round = 1,
        startedAt = 1_793_146_000_000,
        scope = listOf("t-s"),
        keepTier = true,
    )

    /** The same game on this phone: cards 11..14, the tier 5. */
    private val exampleOnThisPhone = SavedGame(
        spec = GameSpec(GameKind.Swiss, 4, seed = 7, refine = false),
        ids = listOf(11, 12, 13, 14),
        history = exampleGame.history,
        round = 1,
        startedAt = 1_793_146_000_000,
        scope = listOf(5),
        keepTier = true,
    )
    private val itemUids = mapOf(11L to "a", 12L to "b", 13L to "c", 14L to "d")
    private val tierUids = mapOf(5L to "t-s")

    @Test
    fun theSharedExample_isWrittenExactly() {
        val shared = exampleOnThisPhone.shared(itemUids::get, tierUids::get, now = Long.MAX_VALUE)

        assertEquals(sharedExample, shared?.let(SharedGameJson::encode))
    }

    @Test
    fun theSharedExample_readsBackAsTheSameGame() {
        val read = SharedGameJson.decode(sharedExampleAsFiled)

        assertEquals(exampleGame, read)
        val ids = itemUids.entries.associate { (id, uid) -> uid to id }
        val tiers = tierUids.entries.associate { (id, uid) -> uid to id }
        assertEquals(exampleOnThisPhone.copy(onBoard = true), read?.local(ids::get, tiers::get))
    }

    @Test
    fun theWholeBoard_saysNoScope_andKeepTierOnlyWhenTrue() {
        val whole = exampleGame.copy(scope = null, keepTier = false)

        val json = SharedGameJson.encode(whole)

        assertFalse("scope" in json)
        assertFalse("keepTier" in json)
        assertTrue(json.endsWith(""""round":1,"startedAt":1793146000000}"""))
    }

    // Every tier of the scope gone from this board leaves an empty one, which the proxy and the decoder below refuse.
    @Test
    fun anEmptyScope_isWrittenAsTheWholeBoard_andReadsBack() {
        val local = exampleGame.local(mapOf("a" to 11L, "b" to 12L, "c" to 13L, "d" to 14L)::get, mapOf<String, Long>()::get)
        val json = local.shared(itemUids::get, tierUids::get, now = Long.MAX_VALUE)?.let(SharedGameJson::encode)

        assertEquals(SharedGameJson.encode(exampleGame.copy(scope = null, keepTier = false)), json)
        assertEquals(exampleGame.copy(scope = null, keepTier = false), json?.let(SharedGameJson::decode))
    }

    @Test
    fun keepingTheTier_meansNothingOverTwoTiers_andIsNotWritten() {
        val json = SharedGameJson.encode(exampleGame.copy(scope = listOf("t-s", "t-a"), keepTier = true))

        assertFalse("keepTier" in json)
        assertEquals(false, SharedGameJson.decode(json)?.keepTier)
    }

    @Test
    fun aClockRunningAhead_neverStartsAGameLaterThanNow() {
        val shared = exampleOnThisPhone.copy(startedAt = 2_000).shared(itemUids::get, tierUids::get, now = 1_500)

        assertEquals(1_500L, shared?.startedAt)
    }

    @Test
    fun aCardWithoutAUid_keepsTheGameOnThisPhone() {
        assertNull(exampleOnThisPhone.shared({ id -> itemUids[id]?.takeUnless { id == 14L } }, tierUids::get, now = Long.MAX_VALUE))
        assertNull(exampleOnThisPhone.shared({ "" }, tierUids::get, now = Long.MAX_VALUE))
    }

    @Test
    fun cardsAndTiersThisBoardDoesNotHave_comeBackAsDeleted() {
        val local = exampleGame.local(mapOf("a" to 11L, "c" to 13L)::get, mapOf<String, Long>()::get)

        assertEquals(listOf(11L, -2L, 13L, -4L), local.ids)
        assertEquals(emptyList<Long>(), local.scope)
        val resumed = local.copy(scope = null).resumeOn(listOf(11L, 13L)) as Resumption.Ready
        assertEquals(listOf(11L, 13L), resumed.game.ids)
    }

    @Test
    fun anythingButAWholeValidGame_isNoGame() {
        val broken = listOf(
            "not json",
            "[]",
            sharedExample.replace(""""kind":"swiss"""", """"kind":"ladder""""),
            sharedExample.replace(""""count":4""", """"count":5"""),
            sharedExample.replace(""""ids":["a","b","c","d"]""", """"ids":["a","a","c","d"]"""),
            sharedExample.replace("[0,1,0]", "[0,1,2]"),
            sharedExample.replace("[0,1,0]", "[0,0,null]"),
            sharedExample.replace("[0,1,0]", "[0,4,0]"),
            sharedExample.replace(""""round":1""", """"round":-1"""),
            sharedExample.replace(""""startedAt":1793146000000""", """"startedAt":0"""),
            sharedExample.replace(""""refine":false""", """"refine":"false""""),
            sharedExample.replace(""""scope":["t-s"]""", """"scope":[]"""),
            sharedExample.replace(""""seed":7""", """"seed":4294967296"""),
        )
        broken.forEach { raw -> assertNull(raw, SharedGameJson.decode(raw)) }
    }

    @Test
    fun theNewerCopyIsOffered_byStartThenAnswers() {
        val inFile = exampleOnThisPhone
        val longerOnBoard = inFile.copy(history = inFile.history + Outcome(0, 2, winner = 2), onBoard = true)
        val olderOnBoard = inFile.copy(startedAt = inFile.startedAt - 1, onBoard = true)

        assertEquals(longerOnBoard, latestGame(onBoard = longerOnBoard, inFile = inFile))
        assertEquals(inFile, latestGame(onBoard = olderOnBoard, inFile = inFile))
        assertEquals(inFile, latestGame(onBoard = inFile.copy(onBoard = true), inFile = inFile))
        assertEquals(olderOnBoard, latestGame(onBoard = olderOnBoard, inFile = null))
    }

    @Test
    fun aFileCopyGoneFromTheBoard_endedElsewhere_whileOneNeverSentStillCounts() {
        assertNull(latestGame(onBoard = null, inFile = exampleOnThisPhone.copy(onBoard = true)))
        assertEquals(exampleOnThisPhone, latestGame(onBoard = null, inFile = exampleOnThisPhone))
    }
}
