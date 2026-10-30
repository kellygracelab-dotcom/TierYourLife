package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.layout.systemBarsContentPadding
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_PENDING_SECTIONS
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.presentation.R
import com.artiuillab.tieryourlife.feature.tier.board.common.label
import java.text.DateFormat
import java.util.Date
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

internal object ProposeTestTags {
    const val SCREEN = "propose_screen"
    const val CLOSE = "propose_close"
    const val SEND = "propose_send"
    const val DROP_ZONE = "propose_drop_zone"
    const val CROP_FRAME = "propose_crop_frame"
    const val CHANGE_PICTURE = "propose_change_picture"
    const val PICTURE_NOTICE = "propose_picture_notice"
    const val NAME = "propose_name"
    const val NAME_TAKEN = "propose_name_taken"
    const val NAME_BAD = "propose_name_bad"
    const val FILE_UNDER = "propose_file_under"
    const val ERROR = "propose_error"
    const val SIGN_IN = "propose_sign_in"
    const val TOO_MANY = "propose_too_many"
    const val DISCARD = "propose_discard"

    fun icon(name: String) = "propose_icon_$name"
}

/** What the form can be asked to do. */
class ProposeCallbacks(
    val onName: (String) -> Unit = {},
    val onPickPicture: () -> Unit = {},
    val onMoveFrame: (PictureFrame) -> Unit = {},
    val onIcon: (String) -> Unit = {},
    val onFileUnder: (String) -> Unit = {},
    val onPropose: () -> Unit = {},
)

@Composable
fun ProposeSectionScreen(
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenSection: (String) -> Unit,
    viewModel: ProposeSectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.pickPicture(it.toString()) }
    }
    LaunchedEffect(viewModel) { viewModel.proposed.collect { onClose() } }
    ProposeSectionContent(
        state = state,
        onClose = onClose,
        onSignIn = onSignIn,
        onOpenSection = onOpenSection,
        calls = ProposeCallbacks(
            onName = viewModel::updateName,
            onPickPicture = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onMoveFrame = viewModel::moveFrame,
            onIcon = viewModel::chooseIcon,
            onFileUnder = viewModel::chooseFileUnder,
            onPropose = viewModel::propose,
        ),
    )
}

/**
 * The proposal form as a full-screen dialog (design-spec-admin §2): close,
 * the title and Propose in the bar; the picture with its crop, the name, the
 * icon and where lists wait below. A guest is asked to sign in instead, and
 * somebody with three proposals waiting is told so instead.
 */
@Composable
fun ProposeSectionContent(
    state: ProposeSectionUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit = {},
    onOpenSection: (String) -> Unit = {},
    calls: ProposeCallbacks = ProposeCallbacks(),
) {
    var discarding by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state.edited && !state.sending) discarding = true else if (!state.sending) onClose() }
    BackHandler(onBack = leave)
    Surface(modifier.fillMaxSize().testTag(ProposeTestTags.SCREEN), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val form = state.signedIn == true && !state.tooManyWaiting && state.error != ProposalError.TooManyPending
            TopBar(onClose = leave, proposing = if (form) state else null, onPropose = calls.onPropose)
            when {
                state.signedIn == null -> Unit
                state.signedIn == false -> SignInFirst(onSignIn = onSignIn, onCancel = onClose)
                !form -> TooManyWaiting(state, onClose)
                else -> Form(state, calls, onOpenSection)
            }
        }
    }
    // A dialog, not a line at the foot of the form: there it sat below the
    // fold, so Back and the close button seemed to do nothing.
    if (discarding) DiscardDialog(onKeepEditing = { discarding = false }, onDiscard = onClose)
}

@Composable
private fun TopBar(onClose: () -> Unit, proposing: ProposeSectionUiState?, onPropose: () -> Unit) {
    val close = stringResource(ThemeR.string.action_cancel)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = close }.testTag(ProposeTestTags.CLOSE)) {
            ClearIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.all_sections_propose),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        if (proposing != null) {
            TextButton(onClick = onPropose, enabled = proposing.canPropose, modifier = Modifier.testTag(ProposeTestTags.SEND)) {
                if (proposing.sending) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.propose_send))
                }
            }
        }
    }
}

