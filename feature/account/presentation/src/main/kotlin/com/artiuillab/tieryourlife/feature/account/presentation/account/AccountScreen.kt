package com.artiuillab.tieryourlife.feature.account.presentation.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.CloseIcon
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.FaceSheet
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.NicknameDialog
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.SignInPitch
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.SignedInPanel
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.previewAccountGuestState
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.previewAccountSignedInState
import com.artiuillab.tieryourlife.feature.account.presentation.account.components.previewAccountSigningInState

@Composable
fun AccountScreen(
    onClose: () -> Unit,
    onOpenPublished: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AccountScreenContent(
        state = state,
        onOpenPublished = onOpenPublished,
        onClose = onClose,
        onSignIn = { viewModel.signIn(context) },
        onSignOut = viewModel::signOut,
        onSetName = viewModel::setDisplayName,
        onSetPhoto = viewModel::setPhoto,
        onNoticeShown = viewModel::dismissNotice,
    )
}

@Composable
internal fun AccountScreenContent(
    state: AccountUiState,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onSetName: (String) -> Unit = {},
    onSetPhoto: (String?) -> Unit = {},
    onNoticeShown: () -> Unit = {},
    onOpenPublished: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var nicknameDialogVisible by rememberSaveable { mutableStateOf(false) }
    var faceSheetVisible by rememberSaveable { mutableStateOf(false) }
    val noticeText = state.notice?.let { stringResource(it.messageRes()) }

    LaunchedEffect(state.notice) {
        if (noticeText != null) {
            snackbarHostState.showSnackbar(noticeText)
            onNoticeShown()
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .testTag(AccountTestTags.SCREEN),
        ) {
            CloseBar(onClose)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            ) {
                when (val account = state.account) {
                    // Painting either panel before Firebase answers means
                    // painting the wrong one and swapping it a frame later.
                    Account.Unknown -> Unit

                    Account.Guest -> SignInPitch(
                        signingIn = state.signingIn,
                        onSignIn = onSignIn,
                        onNotNow = onClose,
                    )

                    is Account.SignedIn -> SignedInPanel(
                        email = account.email,
                        photoUrl = account.photoUrl,
                        displayName = account.displayName,
                        publicListCount = state.publicListCount,
                        credits = state.credits,
                        onEditName = { nicknameDialogVisible = true },
                        onEditFace = { faceSheetVisible = true },
                        onOpenPublished = onOpenPublished,
                        onDone = onClose,
                        onSignOut = onSignOut,
                    )
                }
            }
        }

        val signedIn = state.account as? Account.SignedIn
        if (nicknameDialogVisible && signedIn != null) {
            NicknameDialog(
                current = signedIn.displayName.orEmpty(),
                onDismiss = { nicknameDialogVisible = false },
                onSave = {
                    onSetName(it)
                    nicknameDialogVisible = false
                },
            )
        }

        if (faceSheetVisible && signedIn != null) {
            FaceSheet(
                current = signedIn.photoUrl,
                googlePhotoUrl = state.googlePhotoUrl,
                cardImages = state.faceChoices,
                name = signedIn.displayName,
                onDismiss = { faceSheetVisible = false },
                onChoose = {
                    onSetPhoto(it)
                    faceSheetVisible = false
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(Alignment.Bottom)
                .testTag(AccountTestTags.NOTICE),
        )
    }
}

@Composable
private fun CloseBar(onClose: () -> Unit) {
    val closeDescription = stringResource(R.string.account_cd_close)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = closeDescription }
                .testTag(AccountTestTags.CLOSE),
        ) {
            CloseIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun AccountNotice.messageRes(): Int = when (this) {
    AccountNotice.SignInFailed -> R.string.account_error_sign_in_failed
    AccountNotice.NoGoogleAccount -> R.string.account_error_no_google_account
    AccountNotice.SignInUnavailable -> R.string.account_error_unavailable
    AccountNotice.SignedInToExistingAccount -> R.string.account_notice_existing_account
    AccountNotice.NameNotSaved -> R.string.account_error_name_not_saved
}

@TierYourLifeDevicePreviews
@Composable
private fun AccountScreenGuestLightPreview() = TierYourLifeTheme(false) {
    AccountScreenContent(state = previewAccountGuestState, onClose = {}, onSignIn = {}, onSignOut = {})
}

@TierYourLifeDevicePreviews
@Composable
private fun AccountScreenGuestDarkPreview() = TierYourLifeTheme(true) {
    AccountScreenContent(state = previewAccountGuestState, onClose = {}, onSignIn = {}, onSignOut = {})
}

@Preview(name = "Signed in", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AccountScreenSignedInPreview() = TierYourLifeTheme {
    AccountScreenContent(state = previewAccountSignedInState, onClose = {}, onSignIn = {}, onSignOut = {})
}

@Preview(name = "Signed in, dark", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AccountScreenSignedInDarkPreview() = TierYourLifeTheme(true) {
    AccountScreenContent(state = previewAccountSignedInState, onClose = {}, onSignIn = {}, onSignOut = {})
}

@Preview(name = "Signing in", device = "id:pixel_9", showBackground = true, showSystemUi = true)
@Composable
private fun AccountScreenSigningInPreview() = TierYourLifeTheme {
    AccountScreenContent(state = previewAccountSigningInState, onClose = {}, onSignIn = {}, onSignOut = {})
}
