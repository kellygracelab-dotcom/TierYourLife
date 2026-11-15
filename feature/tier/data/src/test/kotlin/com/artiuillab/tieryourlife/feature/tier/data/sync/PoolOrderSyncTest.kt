package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.IncomingTier
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptItemDto
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.KeptTierDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The pool keeps its order between devices the way a tier does: each card's position, 0 up, within the pool. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PoolOrderSyncTest : SyncEngineBench() {

    @Test
    fun aCardMovedWithinThePool_goesUpInItsNewPlace() = runBlocking {
        val board = TierListEntity(title = "Fruit", ownerUid = someone)
        dao.addBoard(
            board,
            listOf(
                IncomingTier(tier = tier(0, "S"), items = emptyList()),
                IncomingTier(
                    tier = tier(1, "Pool", pool = true),
                    items = listOf("Apple", "Kiwi", "Mango").mapIndexed { index, title -> TierItemEntity(tierId = 0, position = index, title = title, imageUrl = null) },
                ),
            ),
        )
        engine().sync()
        val id = dao.boardByUid(board.uid)!!.id
        val mango = dao.itemsOf(id).single { it.title == "Mango" }

        database.tierDao().moveItem(mango.id, mango.tierId, 0)
        engine().sync()

        val request = boards.kept.last().second
        val pool = request.tiers.single { it.isPool }.uid
        val sent = request.items.filter { it.tierUid == pool }.sortedBy { it.position }
        assertEquals(listOf("Mango", "Apple", "Kiwi"), sent.map { it.title })
        assertEquals(listOf(0, 1, 2), sent.map { it.position })
    }

    @Test
    fun aPoolOrderedOnAnotherDevice_comesDownInThatOrder() = runBlocking {
        val board = theirBoard("remote-1", title = "Games", revision = 3)
        boards.server["remote-1"] = board.copy(
            tiers = board.tiers + KeptTierDto(uid = "remote-1-pool", position = 1, label = "Pool", colorLight = "#111111", colorDark = "#EEEEEE", isPool = true),
            items = listOf(
                KeptItemDto(uid = "c", tierUid = "remote-1-pool", position = 0, title = "Celeste"),
                KeptItemDto(uid = "a", tierUid = "remote-1-pool", position = 2, title = "Alan Wake"),
                KeptItemDto(uid = "b", tierUid = "remote-1-pool", position = 1, title = "Braid"),
            ),
        )

        engine().sync()

        val id = dao.boardByUid("remote-1")!!.id
        val poolId = dao.tiersOf(id).single { it.isPool }.id
        val here = dao.itemsOf(id).filter { it.tierId == poolId }.sortedBy { it.position }
        assertEquals(listOf("Celeste", "Braid", "Alan Wake"), here.map { it.title })
    }

    private fun tier(position: Int, label: String, pool: Boolean = false) =
        TierEntity(tierListId = 0, position = position, label = label, colorLight = "#111111", colorDark = "#EEEEEE", isPool = pool)
}
