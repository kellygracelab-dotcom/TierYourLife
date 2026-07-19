package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierWithItems
import org.junit.Assert.assertEquals
import org.junit.Test

class TierListMapperTest {

    @Test
    fun tier_list_relation_maps_to_domain_with_nested_lists_ordered_by_position() {
        val tierList = TierListEntity(
            id = 1,
            title = "Films",
        )

        val tierS = TierEntity(
            id = 1,
            tierListId = tierList.id,
            position = 1,
            label = "S",
            color = "GOLD",
        )
        val tierA = TierEntity(
            id = 2,
            tierListId = tierList.id,
            position = 2,
            label = "A",
            color = "GREEN",
        )

        val firstItem = TierItemEntity(
            id = 1,
            tierId = tierS.id,
            position = 1,
            title = "First",
            imageUrl = "https://example.com/first.jpg",
        )
        val secondItem = TierItemEntity(
            id = 2,
            tierId = tierS.id,
            position = 2,
            title = "Second",
            imageUrl = "https://example.com/second.jpg",
        )

        val source = TierListWithTiers(
            tierList = tierList,
            tiers = listOf(
                TierWithItems(
                    tier = tierA,
                    items = emptyList(),
                ),
                TierWithItems(
                    tier = tierS,
                    items = listOf(secondItem, firstItem),
                ),
            ),
        )

        val actual = source.toDomain()

        assertEquals(1L, actual.id)
        assertEquals("Films", actual.title)
        assertEquals(listOf("S", "A"), actual.tiers.map { it.label })
        assertEquals(
            listOf("First", "Second"),
            actual.tiers.first().items.map { it.title },
        )
    }
}