@Composable
private fun Form(
    state: ProposeSectionUiState,
    calls: ProposeCallbacks,
    onOpenSection: (String) -> Unit,
) {
    val editable = !state.sending
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(systemBarsContentPadding(start = 24.dp, end = 24.dp, bottom = 24.dp)),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.propose_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val picture = state.picture
            if (picture == null) {
                PictureDropZone(onPick = { if (editable) calls.onPickPicture() })
            } else {
                PictureCropFrame(picture, state.frame, state.icon, state.name, onMove = calls.onMoveFrame)
                TextButton(onClick = calls.onPickPicture, enabled = editable, modifier = Modifier.testTag(ProposeTestTags.CHANGE_PICTURE)) {
                    Text(stringResource(R.string.propose_picture_change))
                }
            }
            state.pictureNotice?.let { notice ->
                Text(
                    pictureNoticeText(notice),
                    modifier = Modifier.testTag(ProposeTestTags.PICTURE_NOTICE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        ProposalNameField(state.name, state.nameStanding, editable, calls.onName, onOpenSection)
        ProposalIconGrid(state.icon, editable, calls.onIcon)
        ProposalFileUnder(state.fileUnder, editable, calls.onFileUnder)
        if (state.waiting.isNotEmpty()) {
            Text(
                pluralStringResource(R.plurals.propose_waiting_count, state.waiting.size, state.waiting.size, MAX_PENDING_SECTIONS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        state.error?.let { ErrorBar(it, onRetry = calls.onPropose.takeIf { state.canPropose }) }
    }
}

/** Anything the fields cannot show themselves: on the error container, with Try again where trying again can help. */
@Composable
private fun ErrorBar(error: ProposalError, onRetry: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .testTag(ProposeTestTags.ERROR),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            proposalErrorText(error),
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        if (error == ProposalError.Failed && onRetry != null) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.explore_try_again)) }
        }
    }
}

@Composable
private fun DiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        text = { Text(stringResource(R.string.propose_discard_question)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.propose_discard)) } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.propose_keep_editing)) } },
        modifier = Modifier.testTag(ProposeTestTags.DISCARD),
    )
}

/** Every refusal the form shows above its actions, in words. Name and picture refusals stay on their fields. */
@Composable
internal fun proposalErrorText(error: ProposalError): String = when (error) {
    ProposalError.BadName -> stringResource(R.string.propose_name_bad)
    is ProposalError.NameTaken -> stringResource(R.string.propose_name_taken_plain)
    ProposalError.BadIcon -> stringResource(R.string.propose_error_icon)
    ProposalError.BadFileUnder -> stringResource(R.string.propose_error_file_under)
    is ProposalError.BadPicture -> stringResource(R.string.propose_picture_unreadable)
    ProposalError.TooManyPending -> stringResource(R.string.propose_too_many_body)
    ProposalError.TooManyToday -> stringResource(R.string.propose_error_today)
    is ProposalError.Banned -> error.untilMillis?.let { until ->
        stringResource(R.string.propose_error_banned_until, DateFormat.getDateInstance(DateFormat.LONG, LocalConfiguration.current.locales[0]).format(Date(until)))
    } ?: stringResource(R.string.propose_error_banned)
    ProposalError.NotSignedIn -> stringResource(R.string.propose_sign_in_title)
    ProposalError.Failed -> stringResource(R.string.propose_failed)
}

@Composable
private fun SignInFirst(onSignIn: () -> Unit, onCancel: () -> Unit) {
    Message(
        title = stringResource(R.string.propose_sign_in_title),
        body = stringResource(R.string.propose_sign_in_body),
        tag = ProposeTestTags.SIGN_IN,
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(ThemeR.string.action_cancel)) }
        Button(onClick = onSignIn) { Text(stringResource(R.string.propose_sign_in)) }
    }
}

/** Three waiting already: what the form would refuse, said before anything is filled in. */
@Composable
private fun TooManyWaiting(state: ProposeSectionUiState, onClose: () -> Unit) {
    Message(
        title = stringResource(R.string.propose_too_many_title),
        body = stringResource(R.string.propose_too_many_body),
        tag = ProposeTestTags.TOO_MANY,
        footer = state.waiting.takeIf { it.isNotEmpty() }?.let { waiting -> { WaitingNames(waiting.map { it.label() }) } },
    ) {
        Button(onClick = onClose) { Text(stringResource(R.string.section_ok)) }
    }
}

@Composable
private fun WaitingNames(names: List<String>) {
    Text(
        names.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Message(
    title: String,
    body: String,
    tag: String,
    footer: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(32.dp).testTag(tag), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            footer?.invoke()
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}
