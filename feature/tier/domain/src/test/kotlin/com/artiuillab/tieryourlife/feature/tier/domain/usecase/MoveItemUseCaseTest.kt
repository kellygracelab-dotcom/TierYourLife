package com.artiuillab.tieryourlife.feature.tier.domain.usecase

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveItemUseCaseTest {

    @Test
    fun `moving item removes it from source tier and adds it to target tier`() {
        val item = TierItem(id = 10, title = "Inception", imageUrl = "url")
        val source = Tier(id = 1, label = "Best", color = "#FF0000", items = listOf(item))
        val target = Tier(id = 2, label = "Worst", color = "#0000FF", items = emptyList())
        val list = TierList(id = 1, title = "Movies", tiers = listOf(source, target))

        val result = MoveItemUseCase()(list, itemId = 10, toTierId = 2)

        val resultSource = result.tiers.first { it.id == 1L }
        val resultTarget = result.tiers.first { it.id == 2L }
        assertFalse(resultSource.items.any { it.id == 10L })
        assertTrue(resultTarget.items.any { it.id == 10L })
    }
}