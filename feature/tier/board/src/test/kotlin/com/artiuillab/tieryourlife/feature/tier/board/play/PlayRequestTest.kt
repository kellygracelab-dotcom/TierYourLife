package com.artiuillab.tieryourlife.feature.tier.board.play

import com.artiuillab.tieryourlife.feature.tier.board.components.previewTierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRequestTest {

    @Test
    fun someoneElsesList_isKeptPerVersion() {
        val first = PlayRequest.someoneElses("abc", version = 1, previewTierList, previewTierList)
        val again = PlayRequest.someoneElses("abc", version = 2, previewTierList, previewTierList)

        assertEquals("list:abc@1", first.key)
        assertNotEquals(first.key, again.key)
        assertTrue(PlayRequest.sameBoard(first.key, again.key))
    }

    @Test
    fun sameBoard_tellsListsAndBoardsApart() {
        assertTrue(PlayRequest.sameBoard("list:abc@1", "list:abc"))
        assertFalse(PlayRequest.sameBoard("list:abc@1", "list:abcd@1"))
        assertFalse(PlayRequest.sameBoard("board:1", "board:12"))
        assertEquals("board:${previewTierList.id}", PlayRequest.ownBoard(previewTierList).key)
    }
}
