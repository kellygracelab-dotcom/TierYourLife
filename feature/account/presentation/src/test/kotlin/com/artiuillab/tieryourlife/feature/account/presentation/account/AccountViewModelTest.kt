package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private var picker = FakeGoogleCredential(GoogleCredentialResult.Cancelled)

    @Test
    fun signIn_whenGoogleReturnsAToken_handsItToTheRepository() = runBlocking {
        val repository = FakeAccountRepository()
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("token-1")))

        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { !it.signingIn }

        assertEquals(listOf("token-1"), repository.tokens)
    }

    // Closing the picker is a decision, not a fault: nothing should be said
    // about it, and the button has to come back to life.
    @Test
    fun signIn_whenThePickerIsClosed_saysNothingAndStopsWaiting() = runBlocking {
        val repository = FakeAccountRepository()
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Cancelled))

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { !it.signingIn }

        assertNull(state.notice)
        assertEquals(emptyList<String>(), repository.tokens)
    }

    @Test
    fun signIn_withoutAGoogleAccountOnThePhone_saysSo() = runBlocking {
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.NoGoogleAccount))

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.NoGoogleAccount, state.notice)
    }

    // Before Google sign-in is switched on in Firebase there is no web client
    // id to ask with. That is a configuration state, not a broken sign-in.
    @Test
    fun signIn_whenSigningInIsNotConfigured_reportsItSeparately() = runBlocking {
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.Unavailable))

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.SignInUnavailable, state.notice)
    }

    @Test
    fun signIn_whenTheRepositoryFails_reportsAFailure() = runBlocking {
        val repository = FakeAccountRepository(outcome = SignInOutcome.Failed)
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("token-1")))

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.SignInFailed, state.notice)
    }

    // Signing into a Google account that already had an identity gives up the
    // guest credits. Saying nothing would leave a balance that silently changed.
    @Test
    fun signIn_ontoAnAccountThatKeptItsOwnCredits_saysWhatHappened() = runBlocking {
        val repository = FakeAccountRepository(
            outcome = SignInOutcome.SignedInToExistingAccount(creditsCarriedOver = false),
        )
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")))

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.SignedInToExistingAccount, state.notice)
    }

    @Test
    fun signIn_twice_doesNotOpenThePickerAgainWhileTheFirstIsRunning() = runBlocking<Unit> {
        val credential = FakeGoogleCredential()
        val viewModel = viewModel(credential = credential)

        viewModel.signIn(picker::request)
        viewModel.signIn(picker::request)
        // The request is launched onto the main thread; counting before that thread
        // has caught up would find no request, or miss a second one.
        withContext(Dispatchers.Main) {}

        assertEquals(1, credential.requests)
        credential.release(GoogleCredentialResult.Cancelled)
        viewModel.state.firstWithin { !it.signingIn }
    }

    // Both halves of the switch, so one of them is running whichever way it
    // is thrown; the waiting one is skipped, not deleted.
    @Test
    fun whileSignedIn_theBalanceIsRead() = runBlocking {
        assumeTrue(Features.GENERATION_OFFERED)
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
        )
        val viewModel = viewModel(repository, credits = FakeGenerationCredits(balance = 12))

        val state = viewModel.state.firstWithin { it.credits != null }

        assertEquals(12, state.credits)
    }

    @Test
    fun whileGenerationIsHeldBack_theBalanceIsNotAskedFor() = runBlocking {
        assumeFalse(Features.GENERATION_OFFERED)
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
        )
        val credits = FakeGenerationCredits(balance = 12)
        val viewModel = viewModel(repository, credits = credits)

        val state = viewModel.state.firstWithin { it.account is Account.SignedIn }

        assertEquals(0, credits.reads)
        assertEquals(null, state.credits)
    }

    // A guest has a balance too, but this screen is not where it belongs: the
    // panel that shows it only exists once there is an account behind it.
    @Test
    fun whileAGuest_noBalanceIsRead() = runBlocking {
        val credits = FakeGenerationCredits(balance = 12)
        val viewModel = viewModel(credits = credits)

        viewModel.state.first()

        assertEquals(0, credits.reads)
    }

    @Test
    fun signOut_returnsToTheGuestState() = runBlocking {
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
        )
        val viewModel = viewModel(repository)
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        val state = viewModel.state.firstWithin { it.account == Account.Guest }

        assertEquals(Account.Guest, state.account)
    }

    // The account has to be gone before the identity that asked is given up,
    // or a failure leaves somebody unable to sign back in or try again.
    @Test
    fun deletingAnAccount_endsItBeforeSigningOut() = runBlocking {
        val order = mutableListOf<String>()
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
            onSignOut = { order += "signed out" },
        )
        val viewModel = viewModel(
            repository = repository,
            erasure = RecordingErasure(onErase = { order += "erased" }),
        )

        viewModel.deleteAccount()
        viewModel.state.firstWithin { !it.deleting && it.notice == null && order.size == 2 }

        assertEquals(listOf("erased", "signed out"), order)
    }

    // Nothing has happened, so the account must still be usable and the person
    // must be told. Silence here reads as "it worked".
    @Test
    fun anAccountTheServerWouldNotDelete_staysSignedIn() = runBlocking {
        var signedOut = false
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
            onSignOut = { signedOut = true },
        )
        val viewModel = viewModel(repository = repository, erasure = RefusingErasure)

        viewModel.deleteAccount()
        val state = viewModel.state.firstWithin { it.notice == AccountNotice.NotDeleted }

        assertEquals(false, signedOut)
        assertEquals(false, state.deleting)
    }

    private fun viewModel(
        repository: AccountRepository = FakeAccountRepository(),
        credential: FakeGoogleCredential = FakeGoogleCredential(GoogleCredentialResult.Cancelled),
        credits: GenerationCredits = FakeGenerationCredits(),
        publishedLists: OwnLists = FakeOwnLists(),
        erasure: AccountErasure = NoErasure,
    ): AccountViewModel {
        picker = credential
        return AccountViewModel(repository, credits, publishedLists, FakeCommunityForAccount(), FakeAppPreferences(), NoMerge, erasure)
    }
}

