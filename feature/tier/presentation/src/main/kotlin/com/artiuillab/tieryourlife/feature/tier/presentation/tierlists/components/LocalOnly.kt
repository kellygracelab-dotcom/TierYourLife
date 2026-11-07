package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Where a guest's boards are kept, said once above them: on this phone, and
 * an account keeps them through a change of phone. The two lines are a fact
 * and stay; the buttons are the offer, asked until answered. A big card and a
 * footer used to say this twice.
 */
@Composable
internal fun LocalOnlyNotice(
    boardCount: Int,
    offerSignIn: Boolean,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TierListsTestTags.LOCAL_ONLY_NOTICE),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = if (offerSignIn) 4.dp else 12.dp)) {
            Row(Modifier.padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                SmartphoneIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.local_only_footer,
                            pluralStringResource(R.plurals.board_count, boardCount, boardCount),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(oneBoard(boardCount, R.string.local_only_notice_keep_one, R.string.local_only_notice_keep_many)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (offerSignIn) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag(TierListsTestTags.LOCAL_ONLY_DISMISS)) {
                        Text(stringResource(R.string.local_only_card_not_now))
                    }
                    TextButton(onClick = onSignIn, modifier = Modifier.testTag(TierListsTestTags.LOCAL_ONLY_SIGN_IN)) {
                        Text(stringResource(R.string.local_only_card_sign_in))
                    }
                }
            }
        }
    }
}

/** Two strings rather than a plural: neither shows the number, and in French "one" also covers zero. */
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
private fun LocalOnlyNoticePreview() = TierYourLifeTheme(false) {
    LocalOnlyNotice(boardCount = 1, offerSignIn = true, onSignIn = {}, onDismiss = {}, modifier = Modifier.padding(16.dp))
}

@Preview(showBackground = true)
@Composable
private fun LocalOnlyNoticeAnsweredDarkPreview() = TierYourLifeTheme(true) {
    LocalOnlyNotice(boardCount = 4, offerSignIn = false, onSignIn = {}, onDismiss = {}, modifier = Modifier.padding(16.dp))
}
