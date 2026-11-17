package com.artiuillab.tieryourlife.feature.community.data.cache

import com.artiuillab.tieryourlife.feature.community.domain.repository.RankingDraft
import com.artiuillab.tieryourlife.feature.community.domain.repository.StartedRanking
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun offerAndFinalChoiceWithLibraryLinkSurviveASecondInstanceAndStayAccountScoped() = runBlocking {
        val progress = RankingDraft(123, listOf(listOf(1), listOf(0)), listOf(2), RankingChoice.Offered, "my-private-board")
        val first = FileRankingDrafts(folder.root)
        first.keep("source", progress, "ann")
        val final = progress.copy(choice = RankingChoice.Private)
        FileRankingDrafts(folder.root).keep("source", final, "ann")
        val reopened = FileRankingDrafts(folder.root)
        assertEquals(final, reopened.read("source", "ann"))
        assertNull(reopened.read("source"))
        assertNull(reopened.read("source", "bob"))
    }

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

    @Test
    fun whatWasBegun_isTheAccountsOwnUnsavedProgress_latestFirst() = runBlocking {
        var clock = 0L
        val drafts = FileRankingDrafts(folder.root) { ++clock }
        drafts.keep("films", RankingDraft(7, listOf(listOf(0, 1), listOf(2)), listOf(3), title = "A24"), "ann")
        drafts.keep("games", RankingDraft(3, listOf(listOf(4)), listOf(0, 1), title = "PS2"), "ann")
        drafts.keep("empty", RankingDraft(1, listOf(emptyList()), listOf(0, 1), title = "Untouched"), "ann")
        drafts.keep("kept", RankingDraft(1, listOf(listOf(0)), listOf(1), savedBoardUid = "board", title = "Saved"), "ann")
        drafts.keep("bobs", RankingDraft(1, listOf(listOf(0)), listOf(1), title = "Bob's"), "bob")

        assertEquals(
            listOf(StartedRanking("games", "PS2", 3, placed = 1, total = 3, keptAt = 2), StartedRanking("films", "A24", 7, placed = 3, total = 4, keptAt = 1)),
            FileRankingDrafts(folder.root).started("ann"),
        )
        assertEquals(listOf("bobs"), drafts.started("bob").map { it.listId })
        assertTrue(drafts.started(null).isEmpty())
    }

    @Test
    fun aRecordFromBeforeTheTitle_stillReads_andStaysOutOfWhatWasBegun() = runBlocking {
        val drafts = FileRankingDrafts(folder.root)
        drafts.keep("old", RankingDraft(5, listOf(listOf(1)), listOf(0), title = "Old"), "ann")
        val file = folder.root.listFiles()!!.single { it.name.endsWith(".json") }
        file.writeText("""{"version":5,"rows":[[1]],"pool":[0],"choice":null}""")

        assertEquals(RankingDraft(5, listOf(listOf(1)), listOf(0)), drafts.read("old", "ann"))
        assertTrue(drafts.started("ann").isEmpty())
    }
}
