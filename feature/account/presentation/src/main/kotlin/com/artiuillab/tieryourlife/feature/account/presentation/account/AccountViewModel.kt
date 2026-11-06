package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountErasure
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.artiuillab.tieryourlife.feature.account.presentation.account.face.AccountFace
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private const val FACE_CHOICE_LIMIT = 24

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val credits: GenerationCredits,
    private val ownLists: OwnLists,
    private val community: CommunityRepository,
    private val merge: BoardMerge,
    private val erasure: AccountErasure,
    private val facePhotos: FacePhotos,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountUiState(credits = credits.lastKnown()),
    )
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    /** Choosing, placing and wearing a face. */
    internal val face = AccountFace(_state, viewModelScope, repository, community, facePhotos, ::refreshPublishedAuthor)

    init {
        viewModelScope.launch {
            repository.account.collect { account ->
                // A face chosen here belongs to the account it was chosen on.
                _state.update { it.copy(account = account, pendingFace = it.pendingFace.takeIf { account is Account.SignedIn }) }
                refreshCredits()
                refreshOwnLists()
            }
        }
    }

    /** [pickAccount] opens Google's sheet; the screen owns it because the sheet needs the activity. */
    fun signIn(pickAccount: suspend () -> GoogleCredentialResult) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true, notice = null) }
        viewModelScope.launch {
            when (val result = pickAccount()) {
                is GoogleCredentialResult.Token -> completeSignIn(result.idToken)
                GoogleCredentialResult.Cancelled -> _state.update { it.copy(signingIn = false) }
                GoogleCredentialResult.NoGoogleAccount -> finish(AccountNotice.NoGoogleAccount)
                GoogleCredentialResult.Unavailable -> finish(AccountNotice.SignInUnavailable)
            }
        }
    }

    fun setDisplayName(name: String) {
        if (_state.value.savingName) return
        _state.update { it.copy(savingName = true) }
        viewModelScope.launch {
            val saved = repository.setDisplayName(name)
            if (saved) refreshPublishedAuthor() else Timber.w("Renaming the account did not stick")
            _state.update {
                it.copy(savingName = false, notice = if (saved) null else AccountNotice.NameNotSaved)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _state.update { it.copy(notice = null) }
        }
    }

    /**
     * Account first, then sign out: signing out first would take away the
     * identity the request is made with, and a failure would leave somebody
     * unable to sign in or to try again. The boards here stay, as the dialog
     * promises: left under the deleted uid, nobody could ever see them again.
     */
    fun deleteAccount() {
        if (_state.value.deleting) return
        val account = (repository.current() as? Account.SignedIn)?.recordKey ?: return
        _state.update { it.copy(deleting = true, notice = null) }
        viewModelScope.launch {
            merge.eraseKeepingBoards(account) { erasure.erase() }.fold(
                onSuccess = {
                    repository.signOut()
                    _state.update { it.copy(deleting = false) }
                },
                onFailure = { error ->
                    Timber.w(error, "Deleting the account failed")
                    _state.update { it.copy(deleting = false, notice = AccountNotice.NotDeleted) }
                },
            )
        }
    }

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }

    private suspend fun completeSignIn(idToken: String) {
        merge.hold()
        try {
            val outcome = repository.signInWithGoogle(idToken)
            val notice = when (outcome) {
                SignInOutcome.Success -> null
                is SignInOutcome.SignedInToExistingAccount ->
                    if (outcome.guestCreditsLost) AccountNotice.SignedInToExistingAccount else null
                SignInOutcome.Failed -> AccountNotice.SignInFailed
                SignInOutcome.Cancelled -> null
                SignInOutcome.NoGoogleAccount -> AccountNotice.NoGoogleAccount
            }
            finish(notice)
            refreshCredits()
            settleGuestBoards(outcome)
        } catch (e: CancellationException) {
            // Left half-way, so nobody will be asked: syncing must not stay stopped for an answer.
            withContext(NonCancellable) { merge.keepHere() }
            throw e
        }
    }

    /**
     * A guest turned into the account is the same person, so their boards go
     * with them unasked. An account used before may be somebody else's, or
     * the same person's other library: asked, and only if this phone has
     * boards made without an account. Another account's boards never come
     * into it; they stay hidden on this phone.
     */
    private suspend fun settleGuestBoards(outcome: SignInOutcome) {
        val signedIn = repository.current() as? Account.SignedIn
        when {
            signedIn == null -> merge.keepHere()
            outcome is SignInOutcome.Success -> merge.moveIn(fromThisPhone = null)
            outcome is SignInOutcome.SignedInToExistingAccount -> {
                val boards = runSuspendCatching { merge.guestBoards() }
                    .onFailure { Timber.w(it, "Could not count the boards made without an account") }
                    .getOrDefault(0)
                if (boards > 0) {
                    _state.update { it.copy(merge = GuestBoardsQuestion(boards = boards, into = signedIn.email)) }
                } else {
                    merge.keepHere()
                }
            }
            else -> merge.keepHere()
        }
    }

    fun moveGuestBoardsIn(fromThisPhone: String) {
        _state.update { it.copy(merge = null) }
        viewModelScope.launch { merge.moveIn(fromThisPhone) }
    }

    /** Closing the question is this answer too: nothing moves, nothing is lost. */
    fun keepGuestBoardsHere() {
        _state.update { it.copy(merge = null) }
        viewModelScope.launch { merge.keepHere() }
    }

    private fun finish(notice: AccountNotice?) {
        _state.update { it.copy(signingIn = false, notice = notice) }
    }

    /** Best effort: a stale face on an old list is not worth refusing the change over. */
    private suspend fun refreshPublishedAuthor() {
        community.refreshAuthor().onFailure {
            Timber.w(it, "Could not bring the author up to date on published lists")
        }
    }

    private suspend fun refreshOwnLists() {
        if (_state.value.account !is Account.SignedIn) return
        // Asked of the server: a list published from a device that is gone is still out there.
        val count = community.myPublished()
            .onFailure { Timber.w(it, "Counting published lists failed") }
            .getOrNull()
            ?.size
            // Unknown is not zero: the last count stands rather than "0 public lists".
            ?: _state.value.publicListCount
        val faces = runSuspendCatching { ownLists.cardImages(FACE_CHOICE_LIMIT) }
            .onFailure { Timber.w(it, "Reading card pictures failed") }
            .getOrDefault(emptyList())
        _state.update {
            it.copy(
                publicListCount = count,
                faceChoices = faces,
            )
        }
    }

    private suspend fun refreshCredits() {
        if (!Features.GENERATION_OFFERED) return
        if (_state.value.account !is Account.SignedIn) return
        val remaining = runSuspendCatching { credits.remaining() }.getOrNull()
        _state.update { it.copy(credits = remaining) }
    }
}
