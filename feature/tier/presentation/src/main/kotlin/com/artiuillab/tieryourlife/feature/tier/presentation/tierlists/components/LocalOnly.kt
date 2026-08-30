package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Said once, when there is finally something to lose.
 *
 * A card in the list rather than a dialog. It fires on arriving at a screen
 * instead of on doing something, and anything that fires on arrival and blocks
 * the way reads as an advertisement. On a list with one board this sits at the
 * top of a nearly empty screen, so it is as visible as a dialog would be at
 * none of the cost.
 *
 * "Not now" means never, not next week. A card somebody has to swat repeatedly
 * teaches them to swat it; [LocalOnlyFooter] carries on saying the same thing
 * quietly for as long as it stays true.
 */
@Composable
internal fun LocalOnlySignInCard(
    boardCount: Int,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth().testTag(TierListsTestTags.LOCAL_ONLY_CARD),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text(
                text = stringResource(
                    oneBoard(
                        boardCount,
                        R.string.local_only_card_title_one,
                        R.string.local_only_card_title_many,
                    ),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    oneBoard(
                        boardCount,
                        R.string.local_only_card_body_one,
                        R.string.local_only_card_body_many,
                    ),
                ),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag(TierListsTestTags.LOCAL_ONLY_DISMISS),
                ) {
                    Text(stringResource(R.string.local_only_card_not_now))
                }
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag(TierListsTestTags.LOCAL_ONLY_SIGN_IN),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) {
                    Text(stringResource(R.string.local_only_card_sign_in))
                }
            }
        }
    }
}

/**
 * The permanent half. A fact with no button on it, which is the point: it
 * reports where the boards are and asks for nothing.
 *
 * Never shown to somebody signed in. It is a warning about being in one place
 * only, and once the boards are kept it is simply false -- there is no
 * "backed up" counterpart, because a state that is fine does not need
 * reporting.
 */
@Composable
internal fun LocalOnlyFooter(boardCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmartphoneIcon(16.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = stringResource(
                R.string.local_only_footer,
                pluralStringResource(R.plurals.board_count, boardCount, boardCount),
            ),
            modifier = Modifier
                .padding(start = 6.dp)
                .testTag(TierListsTestTags.LOCAL_ONLY_FOOTER),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Two strings rather than a plural: neither of them shows the number, and a
 * plural whose message never mentions its count is a translation trap -- in
 * French "one" also covers zero.
 */
private fun oneBoard(boardCount: Int, one: Int, many: Int): Int = if (boardCount == 1) one else many

@Composable
private fun SmartphoneIcon(iconSize: Dp, color: Color) = VectorIcon(iconSize) { scale ->
    drawRoundRect(
        color = color,
        topLeft = Offset(7f * scale, 2.5f * scale),
        size = Size(10f * scale, 19f * scale),
        cornerRadius = CornerRadius(2f * scale, 2f * scale),
        style = Stroke(1.6f * scale),
    )
    drawLine(
        color,
        Offset(10.5f * scale, 18.5f * scale),
        Offset(13.5f * scale, 18.5f * scale),
        1.6f * scale,
    )
}

@Preview(showBackground = true)
@Composable
private fun LocalOnlySignInCardPreview() = TierYourLifeTheme(false) {
    LocalOnlySignInCard(boardCount = 1, onSignIn = {}, onDismiss = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun LocalOnlySignInCardManyDarkPreview() = TierYourLifeTheme(true) {
    LocalOnlySignInCard(boardCount = 4, onSignIn = {}, onDismiss = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun LocalOnlyFooterPreview() = TierYourLifeTheme(false) {
    LocalOnlyFooter(boardCount = 7)
}
