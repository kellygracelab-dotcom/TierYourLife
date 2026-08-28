package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.SettingsTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon

private val AVATAR_SIZE = 40.dp

/**
 * Signed out this is an offer, so it opens something; signed in there is
 * nothing left to open, so it stops being tappable and becomes what it says.
 */
@Composable
internal fun AccountRow(
    account: Account,
    onClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (account) {
        Account.Guest -> GuestRow(onClick = onClick, modifier = modifier)
        is Account.SignedIn -> SignedInRow(account = account, onSignOut = onSignOut, modifier = modifier)
    }
}

@Composable
private fun GuestRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(SettingsTestTags.ACCOUNT)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarPlaceholder()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_account_guest),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_account_guest_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SignedInRow(account: Account.SignedIn, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SettingsTestTags.ACCOUNT)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (account.photoUrl.isNullOrBlank()) {
            AvatarPlaceholder()
        } else {
            AsyncImage(
                model = account.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = account.email ?: stringResource(R.string.settings_account),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onSignOut, modifier = Modifier.testTag(SettingsTestTags.SIGN_OUT)) {
            Text(stringResource(R.string.settings_account_sign_out))
        }
    }
}

@Composable
private fun AvatarPlaceholder() {
    Box(
        modifier = Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AccountCircleIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountRowGuestPreview() = TierYourLifeTheme(false) {
    AccountRow(account = Account.Guest, onClick = {}, onSignOut = {})
}

@Preview(showBackground = true)
@Composable
private fun AccountRowSignedInPreview() = TierYourLifeTheme(false) {
    AccountRow(
        account = Account.SignedIn(email = "danylo@example.com", photoUrl = null),
        onClick = {},
        onSignOut = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountRowSignedInDarkPreview() = TierYourLifeTheme(true) {
    AccountRow(
        account = Account.SignedIn(email = "danylo@example.com", photoUrl = null),
        onClick = {},
        onSignOut = {},
    )
}
