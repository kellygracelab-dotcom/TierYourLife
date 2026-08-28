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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val googleCredential: GoogleCredential,
    private val credits: GenerationCredits,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.account.collect { account ->
                _state.update { it.copy(account = account) }
                refreshCredits()
            }
        }
    }

    /**
     * The context is the activity showing the account picker, and is passed in
     * rather than held: this view model outlives the screen that opened it.
     */
    fun signIn(context: Context) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true, notice = null) }
        viewModelScope.launch {
            when (val result = googleCredential.request(context)) {
                is GoogleCredentialResult.Token -> completeSignIn(result.idToken)
                // Closing the picker is a decision, not a problem to report.
                GoogleCredentialResult.Cancelled -> _state.update { it.copy(signingIn = false) }
                GoogleCredentialResult.NoGoogleAccount -> finish(AccountNotice.NoGoogleAccount)
                GoogleCredentialResult.Unavailable -> finish(AccountNotice.SignInUnavailable)
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

    private suspend fun refreshCredits() {
        if (_state.value.account !is Account.SignedIn) return
        val remaining = runCatching { credits.remaining() }.getOrNull()
        _state.update { it.copy(credits = remaining) }
    }
}
