package com.artiuillab.tieryourlife.feature.tier.board.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MosaicRowsTest {

    @Test
    fun `every count up to six fills its rows with each picture once`() {
        val expected = mapOf(1 to listOf(1), 2 to listOf(2), 3 to listOf(3), 4 to listOf(2, 2), 5 to listOf(3, 2), 6 to listOf(3, 3))

        expected.forEach { (count, rows) ->
            assertEquals("$count pictures", rows, mosaicRows(count))
            assertEquals("$count pictures, none repeated", count, mosaicRows(count).sum())
        }
    }

    @Test
    fun `more than six is shown as six`() {
        assertEquals(listOf(3, 3), mosaicRows(40))
    }
}
