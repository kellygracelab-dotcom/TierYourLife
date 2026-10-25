package com.artiuillab.tieryourlife.feature.community.presentation.list

import org.junit.Assert.assertEquals
import org.junit.Test

class SkeletonTilesTest {

    @Test
    fun theCardsSpreadOverTheRowsFromTheTop() {
        assertEquals(listOf(3, 3, 2, 2, 2), skeletonTiles(itemCount = 12, rows = 5))
    }

    @Test
    fun aRowNeverShowsMoreThanOneLine() {
        assertEquals(listOf(SKELETON_TILES_PER_ROW, SKELETON_TILES_PER_ROW), skeletonTiles(itemCount = 60, rows = 2))
    }

    @Test
    fun noCards_noTiles() {
        assertEquals(listOf(0, 0, 0), skeletonTiles(itemCount = 0, rows = 3))
    }

    @Test
    fun noRows_nothingToFill() {
        assertEquals(emptyList<Int>(), skeletonTiles(itemCount = 5, rows = 0))
    }
}
