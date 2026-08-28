package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

private val AVATAR_SIZE = 72.dp
private val BUTTON_HEIGHT = 52.dp

@Composable
internal fun SignedInPanel(
    email: String?,
    photoUrl: String?,
    credits: Int?,
    onDone: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(photoUrl)
        Spacer(Modifier.height(16.dp))
        Text(
            text = email ?: stringResource(R.string.account_signed_in),
            modifier = Modifier.testTag(AccountTestTags.EMAIL),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
                    text = String.format(stringResource(R.string.account_credits), credits),
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
            onClick = onSignOut,
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
        Text(
            text = stringResource(R.string.account_sign_out_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Avatar(photoUrl: String?) {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNullOrBlank()) {
            CheckIcon(32.dp, MaterialTheme.colorScheme.onPrimaryContainer)
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape),
            )
        }
    }
}
