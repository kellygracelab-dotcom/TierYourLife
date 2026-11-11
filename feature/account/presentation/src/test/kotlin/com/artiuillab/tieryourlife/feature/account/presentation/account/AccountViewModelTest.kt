package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.core.settings.HiddenEntry
import com.artiuillab.tieryourlife.core.settings.PersonalData
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.FakeFacePhotos
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.FaceRefused
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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

    // The guest became the account, so the boards are the same person's.
    @Test
    fun signIn_thatTurnsTheGuestIntoTheAccount_movesTheGuestBoardsIn_unasked() = runBlocking {
        val merge = RecordingMerge(guestBoards = 3)
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)

        viewModel.signIn(picker::request)
        merge.answered.await()

        assertEquals(listOf("hold", "moveIn:null"), merge.calls)
        assertNull(viewModel.state.value.merge)
    }

    @Test
    fun signIn_intoAnAccountUsedBefore_asksAboutTheGuestBoards_andMovesNothingYet() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.merge != null }

        assertEquals(GuestBoardsQuestion(boards = 2, into = FakeAccountRepository.SIGNED_IN_EMAIL), state.merge)
        assertEquals(listOf("hold"), merge.calls)
    }

    @Test
    fun signIn_intoAnAccountUsedBefore_withNoGuestBoards_asksNothing() = runBlocking {
        val merge = RecordingMerge(guestBoards = 0)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)

        viewModel.signIn(picker::request)
        merge.answered.await()

        assertEquals(listOf("hold", "keepHere"), merge.calls)
        assertNull(viewModel.state.value.merge)
    }

    @Test
    fun guestBoardsQuestion_answeredMove_movesThemIn_withThisPhonesName() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)
        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { it.merge != null }

        viewModel.moveGuestBoardsIn("(Pixel)")
        merge.answered.await()

        assertEquals(listOf("hold", "moveIn:(Pixel)"), merge.calls)
        assertNull(viewModel.state.value.merge)
    }

    @Test
    fun guestBoardsQuestion_answeredKeep_leavesThemOnThePhone() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)
        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { it.merge != null }

        viewModel.keepGuestBoardsHere()
        merge.answered.await()

        assertEquals(listOf("hold", "keepHere"), merge.calls)
        assertNull(viewModel.state.value.merge)
    }

    @Test
    fun signIn_withBoardsOfAnAccountSignedOutOf_asksAboutThemFirst_andOnTheirOwn() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2, heldBoards = 3)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)

        viewModel.signIn(picker::request)
        val state = viewModel.state.firstWithin { it.merge != null }

        assertEquals(GuestBoardsQuestion(boards = 3, into = FakeAccountRepository.SIGNED_IN_EMAIL, fromAnotherAccount = true), state.merge)
        assertEquals(listOf("hold"), merge.calls)
    }

    @Test
    fun heldBoardsQuestion_answeredCopy_copiesThem_thenAsksAboutTheGuestBoards() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2, heldBoards = 3)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)
        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { it.merge?.fromAnotherAccount == true }

        viewModel.moveGuestBoardsIn("(Pixel)")
        val next = viewModel.state.firstWithin { it.merge?.fromAnotherAccount == false }

        assertEquals(GuestBoardsQuestion(boards = 2, into = FakeAccountRepository.SIGNED_IN_EMAIL), next.merge)
        assertEquals(listOf("hold", "copyHeldIn:(Pixel)"), merge.calls)
    }

    @Test
    fun heldBoardsQuestion_aCopyThatFails_saysSo_andSyncingIsNotLeftWaiting() = runBlocking {
        val merge = RecordingMerge(guestBoards = 0, heldBoards = 3, copyFails = true)
        val repository = FakeAccountRepository(outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = false))
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)
        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { it.merge?.fromAnotherAccount == true }

        viewModel.moveGuestBoardsIn("(Pixel)")
        merge.answered.await()

        assertEquals(AccountNotice.BoardsNotCopied, viewModel.state.value.notice)
        assertEquals(listOf("hold", "copyHeldIn:(Pixel)", "keepHere"), merge.calls)
    }

    @Test
    fun heldBoardsQuestion_answeredKeep_onANewAccount_movesOnlyTheGuestBoards() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2, heldBoards = 3)
        val viewModel = viewModel(credential = FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)
        viewModel.signIn(picker::request)
        viewModel.state.firstWithin { it.merge?.fromAnotherAccount == true }

        viewModel.keepGuestBoardsHere()
        merge.answered.await()

        assertEquals(listOf("hold", "moveIn:null"), merge.calls)
        assertNull(viewModel.state.value.merge)
    }

    // A failed sign-in leaves the guest where they were, and syncing is not left waiting.
    @Test
    fun signIn_thatFails_releasesTheHold_movingNothing() = runBlocking {
        val merge = RecordingMerge(guestBoards = 2)
        val repository = FakeAccountRepository(outcome = SignInOutcome.Failed)
        val viewModel = viewModel(repository, FakeGoogleCredential(GoogleCredentialResult.Token("t")), merge = merge)

        viewModel.signIn(picker::request)
        merge.answered.await()

        assertEquals(listOf("hold", "keepHere"), merge.calls)
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
    fun signIn_whenThePickerCoroutineIsCancelled_unlocksTheButton_withoutRetrying() = runBlocking {
        val repository = FakeAccountRepository()
        val viewModel = viewModel(repository)
        val entered = CompletableDeferred<Unit>()
        var requests = 0

        viewModel.signIn {
            requests++
            entered.complete(Unit)
            throw CancellationException("test picker cancelled")
        }
        entered.await()
        withContext(Dispatchers.Main) {}

        assertFalse(viewModel.state.value.signingIn)
        assertNull(viewModel.state.value.notice)
        assertEquals(1, requests)
        assertEquals(emptyList<String>(), repository.tokens)

        viewModel.signIn {
            requests++
            GoogleCredentialResult.Cancelled
        }
        withContext(Dispatchers.Main) {}
        assertEquals(2, requests)
        assertFalse(viewModel.state.value.signingIn)
    }

    @Test
    fun signIn_cancelledDuringFirebaseAuth_releasesTheMergeHold_andUnlocksTheButton() = runBlocking {
        val repository = object : FakeAccountRepository() {
            override suspend fun signInWithGoogle(idToken: String): SignInOutcome =
                throw CancellationException("test auth cancelled")
        }
        val merge = RecordingMerge(guestBoards = 2)
        val viewModel = viewModel(repository, merge = merge)

        viewModel.signIn { GoogleCredentialResult.Token("test-token") }
        merge.answered.await()
        withContext(Dispatchers.Main) {}

        assertEquals(listOf("hold", "keepHere"), merge.calls)
        assertFalse(viewModel.state.value.signingIn)
        assertNull(viewModel.state.value.notice)
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
            outcome = SignInOutcome.SignedInToExistingAccount(guestCreditsLost = true),
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

    // Signed out, an account whose erase never answered could no longer be asked about.
    @Test
    fun signOut_settlesAnUnansweredEraseFirst() = runBlocking {
        val repository = FakeAccountRepository(initial = Account.SignedIn(email = "someone@example.com", photoUrl = null))
        val merge = RecordingMerge(guestBoards = 0)
        val viewModel = viewModel(repository, merge = merge)
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        viewModel.state.firstWithin { it.account == Account.Guest }

        assertEquals(listOf("settledBeforeSigningOut"), merge.calls)
    }

    // A32: it can wait out the call a sync is in, up to minutes for a picture.
    @Test
    fun signOut_saysItIsSigningOutUntilItHas() = runBlocking {
        val repository = FakeAccountRepository(initial = Account.SignedIn(email = "someone@example.com", photoUrl = null))
        val syncEnds = CompletableDeferred<Unit>()
        val viewModel = viewModel(repository, merge = RecordingMerge(guestBoards = 0, signOutWaits = syncEnds))
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        viewModel.state.firstWithin { it.signingOut }
        syncEnds.complete(Unit)
        val state = viewModel.state.firstWithin { !it.signingOut }

        assertEquals(Account.Guest, state.account)
    }

    @Test
    fun signOutCompletesWithTheCurrentIdentityBeforeTheListenerCatchesUp() = runBlocking {
        val signedIn = Account.SignedIn(email = "someone@example.com", photoUrl = null)
        val delayedListener = kotlinx.coroutines.flow.MutableStateFlow<Account>(signedIn)
        val repository = object : FakeAccountRepository(initial = signedIn) {
            override val account: kotlinx.coroutines.flow.Flow<Account> = delayedListener
        }
        val viewModel = viewModel(repository, merge = RecordingMerge(guestBoards = 0))
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        val done = viewModel.state.firstWithin { !it.signingOut && repository.current() == Account.Guest }

        assertEquals(Account.Guest, done.account)
        assertEquals(signedIn, delayedListener.value)
    }

    @Test
    fun signOut_pressedAgainWhileItWaits_signsOutOnce() = runBlocking {
        val repository = FakeAccountRepository(initial = Account.SignedIn(email = "someone@example.com", photoUrl = null))
        val syncEnds = CompletableDeferred<Unit>()
        val merge = RecordingMerge(guestBoards = 0, signOutWaits = syncEnds)
        val viewModel = viewModel(repository, merge = merge)
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        viewModel.state.firstWithin { it.signingOut }
        viewModel.signOut()
        syncEnds.complete(Unit)
        viewModel.state.firstWithin { !it.signingOut }

        assertEquals(listOf("settledBeforeSigningOut"), merge.calls)
    }

    // Leaving the screen while it waited took the sign-out with it.
    @Test
    fun signOut_goesThroughAfterTheScreenIsLeft() = runBlocking {
        val repository = FakeAccountRepository(initial = Account.SignedIn(email = "someone@example.com", photoUrl = null))
        val syncEnds = CompletableDeferred<Unit>()
        val store = ViewModelStore()
        val made = viewModel(repository, merge = RecordingMerge(guestBoards = 0, signOutWaits = syncEnds))
        val viewModel = ViewModelProvider.create(store, viewModelFactory { initializer { made } })[AccountViewModel::class]
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        viewModel.state.firstWithin { it.signingOut }
        withContext(Dispatchers.Main) { store.clear() }
        syncEnds.complete(Unit)

        assertEquals(Account.Guest, withTimeout(5_000) { repository.account.first { it == Account.Guest } })
    }

    @Test
    fun signOut_leavesNothingOfThePersonForTheNextOne() = runBlocking {
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null),
        )
        val preferences = FakeAppPreferences()
        preferences.hideList("list-1", "Seen by the last person")
        preferences.hideAuthor("author-1", "The last person's block")
        val viewModel = viewModel(repository, preferences = preferences)
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        viewModel.signOut()
        withTimeout(5_000) { while (preferences.hiddenLists().isNotEmpty()) delay(10) }

        assertEquals(emptyList<HiddenEntry>(), preferences.hiddenAuthors())
    }

    // S22: Play's saved games outlived the person who played them.
    @Test
    fun signingOutAndDeleting_bothForgetWhatEachModuleKept() = runBlocking {
        var forgotten = 0
        val kept = setOf(PersonalData { forgotten++ })
        val signedIn = Account.SignedIn(email = "someone@example.com", photoUrl = null, uid = "someone")

        val leaving = viewModel(FakeAccountRepository(initial = signedIn), kept = kept)
        leaving.state.firstWithin { it.account is Account.SignedIn }
        leaving.signOut()
        withTimeout(5_000) { while (forgotten < 1) delay(10) }

        val deleting = viewModel(FakeAccountRepository(initial = signedIn), kept = kept)
        deleting.state.firstWithin { it.account is Account.SignedIn }
        deleting.deleteAccount()
        withTimeout(5_000) { while (forgotten < 2) delay(10) }

        assertEquals(2, forgotten)
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

    // The dialog says the boards on this phone stay where they are; left
    // under the deleted uid, nobody could ever reach them again.
    @Test
    fun deletingAnAccount_keepsItsBoardsOnThisPhone() = runBlocking {
        var signedOut = false
        val repository = FakeAccountRepository(
            initial = Account.SignedIn(email = "someone@example.com", photoUrl = null, uid = "someone"),
            onSignOut = { signedOut = true },
        )
        val merge = RecordingMerge(guestBoards = 0)
        val viewModel = viewModel(repository = repository, merge = merge)

        viewModel.deleteAccount()
        viewModel.state.firstWithin { !it.deleting && signedOut }

        assertEquals(listOf("erase:someone", "keptBoardsOf:someone"), merge.calls)
    }

    @Test
    fun anAccountTheServerWouldNotDelete_keepsItsBoardsWhereTheyWere() = runBlocking {
        val repository = FakeAccountRepository(initial = Account.SignedIn(email = "someone@example.com", photoUrl = null, uid = "someone"))
        val merge = RecordingMerge(guestBoards = 0)
        val viewModel = viewModel(repository = repository, erasure = RefusingErasure, merge = merge)

        viewModel.deleteAccount()
        viewModel.state.firstWithin { it.notice == AccountNotice.NotDeleted }

        assertEquals(listOf("erase:someone"), merge.calls)
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

    // The photo is cut on the phone, goes up into the person's own folder, the
    // server looks at it and answers with a public address, and that is the face.
    @Test
    fun aPhotoFromTheGallery_isCutSentUpAndBecomesTheFace() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos()
        val viewModel = viewModel(repository, facePhotos = photos)

        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        val state = viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(listOf<Pair<String, FaceCrop?>>("content://photos/1" to null), photos.prepared)
        assertEquals(listOf(FakeFacePhotos.PREPARED_URI), photos.uploaded)
        assertEquals(listOf<String?>(FakeCommunityRepository.FACE_URL), repository.photoUrls)
        assertNull(state.notice)
    }

    // What was slow was waiting for the upload and the server's look before
    // anything changed on screen. The cut picture shows as soon as it is cut.
    @Test
    fun aChosenPhoto_showsAtOnce_whileItIsStillGoingUp() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos(holdUploads = true, holdPrepares = true)
        val viewModel = viewModel(repository, facePhotos = photos)

        viewModel.face.position("content://photos/1", fromGallery = true)
        val crop = FaceCrop(0.1f, 0.2f, 0.5f, 0.5f)
        viewModel.face.confirmPosition(crop)
        // Done shows the chosen square of the photo itself, before anything is cut.
        val chosen = viewModel.state.firstWithin { it.pendingFace != null }
        assertEquals("content://photos/1", chosen.shownFace)
        assertEquals(crop, chosen.shownFaceCrop)
        photos.releasePrepares()
        val onItsWay = viewModel.state.firstWithin { it.shownFace == FakeFacePhotos.PREPARED_URI }

        assertEquals(null, onItsWay.shownFaceCrop)
        assertTrue(onItsWay.makingFace)
        assertEquals(emptyList<String?>(), repository.photoUrls)

        photos.releaseUploads()
        val worn = viewModel.state.firstWithin { !it.makingFace }
        // Still the picture cut here: the same face, and nothing to download again.
        assertEquals(FakeFacePhotos.PREPARED_URI, worn.shownFace)
        assertEquals(listOf<String?>(FakeCommunityRepository.FACE_URL), repository.photoUrls)
    }

    @Test
    fun aPhotoThatWillNotGoUp_putsTheOldFaceBackAndSaysSo() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val photos = FakeFacePhotos(Result.failure(IOException("unreadable")))
        val viewModel = viewModel(repository, facePhotos = photos)
        viewModel.state.firstWithin { it.shownFace == OLD_FACE }

        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.FaceNotSaved, state.notice)
        assertEquals(OLD_FACE, state.shownFace)
        assertEquals(emptyList<String?>(), repository.photoUrls)
        assertEquals(false, state.makingFace)
    }

    // Refused by the server after the upload: the person hears about it the
    // same way, the old face comes back, and nothing is written to the account.
    @Test
    fun aPhotoTheServerRefuses_putsTheOldFaceBackAndSaysSo() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val viewModel = viewModel(repository, community = RefusingFaces())
        viewModel.state.firstWithin { it.shownFace == OLD_FACE }

        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.FaceNotMade, state.notice)
        assertEquals(OLD_FACE, state.shownFace)
        assertEquals(emptyList<String?>(), repository.photoUrls)
    }

    // A18: a server that did not answer read as a picture it turned down.
    @Test
    fun aPhotoTheServerNeverAnswersFor_saysSo_ratherThanThatThePictureWasRefused() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val viewModel = viewModel(repository, community = UnansweredFaces())
        viewModel.state.firstWithin { it.shownFace == OLD_FACE }

        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        val state = viewModel.state.firstWithin { it.notice != null }

        assertEquals(AccountNotice.FaceNotSaved, state.notice)
        assertEquals(OLD_FACE, state.shownFace)
    }

    // A poster left where it was needs no copy: its address is the face at once.
    @Test
    fun aPosterLeftInTheMiddle_isWornByItsAddress_withoutGoingUp() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos()
        val viewModel = viewModel(repository, facePhotos = photos)

        viewModel.face.position(POSTER, fromGallery = false)
        viewModel.face.confirmPosition(null)
        assertEquals(POSTER, viewModel.state.value.shownFace)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(listOf<String?>(POSTER), repository.photoUrls)
        assertTrue(photos.prepared.isEmpty())
    }

    // Moved inside the circle, the poster is a new picture: cut here and sent up like a photo.
    @Test
    fun aPosterMovedInTheCircle_isCutAndSentUp() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos()
        val viewModel = viewModel(repository, facePhotos = photos)
        val crop = FaceCrop(0f, 0.1f, 1f, 0.66f)

        viewModel.face.position(POSTER, fromGallery = false)
        viewModel.face.confirmPosition(crop)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(listOf<Pair<String, FaceCrop?>>(POSTER to crop), photos.prepared)
        assertEquals(listOf<String?>(FakeCommunityRepository.FACE_URL), repository.photoUrls)
    }

    // The letter needs nothing from anywhere: it is on screen before the account has saved it.
    @Test
    fun theLetter_replacesAPhotoAtOnce() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val viewModel = viewModel(repository)
        viewModel.state.firstWithin { it.shownFace == OLD_FACE }

        viewModel.face.wearLetter()
        assertNull(viewModel.state.value.shownFace)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(listOf<String?>(null), repository.photoUrls)
        assertNull(viewModel.state.value.shownFace)
    }

    @Test
    fun cancellingThePlacing_changesNothing() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos()
        val viewModel = viewModel(repository, facePhotos = photos)

        viewModel.face.position(POSTER, fromGallery = false)
        assertEquals(FaceToPosition(POSTER, fromGallery = false), viewModel.state.value.positioning)
        viewModel.face.cancelPositioning()

        assertNull(viewModel.state.value.positioning)
        assertTrue(photos.prepared.isEmpty())
        assertEquals(emptyList<String?>(), repository.photoUrls)
    }

    private fun viewModel(
        repository: AccountRepository = FakeAccountRepository(),
        credential: FakeGoogleCredential = FakeGoogleCredential(GoogleCredentialResult.Cancelled),
        credits: GenerationCredits = FakeGenerationCredits(),
        publishedLists: OwnLists = FakeOwnLists(),
        erasure: AccountErasure = NoErasure,
        merge: BoardMerge = RecordingMerge(guestBoards = 0),
        community: FakeCommunityRepository = FakeCommunityForAccount(),
        facePhotos: FacePhotos = FakeFacePhotos(),
        preferences: FakeAppPreferences = FakeAppPreferences(),
        kept: Set<PersonalData> = emptySet(),
    ): AccountViewModel {
        picker = credential
        return AccountViewModel(repository, credits, publishedLists, community, merge, erasure, facePhotos, ThePerson(preferences, kept))
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

private const val OLD_FACE = "https://example.test/old-face.jpg"
private const val POSTER = "https://image.tmdb.org/t/p/w500/poster.jpg"

private fun Account.withFace(photoUrl: String): Account = (this as Account.SignedIn).copy(photoUrl = photoUrl)

private class FakeCommunityForAccount : FakeCommunityRepository()

private class RefusingFaces : FakeCommunityRepository() {
    override suspend fun makeFace(pictureId: String): Result<String> = Result.failure(FaceRefused())
}

private class UnansweredFaces : FakeCommunityRepository() {
    override suspend fun makeFace(pictureId: String): Result<String> = Result.failure(IOException("no answer"))
}

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

private class RecordingMerge(
    private val guestBoards: Int,
    private val heldBoards: Int = 0,
    private val copyFails: Boolean = false,
    private val signOutWaits: CompletableDeferred<Unit>? = null,
) : BoardMerge {
    val calls = mutableListOf<String>()
    val answered = CompletableDeferred<Unit>()

    override fun hold() {
        calls += "hold"
    }

    override suspend fun guestBoards(): Int = guestBoards

    override suspend fun moveIn(fromThisPhone: String?) {
        calls += "moveIn:$fromThisPhone"
        answered.complete(Unit)
    }

    override suspend fun keepHere() {
        calls += "keepHere"
        answered.complete(Unit)
    }

    override suspend fun heldBoards(): Int = heldBoards

    override suspend fun copyHeldIn(fromThisPhone: String?) {
        calls += "copyHeldIn:$fromThisPhone"
        check(!copyFails) { "disk full" }
    }

    override suspend fun eraseKeepingBoards(account: String, erase: suspend () -> Result<Unit>, signOut: suspend () -> Unit): Result<Unit> {
        calls += "erase:$account"
        return erase().onSuccess {
            calls += "keptBoardsOf:$account"
            signOut()
        }
    }

    override suspend fun signOutSettlingErase(signOut: suspend () -> Unit) {
        calls += "settledBeforeSigningOut"
        signOutWaits?.await()
        signOut()
    }

    override suspend fun settleUnansweredErase() = Unit
}

/** Ending an account is its own test; here it must simply exist. */
private object NoErasure : AccountErasure {
    override suspend fun erase(): Result<Unit> = Result.success(Unit)
}
