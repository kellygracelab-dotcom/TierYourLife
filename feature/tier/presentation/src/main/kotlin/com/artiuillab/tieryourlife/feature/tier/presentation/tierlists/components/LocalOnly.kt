package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
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
import com.artiuillab.tieryourlife.core.theme.layout.currentWindowShape
import com.artiuillab.tieryourlife.core.theme.ui.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/**
 * Where a guest's boards are kept, said once above them: on this phone, and
 * an account keeps them through a change of phone. The two lines are a fact
 * and stay; the buttons are the offer, asked until answered. A big card and a
 * footer used to say this twice. A window with a rail is not called a phone:
 * it is a tablet or an opened fold. A narrow phone gives the words the icon's room.
 */
@Composable
internal fun LocalOnlyNotice(
    boardCount: Int,
    offerSignIn: Boolean,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val device = currentWindowShape.hasRail
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TierListsTestTags.LOCAL_ONLY_NOTICE),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        BoxWithConstraints {
            val narrow = maxWidth < NARROW_NOTICE
            Column(Modifier.padding(start = 14.dp, end = 8.dp, top = if (narrow) 10.dp else 12.dp, bottom = if (offerSignIn) 2.dp else 12.dp)) {
                Row(Modifier.padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!narrow) DeviceIcon(20.dp, MaterialTheme.colorScheme.onSurfaceVariant, tablet = device)
                    Column(Modifier.padding(start = if (narrow) 0.dp else 12.dp)) {
                        Text(
                            text = stringResource(
                                if (device) R.string.local_only_footer_device else R.string.local_only_footer,
                                pluralStringResource(R.plurals.board_count, boardCount, boardCount),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(keepThem(boardCount, device)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (offerSignIn) Offer(onSignIn, onDismiss, compact = narrow)
            }
        }
    }
}

@Composable
private fun Offer(onSignIn: () -> Unit, onDismiss: () -> Unit, compact: Boolean) {
    // On a narrow phone the two answers sit closer: still a finger's width apart, a third of the screen no longer.
    val size = if (compact) Modifier.height(COMPACT_BUTTON) else Modifier
    val padding = if (compact) PaddingValues(horizontal = 10.dp) else ButtonDefaults.TextButtonContentPadding
    Row(Modifier.fillMaxWidth().padding(top = if (compact) 4.dp else 0.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss, modifier = size.testTag(TierListsTestTags.LOCAL_ONLY_DISMISS), contentPadding = padding) {
            Text(stringResource(R.string.local_only_card_not_now))
        }
        TextButton(onClick = onSignIn, modifier = size.testTag(TierListsTestTags.LOCAL_ONLY_SIGN_IN), contentPadding = padding) {
            Text(stringResource(R.string.local_only_card_sign_in))
        }
    }
}

/** Below this the icon gives its room to the words, which then keep to fewer lines. */
private val NARROW_NOTICE = 300.dp
private val COMPACT_BUTTON = 32.dp

/** Two strings rather than a plural: neither shows the number, and in French "one" also covers zero. */
private fun keepThem(boardCount: Int, device: Boolean): Int = when {
    device && boardCount == 1 -> R.string.local_only_notice_keep_one_device
    device -> R.string.local_only_notice_keep_many_device
    boardCount == 1 -> R.string.local_only_notice_keep_one
    else -> R.string.local_only_notice_keep_many
}

@Composable
private fun DeviceIcon(iconSize: Dp, color: Color, tablet: Boolean) = VectorIcon(iconSize) { scale ->
    drawRoundRect(
        color = color,
        topLeft = if (tablet) Offset(4.5f * scale, 2.5f * scale) else Offset(7f * scale, 2.5f * scale),
        size = if (tablet) Size(15f * scale, 19f * scale) else Size(10f * scale, 19f * scale),
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
