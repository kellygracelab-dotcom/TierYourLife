package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The signed-in reader's own name and face: an anonymous list of theirs carries neither, yet is theirs to recognise. */
@Immutable
data class OwnFace(val name: String?, val photoUrl: String?)

internal val LocalOwnFace = staticCompositionLocalOf<OwnFace?> { null }

@HiltViewModel
class OwnFaceViewModel @Inject constructor(accounts: AccountRepository) : ViewModel() {

    val face: StateFlow<OwnFace?> = accounts.account
        .map { account -> (account as? Account.SignedIn)?.let { OwnFace(it.authorName, it.photoUrl) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
