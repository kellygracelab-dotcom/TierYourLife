package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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

@Composable
internal fun AccountRow(
    account: Account,
    credits: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (account) {
        Account.Unknown -> AccountPlaceholder(modifier = modifier)
        Account.Guest -> SignInOffer(onClick = onClick, modifier = modifier)
        is Account.SignedIn -> SignedInRow(
            account = account,
            credits = credits,
            onClick = onClick,
            modifier = modifier,
        )
    }
}

/**
 * Holds the signed-in row's exact height while Firebase is still answering, so
 * the card neither flashes the sign-in offer nor resizes underneath a finger.
 */
@Composable
private fun AccountPlaceholder(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(AVATAR_SIZE).clip(CircleShape).background(tint))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(tint))
            Box(Modifier.fillMaxWidth(0.32f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(tint))
        }
    }
}

@Composable
private fun SignInOffer(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SettingsTestTags.ACCOUNT)
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_account_offer_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_account_offer_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.testTag(SettingsTestTags.ACCOUNT_ACTION),
        ) {
            Text(stringResource(R.string.settings_account_offer_action))
        }
    }
}

@Composable
private fun SignedInRow(
    account: Account.SignedIn,
    credits: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = if (credits == null) {
        stringResource(R.string.settings_account_signed_in)
    } else {
        pluralStringResource(R.plurals.settings_account_signed_in_credits, credits, credits)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(SettingsTestTags.ACCOUNT)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(account.photoUrl)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = account.email ?: stringResource(R.string.settings_account),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun Avatar(photoUrl: String?) {
    if (photoUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AccountCircleIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

@Preview(showBackground = true)
@Composable
private fun AccountRowGuestPreview() = TierYourLifeTheme(false) {
    AccountRow(account = Account.Guest, credits = null, onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun AccountRowGuestDarkPreview() = TierYourLifeTheme(true) {
    AccountRow(account = Account.Guest, credits = null, onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun AccountRowSignedInPreview() = TierYourLifeTheme(false) {
    AccountRow(
        account = Account.SignedIn(email = "danylo@example.com", photoUrl = null),
        credits = 12,
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountRowSignedInDarkPreview() = TierYourLifeTheme(true) {
    AccountRow(
        account = Account.SignedIn(email = "danylo@example.com", photoUrl = null),
        credits = 12,
        onClick = {},
    )
}
