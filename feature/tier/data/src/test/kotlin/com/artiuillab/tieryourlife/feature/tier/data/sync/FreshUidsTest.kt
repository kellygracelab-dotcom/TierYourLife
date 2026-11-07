package com.artiuillab.tieryourlife.feature.tier.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreshUidsTest {

    private val fresh = FreshUids(tierUids = listOf("t1"), itemUids = listOf("a", "b", "c"))

    @Test
    fun aGameNamesTheCopysCardsAndTiers_inTheSameOrder() {
        val game = """{"kind":"swiss","count":3,"seed":7,"refine":false,"ids":["a","b","c"],"history":[[0,1,0]],"round":1,"startedAt":5,"scope":["t1"],"keepTier":true}"""

        val renamed = fresh.play(game)

        val ids = listOf("a", "b", "c").joinToString(",") { "\"${fresh.items.getValue(it)}\"" }
        val expected = """{"kind":"swiss","count":3,"seed":7,"refine":false,"ids":[$ids],"history":[[0,1,0]],"round":1,"startedAt":5,""" +
            """"scope":["${fresh.tiers.getValue("t1")}"],"keepTier":true}"""
        assertEquals(expected, renamed)
    }

    @Test
    fun aGameNamingACardTheCopyDoesNotHave_isNotCarried() {
        assertNull(fresh.play("""{"kind":"hill","count":1,"seed":1,"refine":false,"ids":["z"],"history":[],"round":0,"startedAt":1}"""))
    }

    @Test
    fun noGameOrNoReadableGame_isNoGame() {
        assertNull(fresh.play(null))
        assertNull(fresh.play("not json"))
    }
}
