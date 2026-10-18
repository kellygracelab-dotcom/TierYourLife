package com.artiuillab.tieryourlife.feature.settings.presentation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
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
        viewModel.pendingReports.firstWithin { it == 2 }

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
        val account = FakeAccountRepository(initial = Account.Unknown)
        val viewModel = viewModel(account = account)

        viewModel.loadBackupSettings()
        account.accounts.value = FakeAccountRepository.signedIn

        assertNotNull(viewModel.backupSettings.firstWithin { it != null })
    }

    @Test
    fun aGuest_hasNoBackupSectionAtAll() = runBlocking {
        val viewModel = viewModel(account = FakeAccountRepository())

        viewModel.loadBackupSettings()

        assertNull(viewModel.backupSettings.value)
    }

    private fun viewModel(
        preferences: FakeAppPreferences = FakeAppPreferences(),
        community: CommunityRepository = FakeCommunity(),
        account: AccountRepository = FakeAccountRepository(),
    ) = SettingsViewModel(
        repository = RecordingTierRepository(),
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

private class FakeCommunity(
    private val waiting: Int = 0,
    private val readable: Boolean = true,
) : FakeCommunityRepository() {

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
}
