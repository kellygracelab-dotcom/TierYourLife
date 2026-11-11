package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.Ranking
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnedCommunityDefaultsTest {
    @Test fun ownedPublishDefaultsToTheExistingFakePublishOverride() = runBlocking {
        var sent: TierList? = null
        val repository = object : FakeCommunityRepository() {
            override suspend fun publish(list: TierList): Result<Published> {
                sent = list
                return Result.success(Published("published", "fp"))
            }
        }
        val board = TierList(1, "Mine", emptyList())
        assertEquals("published", repository.publishOwned(board, "A").getOrThrow().id)
        assertEquals(board, sent)
    }

    @Test fun versionedOwnedRankingDefaultsToTheExistingFakeRankingOverride() = runBlocking {
        var sentTo: String? = null
        val repository = object : FakeCommunityRepository() {
            override suspend fun showMyRanking(listId: String, arrangement: List<Int?>, rows: List<List<Int>>, tierLabels: List<String>): Result<ShownRanking> {
                sentTo = listId
                return Result.success(ShownRanking(Ranking("A", "A", null, arrangement, tierLabels, 1, rows = rows), true))
            }
        }
        val result = repository.showMyRankingVersioned("source", listOf(0), listOf(listOf(0)), listOf("S"), version = 42, expectedOwnerUid = "A")
        assertEquals("source", sentTo)
        assertEquals(listOf(listOf(0)), result.getOrThrow().ranking.rows)
    }
}
