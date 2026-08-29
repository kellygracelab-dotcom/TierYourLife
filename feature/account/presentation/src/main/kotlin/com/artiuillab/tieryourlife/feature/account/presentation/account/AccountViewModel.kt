package com.artiuillab.tieryourlife.feature.account.presentation.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.SignInOutcome
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredential
import com.artiuillab.tieryourlife.feature.account.presentation.signin.GoogleCredentialResult
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
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
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState(credits = credits.lastKnown()))
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.account.collect { account ->
                _state.update { it.copy(account = account) }
                refreshCredits()
                refreshOwnLists()
            }
        }
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
                googlePhotoUrl = repository.googlePhotoUrl(),
            )
        }
    }

    fun setPhoto(photoUrl: String?) {
        viewModelScope.launch {
            val saved = repository.setPhotoUrl(photoUrl)
            if (saved) refreshPublishedAuthor() else _state.update { it.copy(notice = AccountNotice.NameNotSaved) }
        }
    }

    private suspend fun refreshCredits() {
        if (_state.value.account !is Account.SignedIn) return
        val remaining = runCatching { credits.remaining() }.getOrNull()
        _state.update { it.copy(credits = remaining) }
    }
}