private class RecordingErasure(private val onErase: () -> Unit) : AccountErasure {
    override suspend fun erase(): Result<Unit> {
        onErase()
        return Result.success(Unit)
    }
}

private object RefusingErasure : AccountErasure {
    override suspend fun erase(): Result<Unit> = Result.failure(IllegalStateException("offline"))
}

private class FakeCommunityForAccount : FakeCommunityRepository()

private class FakeOwnLists(private val published: Int = 0, private val boards: Int = 0) : OwnLists {
    override suspend fun publishedCount(): Int = published

    override suspend fun boardCount(): Int = boards

    override suspend fun cardImages(limit: Int): List<String> = emptyList()
}

private class FakeGoogleCredential(result: GoogleCredentialResult? = null) {
    var requests = 0
    private val answer = CompletableDeferred<GoogleCredentialResult>()

    init {
        if (result != null) answer.complete(result)
    }

    fun release(result: GoogleCredentialResult) {
        answer.complete(result)
    }

    suspend fun request(): GoogleCredentialResult {
        requests++
        return answer.await()
    }
}

private class FakeGenerationCredits(private val balance: Int? = null) : GenerationCredits {
    override fun lastKnown(): Int? = null

    var reads = 0

    override suspend fun remaining(): Int? {
        reads++
        return balance
    }
}

/** Nothing on either side, so the question never comes up in these cases. */
private object NoMerge : BoardMerge {
    override suspend fun choice() = MergeChoice(accountBoards = 0, localBoards = 0)
    override suspend fun keepEverything(fromThisPhone: String) = Unit
    override suspend fun useAccountBoards() = Unit
}

private class FakeAppPreferences : AppPreferences {
    private var backUp = true

    override fun themeChoice(): ThemeChoice = ThemeChoice.SYSTEM
    override fun setThemeChoice(choice: ThemeChoice) = Unit
    override fun languageTag(): String? = null
    override fun setLanguageTag(tag: String?) = Unit
    override fun lastKnownCredits(): Int? = null
    override fun setLastKnownCredits(credits: Int?) = Unit

    override fun lastKnownPendingReports(): Int? = null

    override fun setLastKnownPendingReports(reports: Int?) = Unit

    override fun lastKnownTrashCount(): Int = 0

    override fun setLastKnownTrashCount(count: Int) = Unit
    override fun hiddenListIds(): Set<String> = emptySet()
    override fun hiddenLists(): List<HiddenEntry> = emptyList()
    override fun hideList(publishedId: String, title: String) = Unit
    override fun unhideList(publishedId: String) = Unit
    override fun hiddenAuthorUids(): Set<String> = emptySet()
    override fun hiddenAuthors(): List<HiddenEntry> = emptyList()
    override fun hideAuthor(authorUid: String, name: String) = Unit
    override fun unhideAuthor(authorUid: String) = Unit
    override fun signInOfferAnswered(): Boolean = false
    override fun markSignInOfferAnswered() = Unit

    private var asPictures = false

    override fun boardsAsPictures(): Boolean = asPictures

    override fun setBoardsAsPictures(asPictures: Boolean) {
        this.asPictures = asPictures
    }

    override fun backUpBoards(): Boolean = backUp

    override fun setBackUpBoards(backUp: Boolean) {
        this.backUp = backUp
    }

    override fun picturesOnWifiOnly(): Boolean = true

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) = Unit

    override fun lastSyncedAtMs(): Long? = null

    override fun setLastSyncedAtMs(atMs: Long?) = Unit

    override fun conflictsSeen(): Set<String> = emptySet()

    override fun markConflictSeen(listUid: String) = Unit
}

/** Ending an account is its own test; here it must simply exist. */
private object NoErasure : AccountErasure {
    override suspend fun erase(): Result<Unit> = Result.success(Unit)
}
