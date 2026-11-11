package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FileRankingDraftsTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun aResetSurvivesReopeningWithoutChangingAnotherAccountsProgress() = runBlocking {
        val original = RankingDraft(7, listOf(listOf(2, 0), listOf(3)), listOf(1))
        val reset = RankingDraft(7, listOf(emptyList(), emptyList()), listOf(1, 2, 0, 3))
        val first = FileRankingDrafts(folder.root)
        first.keep("list", original, "ann")
        first.keep("list", original)
        first.keep("list", reset)

        val reopened = FileRankingDrafts(folder.root)
        assertEquals(reset, reopened.read("list"))
        assertEquals(original, reopened.read("list", "ann"))
        assertNull(reopened.read("list", "bob"))
        assertNull(reopened.read("different-list"))
    }
}
