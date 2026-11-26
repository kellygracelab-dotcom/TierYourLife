package com.artiuillab.tieryourlife.feature.tier.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Danylo, 26.11: a new list waits for approval before the feed; each check of what is published says which. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AwaitingApprovalSyncTest : SyncEngineBench() {

    private class Mine(var waiting: Boolean) : FakeCommunityRepository() {
        override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(
            listOf(
                PublishedListSummary(
                    id = "pub-1",
                    title = "Films",
                    authorUid = "",
                    authorName = "",
                    category = "films",
                    itemCount = 1,
                    updatedAtMillis = 0,
                    awaitingApproval = waiting,
                ),
            ),
        )
    }

    @Test
    fun aListWaitingForApproval_isKnownAsSuch_untilApproved() = runBlocking {
        givenLocalBoard("Films", publishedId = "pub-1")
        val preferences = FakeAppPreferences()
        val mine = Mine(waiting = true)

        engine(preferences = preferences, community = mine).sync()
        assertEquals(setOf("pub-1"), preferences.awaitingApproval())

        mine.waiting = false
        engine(preferences = preferences, community = mine).sync()
        assertEquals(emptySet<String>(), preferences.awaitingApproval())
    }
}
