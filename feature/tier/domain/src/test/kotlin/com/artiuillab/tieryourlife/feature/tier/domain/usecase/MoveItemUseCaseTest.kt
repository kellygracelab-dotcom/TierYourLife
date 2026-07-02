package com.artiuillab.tieryourlife.feature.tier.domain.usecase

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MoveItemUseCaseTest {

    private lateinit var item: TierItem
    private lateinit var list: TierList


    @Before
    fun setup() {
        item = TierItem(id = 10, title = "Inception", imageUrl = "url")
        val source = Tier(id = 1, label = "Best", color = "#FF0000", items = listOf(item))
        val target = Tier(id = 2, label = "Worst", color = "#0000FF", items = emptyList())
        list = TierList(id = 1, title = "Movies", tiers = listOf(source, target))
    }

    @Test
    fun `moving item removes it from source tier and adds it to target tier`() {
        val result = MoveItemUseCase()(list, itemId = 10, toTierId = 2)

        val resultSource = result.tiers.first { it.id == 1L }
        val resultTarget = result.tiers.first { it.id == 2L }
        assertFalse(resultSource.items.any { it.id == 10L })
        assertTrue(resultTarget.items.any { it.id == 10L })
    }

    @Test
    fun `moving item to the tier it is already in keeps it once`() {
        val result = MoveItemUseCase()(list, itemId = 10, toTierId = 1)
        val resultTier = result.tiers[0]
        val timesFound = resultTier.items.count { it.id == 10L }

        assertEquals(1, timesFound)
    }

    @Test(expected = NoSuchElementException::class)
    fun `moving non-existent item throws`() {
        MoveItemUseCase()(list, itemId = 999, toTierId = 2)
    }
}