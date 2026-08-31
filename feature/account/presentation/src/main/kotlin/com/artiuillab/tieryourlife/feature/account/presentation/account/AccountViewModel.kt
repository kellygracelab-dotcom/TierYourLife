package com.artiuillab.tieryourlife.feature.account.presentation.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredential
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardMerge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val FACE_CHOICE_LIMIT = 24

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val googleCredential: GoogleCredential,
    private val credits: GenerationCredits,
    private val ownLists: OwnLists,
    private val community: CommunityRepository,
    private val preferences: AppPreferences,
    private val merge: BoardMerge,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountUiState(credits = credits.lastKnown(), backUpBoards = preferences.backUpBoards()),
    )
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.account.collect { account ->
                _state.update { it.copy(account = account) }
                refreshCredits()
                refreshOwnLists()
                _state.update { it.copy(boardCount = ownLists.boardCount()) }
            }
        }
    }

    /**
     * Answered before signing in, so nothing has gone up yet and nothing has
     * to be undone. Turning it off later is a different question with a
     * different answer, and it is asked in Settings where the copy exists.
     */
    fun setBackUpBoards(backUp: Boolean) {
        preferences.setBackUpBoards(backUp)
        _state.update { it.copy(backUpBoards = backUp) }
    }

    fun signIn(context: Context) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true, notice = null) }
        viewModelScope.launch {
            when (val result = googleCredential.request(context)) {
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

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }

    private suspend fun completeSignIn(idToken: String) {
        val notice = when (val outcome = repository.signInWithGoogle(idToken)) {
            SignInOutcome.Success -> null
            is SignInOutcome.SignedInToExistingAccount ->
                if (outcome.creditsCarriedOver) null else AccountNotice.SignedInToExistingAccount
            SignInOutcome.Failed -> AccountNotice.SignInFailed
            SignInOutcome.Cancelled -> null
            SignInOutcome.NoGoogleAccount -> AccountNotice.NoGoogleAccount
        }
        finish(notice)
        refreshCredits()
        askAboutMergeIfNeeded()
    }

    /**
     * Asked only when both sides hold something. An account with nothing on it
     * takes this phone's boards silently, because there is nothing to weigh
     * them against, and a question with one possible answer is a delay.
     */
    private suspend fun askAboutMergeIfNeeded() {
        if (!preferences.backUpBoards()) return
        val choice = merge.choice()
        if (choice.needed) {
            _state.update { it.copy(merge = choice, mergeKeep = MergeKeep.Everything) }
        }
    }

    fun setMergeKeep(keep: MergeKeep) {
        _state.update { it.copy(mergeKeep = keep) }
    }

    fun applyMerge(fromThisPhone: String) {
        val keep = _state.value.mergeKeep
        _state.update { it.copy(merge = null) }
        viewModelScope.launch {
            when (keep) {
                MergeKeep.Everything -> merge.keepEverything(fromThisPhone)
                MergeKeep.AccountOnly -> merge.useAccountBoards()
            }
        }
    }

    /**
     * Closing the question leaves them signed out with nothing written, which
     * is the only honest reading of a person who did not answer it.
     */
    fun abandonMerge() {
        _state.update { it.copy(merge = null) }
        signOut()
    }

    private fun finish(notice: AccountNotice?) {
        _state.update { it.copy(signingIn = false, notice = notice) }
    }

    /**
     * Best effort: the profile is already saved, and a stale face on an old
     * list is not worth refusing the change over.
     */
    private suspend fun refreshPublishedAuthor() {
        community.refreshAuthor().onFailure {
            Timber.w(it, "Could not bring the author up to date on published lists")
        }
    }

    private suspend fun refreshOwnLists() {
        if (_state.value.account !is Account.SignedIn) return
        // Asked of the server, not of this phone: a list published from a
        // device that is gone is still out there, and counting locally hid it.
        val count = community.myPublished()
            .onFailure { Timber.w(it, "Counting published lists failed") }
            .getOrNull()
            ?.size
            ?: 0
        val faces = runCatching { ownLists.cardImages(FACE_CHOICE_LIMIT) }
            .onFailure { Timber.w(it, "Reading card pictures failed") }
            .getOrDefault(emptyList())
        _state.update {
            it.copy(
                publicListCount = count,
                faceChoices = faces,
            )
        }
    }

    /**
     * A face is either an address already or a picture of this person's own.
     *
     * Catalogue art is hosted by somebody else and can be used as it stands.
     * A picture this app generated is a file in a folder only its owner may
     * read, so the server copies it somewhere the community can see -- after
     * looking at it, on the same terms as a published board's pictures.
     */
    fun setPhoto(photoUrl: String?) {
        viewModelScope.launch {
            _state.update { it.copy(makingFace = photoUrl != null && !photoUrl.startsWith("https://")) }
            val address = when {
                photoUrl == null || photoUrl.startsWith("https://") -> photoUrl
                else -> community.makeFace(photoUrl.substringAfterLast('/')).getOrNull()
            }
            if (photoUrl != null && address == null) {
                _state.update { it.copy(makingFace = false, notice = AccountNotice.FaceNotMade) }
                return@launch
            }
            val saved = repository.setPhotoUrl(address)
            _state.update { it.copy(makingFace = false) }
            if (saved) refreshPublishedAuthor() else _state.update { it.copy(notice = AccountNotice.NameNotSaved) }
        }
    }

    private suspend fun refreshCredits() {
        if (_state.value.account !is Account.SignedIn) return
        val remaining = runCatching { credits.remaining() }.getOrNull()
        _state.update { it.copy(credits = remaining) }
    }
}
