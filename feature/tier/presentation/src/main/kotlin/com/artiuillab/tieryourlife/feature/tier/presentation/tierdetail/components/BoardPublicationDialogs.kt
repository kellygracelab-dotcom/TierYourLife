package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.core.theme.ui.RankingChoiceDialog
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.board.components.sheets.publishErrorText
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.BoardPublicationState
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

@Composable
internal fun BoardPublicationDialogs(
    state: BoardPublicationState,
    signedIn: Boolean,
    onConfirm: () -> Unit,
    onDismissPreview: () -> Unit,
    onPublic: () -> Unit,
    onDismissOffer: () -> Unit,
    onDismissError: () -> Unit,
    onRecheckOriginal: () -> Unit,
    onSignIn: () -> Unit,
) {
    val refusal = state.error
    if (refusal != null) {
        AlertDialog(
            modifier = Modifier.testTag("publication_error"),
            onDismissRequest = { if (!state.busy) onDismissError() },
            title = { Text(stringResource(ThemeR.string.publication_preview_title)) },
            text = { Text(publishErrorText(refusal, LocalResources.current, LocalConfiguration.current.locales[0])) },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    modifier = Modifier.testTag("publication_error_action"),
                    onClick = {
                        when (refusal) {
                            PublishError.SourceChanged -> onRecheckOriginal()
                            PublishError.NotSignedIn -> {
                                onDismissError()
                                onSignIn()
                            }
                            else -> onDismissError()
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            when (refusal) {
                        PublishError.SourceChanged -> ThemeR.string.source_changed_recheck
                        PublishError.NotSignedIn -> ThemeR.string.ranking_choice_sign_in
                        else -> android.R.string.ok
                    },
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissError, enabled = !state.busy) { Text(stringResource(ThemeR.string.action_cancel)) }
            },
        )
        return
    }
    val offer = state.offer
    if (offer != null) {
        RankingChoiceDialog(signedIn = signedIn, busy = state.busy, onShow = onPublic, onNotNow = onDismissOffer)
        return
    }
    val preview = state.preview ?: return
    val description = when (val kind = preview.kind) {
        is PublicationKind.Ranking -> ThemeR.string.publication_preview_ranking
        is PublicationKind.NewList -> ThemeR.string.publication_preview_independent
    }
    AlertDialog(
        modifier = Modifier.testTag("publication_preview"),
        onDismissRequest = { if (!state.busy) onDismissPreview() },
        title = { Text(stringResource(ThemeR.string.publication_preview_title)) },
        text = {
            Text(
                stringResource(description) + "\n\n" + stringResource(
                if (signedIn) ThemeR.string.ranking_choice_storage_account else ThemeR.string.ranking_choice_storage_guest,
            ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.busy, modifier = Modifier.testTag("publication_confirm")) {
                Text(stringResource(ThemeR.string.publication_preview_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissPreview, enabled = !state.busy, modifier = Modifier.testTag("publication_cancel")) {
                Text(stringResource(ThemeR.string.action_cancel))
            }
        },
    )
}
