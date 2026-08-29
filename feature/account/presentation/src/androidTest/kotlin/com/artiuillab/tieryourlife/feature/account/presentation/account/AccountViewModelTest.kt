package com.artiuillab.tieryourlife.feature.account.presentation.account

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredential
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountViewModelTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun signIn_whenGoogleReturnsAToken_handsItToTheRepository() = runBlocking {
        val repository = FakeAccountRepository()
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("token-1")))

        viewModel.signIn(context)
        viewModel.state.first { !it.signingIn }

        assertEquals(listOf("token-1"), repository.tokens)
    }

    // Closing the picker is a decision, not a fault: nothing should be said
    // about it, and the button has to come back to life.
    @Test
    fun signIn_whenThePickerIsClosed_saysNothingAndStopsWaiting() = runBlocking {
        val repository = FakeAccountRepository()
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Cancelled))

        viewModel.signIn(context)
        val state = viewModel.state.first { !it.signingIn }

        assertNull(state.notice)
        assertEquals(emptyList<String>(), repository.tokens)
    }

    @Test
    fun signIn_withoutAGoogleAccountOnThePhone_saysSo() = runBlocking {
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.NoGoogleAccount))

        viewModel.signIn(context)
        val state = viewModel.state.first { it.notice != null }

        assertEquals(AccountNotice.NoGoogleAccount, state.notice)
    }

    // Before Google sign-in is switched on in Firebase there is no web client
    // id to ask with. That is a configuration state, not a broken sign-in.
    @Test
    fun signIn_whenSigningInIsNotConfigured_reportsItSeparately() = runBlocking {
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.Unavailable))

        viewModel.signIn(context)
        val state = viewModel.state.first { it.notice != null }

        assertEquals(AccountNotice.SignInUnavailable, state.notice)
    }

    @Test
    fun signIn_whenTheRepositoryFails_reportsAFailure() = runBlocking {
        val repository = FakeAccountRepository(outcome = SignInOutcome.Failed)
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("token-1")))

        viewModel.signIn(context)
        val state = viewModel.state.first { it.notice != null }

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

        viewModel.signIn(context)
        val state = viewModel.state.first { it.notice != null }

        assertEquals(AccountNotice.SignedInToExistingAccount, state.notice)
    }

    @Test
    fun signIn_twice_doesNotOpenThePickerAgainWhileTheFirstIsRunning() = runBlocking {
        val credential = FakeGoogleCredential(GoogleCredentialResult.Cancelled)
        val viewModel = viewModel(credential = credential)

        viewModel.signIn(context)
        viewModel.signIn(context)
        viewModel.state.first { !it.signingIn }

        assertEquals(1, credential.requests)
    }

    @Test
    fun whileSignedIn_theBalanceIsRead() = runBlocking {
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
        )
        val viewModel = viewModel(repository, credits = FakeGenerationCredits(balance = 12))

        val state = viewModel.state.first { it.credits != null }

        assertEquals(12, state.credits)
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
        viewModel.state.first { it.account is Account.SignedIn }

        viewModel.signOut()
        val state = viewModel.state.first { it.account == Account.Guest }

        assertEquals(Account.Guest, state.account)
    }

    private fun viewModel(
        repository: AccountRepository = FakeAccountRepository(),
        credential: GoogleCredential = FakeGoogleCredential(GoogleCredentialResult.Cancelled),
        credits: GenerationCredits = FakeGenerationCredits(),
        publishedLists: OwnLists = FakeOwnLists(),
    ) = AccountViewModel(repository, credential, credits, publishedLists, FakeCommunityForAccount())
}

private class FakeCommunityForAccount : CommunityRepository {
    override suspend fun feed(
        category: ListCategory?,
        query: String?,
        author: String?,
    ): Result<List<PublishedListSummary>> = Result.success(emptyList())

    override suspend fun open(id: String): Result<PublishedList> = Result.failure(IllegalStateException())

    override suspend fun publish(list: TierList): Result<String> = Result.failure(IllegalStateException())

    override suspend fun unpublish(publishedId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshAuthor(): Result<Unit> = Result.success(Unit)

    override suspend fun report(
        publishedId: String,
        reason: ReportReason,
        note: String?,
    ): Result<Unit> = Result.success(Unit)
}

private class FakeOwnLists(private val published: Int = 0) : OwnLists {
    override suspend fun publishedCount(): Int = published

    override suspend fun cardImages(limit: Int): List<String> = emptyList()
}

private class FakeAccountRepository(
    initial: Account = Account.Guest,
    private val outcome: SignInOutcome = SignInOutcome.Success,
) : AccountRepository {

    val tokens = mutableListOf<String>()
    private val state = MutableStateFlow(initial)

    override val account: Flow<Account> = state

    override suspend fun signInWithGoogle(idToken: String): SignInOutcome {
        tokens += idToken
        if (outcome is SignInOutcome.Success) {
            state.value = Account.SignedIn(email = "someone@example.com", photoUrl = null)
        }
        return outcome
    }

    override suspend fun setDisplayName(name: String): Boolean = true

    override suspend fun setPhotoUrl(photoUrl: String?): Boolean = true

    override fun googlePhotoUrl(): String? = null

    override suspend fun signOut() {
        state.value = Account.Guest
    }
}

private class FakeGoogleCredential(private val result: GoogleCredentialResult) : GoogleCredential {
    var requests = 0

    override suspend fun request(context: Context): GoogleCredentialResult {
        requests++
        return result
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
