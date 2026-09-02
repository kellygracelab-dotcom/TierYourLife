package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

private val BUTTON_HEIGHT = 52.dp
private val AVATAR_IN_ROW = 40.dp

@Composable
internal fun SignedInPanel(
    email: String?,
    photoUrl: String?,
    displayName: String?,
    publicListCount: Int,
    credits: Int?,
    onEditName: () -> Unit,
    onEditFace: () -> Unit,
    onOpenPublished: () -> Unit,
    onDone: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {},
    deleting: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val name = displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.account_signed_in)

    var confirming by rememberSaveable { mutableStateOf(false) }
    var deleteConfirming by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(photoUrl = photoUrl, name = displayName, onEdit = onEditFace)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onEditName)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag(AccountTestTags.EDIT_NAME),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier.testTag(AccountTestTags.NAME),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            PencilIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (email != null) {
            Text(
                text = email,
                modifier = Modifier.testTag(AccountTestTags.EMAIL),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))
        CommunitySection(
            name = name,
            photoUrl = photoUrl,
            publicListCount = publicListCount,
            onOpenPublished = onOpenPublished,
        )

        if (credits != null) {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(20.dp)
                    .testTag(AccountTestTags.CREDITS),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = pluralStringResource(R.plurals.account_credits, credits, credits),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.account_credits_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag(AccountTestTags.DONE),
        ) {
            Text(stringResource(R.string.account_action_done))
        }

        Spacer(Modifier.height(40.dp))
        OutlinedButton(
            onClick = { confirming = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .testTag(AccountTestTags.SIGN_OUT),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.account_action_sign_out))
        }
        Spacer(Modifier.height(8.dp))
        // The same three facts the question asks, run together as one line:
        // read before pressing anything it is reassurance, and the dialog
        // breaks them apart only because that is the moment they are being
        // weighed. Both replaced "your lists stay on this phone", which was
        // true when this was the only place they lived and became a
        // half-truth that read as a threat.
        Text(
            text = stringResource(R.string.account_sign_out_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Quieter than signing out and further from it, because it is the one
        // thing here that cannot be undone. Plain text rather than a second
        // outlined button: two bordered buttons in a column read as a pair of
        // equals, and these two are not.
        Spacer(Modifier.height(24.dp))
        TextButton(
            onClick = { deleteConfirming = true },
            enabled = !deleting,
            modifier = Modifier.testTag(AccountTestTags.DELETE_ACCOUNT),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.account_action_delete))
        }
    }

    // Only exists because sync exists. Before boards were kept, signing out
    // changed nothing about anybody's data and a confirmation would have been
    // a speed bump for its own sake.
    if (deleteConfirming) {
        DeleteAccountDialog(
            onConfirm = {
                deleteConfirming = false
                onDeleteAccount()
            },
            onDismiss = { deleteConfirming = false },
        )
    }

    if (confirming) {
        SignOutDialog(
            onConfirm = {
                confirming = false
                onSignOut()
            },
            onDismiss = { confirming = false },
        )
    }
}

/** Shows the author row exactly as the community sees it, email left out of it. */
@Composable
private fun CommunitySection(
    name: String,
    photoUrl: String?,
    publicListCount: Int,
    onOpenPublished: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.account_community_section),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable(onClick = onOpenPublished)
                .padding(16.dp)
                .testTag(AccountTestTags.COMMUNITY_ROW),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(photoUrl = photoUrl, name = name, size = AVATAR_IN_ROW)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.account_community_lists,
                        publicListCount,
                        publicListCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.account_community_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
