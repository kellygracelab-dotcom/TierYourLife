package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.tier.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.FeedSort
import com.artiuillab.tieryourlife.feature.tier.domain.model.FollowState
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.SuggestedAuthor
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FakeAppPreferences
import com.artiuillab.tieryourlife.feature.tier.presentation.community.FakeTierRepositoryForCommunity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings screen is read the moment it opens, so what it can only learn
 * later must not be allowed to rearrange it under the reader. These pin the
 * two answers that decide whether a row exists at all.
 */
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {

    @Test
    fun theReportQueue_opensWithWhatItSaidLastTime() = runBlocking {
        val viewModel = viewModel(preferences = FakeAppPreferences(pendingReports = 4))

        assertEquals(4, viewModel.pendingReports.value)
    }

    // The row is the moderator's way in. Losing it because the phone was on a
    // train would be a worse answer than a stale number.
    @Test
    fun aQueueThatCannotBeReached_leavesTheRowWhereItWas() = runBlocking {
        val viewModel = viewModel(
            preferences = FakeAppPreferences(pendingReports = 4),
            community = FakeCommunity(readable = false),
        )

        viewModel.loadPendingReports()

        assertEquals(4, viewModel.pendingReports.value)
    }

    @Test
    fun aQueueThatAnswers_isRememberedForNextTime() = runBlocking {
        val preferences = FakeAppPreferences()
        val viewModel = viewModel(preferences = preferences, community = FakeCommunity(waiting = 2))

        viewModel.loadPendingReports()
        viewModel.pendingReports.first { it == 2 }

        assertEquals(2, preferences.lastKnownPendingReports())
    }

    // Everyone who does not read reports is turned away, and being turned away
    // is the only way the app finds out. It must not turn into a row.
    @Test
    fun somebodyWhoDoesNotReadReports_neverGrowsTheRow() = runBlocking {
        val viewModel = viewModel(community = FakeCommunity(readable = false))

        viewModel.loadPendingReports()

        assertNull(viewModel.pendingReports.value)
    }

    @Test
    fun theTrashCount_opensWithWhatItHeldLastTime() = runBlocking {
        val viewModel = viewModel(preferences = FakeAppPreferences(trashCount = 7))

        assertEquals(7, viewModel.trashCount.value)
    }

    /**
     * The account is [Account.Unknown] for a moment on the way in. Asked then,
     * it answers "no account", and a signed-in person loses the whole section.
     */
    @Test
    fun theBackupSection_waitsForTheAccountRatherThanAskingWhileItIsUnknown() = runBlocking {
        val account = MutableStateFlow<Account>(Account.Unknown)
        val viewModel = viewModel(account = FakeAccountRepository(account))

        viewModel.loadBackupSettings()
        account.value = Account.SignedIn(email = "someone@example.test", photoUrl = null)

        assertNotNull(viewModel.backupSettings.first { it != null })
    }

    @Test
    fun aGuest_hasNoBackupSectionAtAll() = runBlocking {
        val viewModel = viewModel(account = FakeAccountRepository(MutableStateFlow(Account.Guest)))

        viewModel.loadBackupSettings()

        assertNull(viewModel.backupSettings.value)
    }

    private fun viewModel(
        preferences: FakeAppPreferences = FakeAppPreferences(),
        community: CommunityRepository = FakeCommunity(),
        account: AccountRepository = FakeAccountRepository(MutableStateFlow(Account.Guest)),
    ) = SettingsViewModel(
        repository = FakeTierRepositoryForCommunity(),
        accountRepository = account,
        generationCredits = NoCredits,
        community = community,
        backup = FakeBackup,
        preferences = preferences,
    )
}

private object NoCredits : GenerationCredits {
    override suspend fun remaining(): Int? = null
    override fun lastKnown(): Int? = null
}

private object FakeBackup : BoardBackup {
    override suspend fun settings() = BackupSettings(
        on = true,
        picturesOnWifiOnly = true,
        storedBytes = 0,
        lastSyncedAtMs = null,
    )

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit
    override fun start() = Unit
    override suspend fun stopAndDelete(): Boolean = true
}

private class FakeAccountRepository(private val accounts: MutableStateFlow<Account>) : AccountRepository {
    override val account: Flow<Account> = accounts
    override suspend fun signInWithGoogle(idToken: String): SignInOutcome = SignInOutcome.Success
    override suspend fun setDisplayName(name: String): Boolean = true
    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true
    override suspend fun signOut() = Unit
}

private class FakeCommunity(
    private val waiting: Int = 0,
    private val readable: Boolean = true,
) : CommunityRepository {

    override suspend fun reports(): Result<List<ModerationReport>> = if (readable) {
        Result.success(List(waiting) { report(it.toString()) })
    } else {
        Result.failure(IllegalStateException("not yours"))
    }

    private fun report(listId: String) = ModerationReport(
        listId = listId,
        listTitle = "A list",
        authorName = "Someone",
        reasons = listOf(ReportReason.Spam),
        notes = emptyList(),
        reportCount = 1,
        newestAtMillis = 0,
        hidden = false,
        reviewed = false,
    )

    override suspend fun takeDown(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun dismissReports(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
        after: String?,
        sort: FeedSort,
        following: Boolean,
    ): Result<CommunityPage> = Result.success(CommunityPage(emptyList()))

    override suspend fun myPublished(): Result<List<PublishedListSummary>> = Result.success(emptyList())
    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())
    override suspend fun publish(list: TierList): Result<Published> = Result.failure(IllegalStateException())
    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)
    override suspend fun makeFace(pictureId: String): Result<String> =
        Result.success("https://example.test/face.jpg")

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)
    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun follow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun unfollow(authorUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun followState(authorUid: String): Result<FollowState> =
        Result.success(FollowState(following = false, followers = 0))

    override suspend fun suggestedAuthors(): Result<List<SuggestedAuthor>> = Result.success(emptyList())
    override suspend fun noteTaken(publishedId: String): Result<Unit> = Result.success(Unit)
}
