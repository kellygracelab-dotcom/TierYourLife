package com.artiuillab.tieryourlife.feature.tier.domain.ordering

import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TierListMovesTest {

    private fun item(id: Long) = TierItem(id = id, title = "item $id", imageUrl = null)

    private fun tier(id: Long, vararg itemIds: Long, isPool: Boolean = false) = Tier(
        id = id,
        label = "T$id",
        colorLight = "#FFFFFF",
        colorDark = "#000000",
        items = itemIds.map(::item),
        isPool = isPool,
    )

    private fun list(vararg tiers: Tier) = TierList(id = 1L, title = "list", tiers = tiers.toList())

    private fun itemIds(list: TierList, tierId: Long) =
        list.tiers.first { it.id == tierId }.items.map { it.id }

    @Test
    fun `item moves between tiers and lands at the requested index`() {
        val before = list(tier(10, 1, 2), tier(20, 3, 4, isPool = true))

        val after = before.withItemMoved(itemId = 3, toTierId = 10, toPosition = 1)

        assertEquals(listOf(1L, 3L, 2L), itemIds(after, 10))
        assertEquals(listOf(4L), itemIds(after, 20))
    }

    @Test
    fun `moving within one tier removes the item before inserting it`() {
        val before = list(tier(10, 1, 2, 3))

        val after = before.withItemMoved(itemId = 1, toTierId = 10, toPosition = 2)

        assertEquals(listOf(2L, 3L, 1L), itemIds(after, 10))
    }

    @Test
    fun `a position past the end is clamped instead of dropping the item`() {
        val before = list(tier(10, 1), tier(20, 2))

        val after = before.withItemMoved(itemId = 2, toTierId = 10, toPosition = 99)

        assertEquals(listOf(1L, 2L), itemIds(after, 10))
        assertEquals(emptyList<Long>(), itemIds(after, 20))
    }

    @Test
    fun `a negative position is clamped to the front`() {
        val before = list(tier(10, 1, 2), tier(20, 3))

        val after = before.withItemMoved(itemId = 3, toTierId = 10, toPosition = -5)

        assertEquals(listOf(3L, 1L, 2L), itemIds(after, 10))
    }

    @Test
    fun `an unknown item leaves the list untouched`() {
        val before = list(tier(10, 1))

        assertSame(before, before.withItemMoved(itemId = 404, toTierId = 10, toPosition = 0))
    }

    @Test
    fun `an unknown target tier leaves the list untouched`() {
        val before = list(tier(10, 1))

        assertSame(before, before.withItemMoved(itemId = 1, toTierId = 404, toPosition = 0))
    }

    @Test
    fun `tier order and metadata survive the move`() {
        val before = list(tier(10, 1), tier(20, 2, isPool = true))

        val after = before.withItemMoved(itemId = 2, toTierId = 10, toPosition = 0)

        assertEquals(listOf(10L, 20L), after.tiers.map { it.id })
        assertEquals(true, after.tiers.first { it.id == 20L }.isPool)
        assertEquals("T10", after.tiers.first { it.id == 10L }.label)
    }
}
